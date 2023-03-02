/*
 * File: mb4s.scala                                                            *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-03-02 12:09:31 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.mem.ram

import chisel3._
import chisel3.util._
import chisel3.experimental._

import herd.common.gen._
import herd.common.tools._
import herd.common.mem.mb4s._
import herd.common.field._


class Mb4sCtrl (p: Mb4sCtrlParams) extends Module {
  val io = IO(new Bundle {
    val b_port = Flipped(new Mb4sIO(p.pPort))

    val i_slct_read = if (p.useFieldSlct) Some(Input(new SlctBus(p.nField, p.nPart, 1))) else None    
    val b_read = Flipped(new CtrlReadIO(p.useField, p.nField, p.nAddrBit, p.nDataByte))

    val i_slct_write = if (p.useFieldSlct) Some(Input(new SlctBus(p.nField, p.nPart, 1))) else None
    val b_write = Flipped(new CtrlWriteIO(p.useField, p.nField, p.nAddrBit, p.nDataByte))
  })

  val m_req = Module(new Mb4sReqSReg(p.pPort, p.useReqReg))
  val m_ack = Module(new GenSReg(p, new Mb4sReqBus(p.pPort), UInt(0.W), false, false, true))

  // ******************************
  //        FIELD INTERFACE
  // ******************************
  val r_slct_ack = Reg(new SlctBus(p.nField, p.nPart, 1))

  val w_slct_req = Wire(new SlctBus(p.nField, p.nPart, 1))
  val w_slct_ack = Wire(new SlctBus(p.nField, p.nPart, 1))
  val w_slct_rdata = Wire(new SlctBus(p.nField, p.nPart, 1))

  if (p.useFieldSlct) {
    w_slct_req := io.i_slct_read.get
    w_slct_ack := io.i_slct_write.get
    r_slct_ack := w_slct_req
    w_slct_rdata := r_slct_ack
  } else {    
    w_slct_req := SLCT.ZERO
    w_slct_ack := SLCT.ZERO
    w_slct_rdata := SLCT.ZERO
  }

  // ******************************
  //              REQ
  // ******************************  
  val w_req = Wire(new GenSVBus(p, new Mb4sReqBus(p.pPort), UInt(0.W)))

  val w_req_wait = Wire(Bool())  

  val w_req_rwait = Wire(Bool())
  val w_req_wwait = Wire(Bool())
  val w_req_await = Wire(Bool())

  // ------------------------------
  //             INPUT
  // ------------------------------
  m_req.io.b_port <> io.b_port.req
  if (p.useFieldSlct) m_req.io.i_slct.get := w_slct_req
  m_req.io.b_sout.ready := ~w_req_wait & ~w_req_await

  w_req.valid := m_req.io.b_sout.valid
  if (p.useField) w_req.field.get := m_req.io.b_sout.field.get
  w_req.ctrl.get := m_req.io.b_sout.ctrl.get
  if (p.readOnly) w_req.ctrl.get.op := OP.R

  // ------------------------------
  //             WAIT
  // ------------------------------  
  w_req_rwait := w_req.valid & w_req.ctrl.get.ra & ~io.b_read.ready  
  if (p.isRom || p.readOnly) {
    w_req_wwait := false.B
  } else {
    w_req_wwait := m_ack.io.o_val.valid(w_slct_req.field) & m_ack.io.o_val.ctrl.get(w_slct_req.field).wa & w_req.ctrl.get.ra  
  }  

  w_req_wait := w_req_rwait | w_req_wwait

  // ------------------------------
  //             READ
  // ------------------------------
  io.b_read.valid := w_req.valid & w_req.ctrl.get.ra & ~w_req_await & ~w_req_wwait   
  if (p.useFieldSlct) io.b_read.field.get := w_slct_req.field else if (p.useField) io.b_read.field.get := w_req.field.get
  io.b_read.mask := SIZE.toMask(p.nDataByte, w_req.ctrl.get.size)
  io.b_read.addr := w_req.ctrl.get.addr  

  // ******************************
  //             ACK
  // ******************************  
  val w_ack = Wire(new GenSVBus(p, new Mb4sReqBus(p.pPort), UInt(0.W)))  

  val w_ack_wait = Wire(Bool())
  val w_ack_pwait = Wire(Bool())
  val w_mb4s_wwait = Wire(Bool())
  val w_mb4s_rwait = Wire(Bool())
  
  // ------------------------------
  //           REGISTER
  // ------------------------------  
  for (fs <- 0 until p.nFieldSlct) {
    m_ack.io.i_flush(fs) := false.B
  }

  w_req_await := ~m_ack.io.b_sin.ready
  if (p.useFieldSlct) m_ack.io.i_slct_in.get := w_slct_req
  m_ack.io.b_sin.valid := w_req.valid & ~w_req_wait
  if (p.useField) m_ack.io.b_sin.field.get := w_req.field.get
  m_ack.io.b_sin.ctrl.get := w_req.ctrl.get

  m_ack.io.b_sout.ready := ~w_ack_wait & ~w_ack_pwait
  if (p.useFieldSlct) m_ack.io.i_slct_out.get := w_slct_ack
  w_ack.valid := m_ack.io.b_sout.valid
  if (p.useField) w_ack.field.get := m_ack.io.b_sout.field.get
  w_ack.ctrl.get := m_ack.io.b_sout.ctrl.get

  // ------------------------------
  //            WRITE
  // ------------------------------
  val m_wmb4s = if (!p.readOnly && !p.isRom) Some(Module(new Mb4sDataSReg(p.pPort))) else None

  if (!p.readOnly && !p.isRom) {
    // Mb4s write port
    m_wmb4s.get.io.b_port <> io.b_port.write

    if (p.useFieldSlct) m_wmb4s.get.io.i_slct.get := w_slct_ack
    m_wmb4s.get.io.b_sout.ready := w_ack.valid & w_ack.ctrl.get.wa & ~w_ack_wait

    // Memory write port
    w_ack_wait := w_ack.valid & w_ack.ctrl.get.wa & ~io.b_write.ready

    io.b_write.valid := w_ack.valid & w_ack.ctrl.get.wo & ~w_ack_pwait
    if (p.useFieldSlct) io.b_write.field.get := w_slct_ack.field else if (p.useField) io.b_write.field.get := w_ack.field.get
    io.b_write.addr := w_ack.ctrl.get.addr
    io.b_write.mask := SIZE.toMask(p.nDataByte, w_ack.ctrl.get.size)
    io.b_write.data := m_wmb4s.get.io.b_sout.data.get
  } else {
    // Mb4s write port
    for (fs <- 0 until p.nFieldSlct) {
      io.b_port.write.ready(fs) := false.B
    }

    // Memory write port
    w_ack_wait := false.B

    io.b_write := DontCare
    io.b_write.valid := false.B
  }

  // ------------------------------
  //             READ
  // ------------------------------
  val r_rdata_av = RegInit(VecInit(Seq.fill(p.nFieldSlct) {false.B}))
  val r_rdata = Reg(Vec(p.nFieldSlct, UInt((p.nDataByte * 8).W)))

  val w_rdata_av = Wire(Bool())
  val w_rdata = Wire(UInt((p.nDataByte * 8).W))

  // Read data buffer
  when (~r_rdata_av(w_slct_rdata.field)) {
    r_rdata_av(w_slct_rdata.field) := m_ack.io.o_val.valid(w_slct_rdata.field) & m_ack.io.o_val.ctrl.get(w_slct_rdata.field).ra & (w_ack_pwait | (w_slct_rdata.field =/= w_slct_ack.field))
    r_rdata(w_slct_rdata.field) := io.b_read.data
  }

  when (r_rdata_av(w_slct_rdata.field)) {
    r_rdata_av(w_slct_rdata.field) := w_ack_pwait
  }  

  w_rdata_av := r_rdata_av(w_slct_ack.field)
  w_rdata := r_rdata(w_slct_ack.field)
  
  // Memory read port  
  io.b_port.read.valid := w_ack.valid & w_ack.ctrl.get.ra & ~w_ack_wait
  if (p.useFieldSlct) io.b_port.read.field.get := w_slct_ack.field else if (p.useFieldTag) io.b_port.read.field.get := w_ack.field.get
  io.b_port.read.data := Mux(w_rdata_av, w_rdata, io.b_read.data)  

  // ------------------------------
  //             WAIT
  // ------------------------------
  if (!p.readOnly && !p.isRom) w_mb4s_wwait := w_ack.ctrl.get.wa & ~m_wmb4s.get.io.b_sout.valid else w_mb4s_wwait := false.B
  w_mb4s_rwait := w_ack.ctrl.get.ra & ~io.b_port.read.ready(w_slct_ack.field) 

  w_ack_pwait := w_ack.valid & (w_mb4s_wwait | w_mb4s_rwait)

  // ******************************
  //             DEBUG
  // ******************************
  if (p.debug) {
    dontTouch(io.b_port)
  } 
}

object Mb4sCtrl extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Mb4sCtrl(Mb4sCtrlConfigBase), args)
}
