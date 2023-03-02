/*
 * File: reg.scala                                                             *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-03-02 01:51:54 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.mem.mb4s

import chisel3._
import chisel3.util._

import herd.common.gen._
import herd.common.tools._
import herd.common.field._


class Mb4sReqDReg(p: Mb4sReqParams, useReg: Boolean) extends Module {
  val io = IO(new Bundle {  
    val b_port = Flipped(new Mb4sReqIO(p))

    val o_back = Output(Vec(p.nFieldSlct, Bool()))

    val b_dout = new GenDRVIO(p, new Mb4sReqBus(p), UInt(0.W))
  })  

  val w_lock = Wire(Vec(p.nFieldSlct, Bool()))

  // ******************************
  //          BACK REGISTER
  // ******************************
  val m_back = Module(new GenDReg(p, new Mb4sReqBus(p), UInt(0.W), false, false, false))

  val w_back = Wire(new GenDVBus(p, new Mb4sReqBus(p), UInt(0.W)))  

  for (fs <- 0 until p.nFieldSlct) {
    io.b_port.ready(fs) := m_back.io.b_din.ready(fs)

    // Write
    m_back.io.i_flush(fs) := false.B
    if (p.useFieldSlct) {
      m_back.io.b_din.valid(fs) := io.b_port.valid & (fs.U === io.b_port.field.get) & w_lock(fs)
    } else {
      m_back.io.b_din.valid(0) := io.b_port.valid & w_lock(0)
    }
    if (p.useFieldTag) m_back.io.b_din.field.get := io.b_port.field.get
    m_back.io.b_din.ctrl.get(fs) := io.b_port.ctrl

    // Read
    m_back.io.b_dout.ready(fs) := ~w_lock(fs)
    when (m_back.io.b_dout.valid(fs)) {
      w_back.valid(fs) := true.B
      if (p.useFieldTag) w_back.field.get := m_back.io.b_dout.field.get
      w_back.ctrl.get(fs) := m_back.io.b_dout.ctrl.get(fs)
    }.otherwise {
      if (p.useFieldSlct) {
        w_back.valid(fs) := io.b_port.valid & (fs.U === io.b_port.field.get)
      } else {
        w_back.valid(0) := io.b_port.valid
      }
      if (p.useFieldTag) w_back.field.get := m_back.io.b_dout.field.get
      w_back.ctrl.get(fs) := io.b_port.ctrl
    }
  }  

  // ******************************
  //         PORT REGISTER
  // ******************************
  // ------------------------------
  //             WITH
  // ------------------------------
  if (useReg) {
    val m_reg = Module(new GenDReg(p, new Mb4sReqBus(p), UInt(0.W), false, false, true))

    // Write
    for (fs <- 0 until p.nFieldSlct) {
      m_reg.io.i_flush(fs) := false.B

      w_lock(fs) := ~m_reg.io.b_din.ready(fs)

      m_reg.io.b_din.valid(fs) := w_back.valid(fs)
      if (p.useFieldTag) m_reg.io.b_din.field.get := w_back.field.get
      m_reg.io.b_din.ctrl.get(fs) := w_back.ctrl.get(fs)
    } 
      
    // Outputs
    io.b_dout <> m_reg.io.b_dout
      
  // ------------------------------
  //            WITHOUT
  // ------------------------------
  } else {
    for (fs <- 0 until p.nFieldSlct) {
      w_lock(fs) := ~io.b_dout.ready(fs)
    } 

    // Outputs
    io.b_dout.valid := w_back.valid
    if (p.useFieldTag) io.b_dout.field.get := w_back.field.get
    io.b_dout.ctrl.get := w_back.ctrl.get        
  }

  // ******************************
  //        EXTERNAL ACCESS
  // ******************************
  for (fs <- 0 until p.nFieldSlct) {
    io.o_back(fs) := m_back.io.o_val.valid(fs)
  }

  // ******************************
  //             DEBUG
  // ******************************
  if (p.debug) {
    
  } 
}

class Mb4sReqSReg(p: Mb4sReqParams, useReg: Boolean) extends Module {
  val io = IO(new Bundle {  
    val b_port = Flipped(new Mb4sReqIO(p))

    val o_back = Output(Vec(p.nFieldSlct, Bool()))

    val i_slct = if (p.useFieldSlct) Some(Input(new SlctBus(p.nField, p.nPart, 1))) else None 
    val b_sout = new GenSRVIO(p, new Mb4sReqBus(p), UInt(0.W))  
  })  

  val m_reg = Module(new Mb4sReqDReg(p, useReg))
  val m_mux = Module(new GenSMux(p, new Mb4sReqBus(p), UInt(0.W)))

  m_reg.io.b_port <> io.b_port

  io.o_back := m_reg.io.o_back

  m_mux.io.b_din <> m_reg.io.b_dout
  if (p.useFieldSlct) m_mux.io.i_slct.get := io.i_slct.get
  io.b_sout <> m_mux.io.b_sout

  // ******************************
  //             DEBUG
  // ******************************
  if (p.debug) {
    
  } 
}

class Mb4sDataDReg(p: Mb4sDataParams) extends Module {
  val io = IO(new Bundle {  
    val b_port = Flipped(new Mb4sDataIO(p))

    val o_back = Output(Vec(p.nFieldSlct, Bool()))

    val b_dout = new GenDRVIO(p, UInt(0.W), UInt((p.nDataByte * 8).W))
  })

  val w_lock = Wire(Vec(p.nFieldSlct, Bool()))

  // ******************************
  //         OUTPUT REGISTER
  // ******************************
  val m_back = Module(new GenDReg(p, UInt(0.W), UInt((p.nDataByte * 8).W), false, false, false))

  // Write
  for (fs <- 0 until p.nFieldSlct) {
    m_back.io.i_flush(fs) := false.B

    io.b_port.ready(fs) := m_back.io.b_din.ready(fs)

    if (p.useFieldSlct) {
      m_back.io.b_din.valid(fs) := io.b_port.valid & w_lock(fs) & (fs.U === io.b_port.field.get)
    } else {
      m_back.io.b_din.valid(0) := io.b_port.valid & w_lock(fs)
    }    
    if (p.useFieldTag) m_back.io.b_din.field.get := io.b_port.field.get
    m_back.io.b_din.data.get(fs) := io.b_port.data
  }

  // Read
  for (fs <- 0 until p.nFieldSlct) {
    w_lock(fs) := ~io.b_dout.ready(fs)

    m_back.io.b_dout.ready(fs) := ~w_lock(fs) 
    when (m_back.io.b_dout.valid(fs)) {
      io.b_dout.valid(fs) := true.B
      if (p.useFieldTag) io.b_dout.field.get := m_back.io.b_dout.field.get
      io.b_dout.data.get(fs) := m_back.io.b_dout.data.get(fs)
    }.otherwise {
      if (p.useFieldSlct) {
        io.b_dout.valid(fs) := io.b_port.valid & (io.b_port.field.get === fs.U)
      } else {
        io.b_dout.valid(0) := io.b_port.valid
      }  
      if (p.useFieldTag) io.b_dout.field.get := io.b_port.field.get
      io.b_dout.data.get(fs) := io.b_port.data
    }
  } 

  // ******************************
  //        EXTERNAL ACCESS
  // ******************************
  for (fs <- 0 until p.nFieldSlct) {
    io.o_back(fs) := m_back.io.o_val.valid(fs)
  }

  // ******************************
  //             DEBUG
  // ******************************
  if (p.debug) {
    
  } 
}

class Mb4sDataSReg(p: Mb4sDataParams) extends Module {
  val io = IO(new Bundle {  
    val b_port = Flipped(new Mb4sDataIO(p))

    val o_back = Output(Vec(p.nFieldSlct, Bool()))

    val i_slct = if (p.useFieldSlct) Some(Input(new SlctBus(p.nField, p.nPart, 1))) else None 
    val b_sout = new GenSRVIO(p, UInt(0.W), UInt((p.nDataByte * 8).W))
  })  

  val m_reg = Module(new Mb4sDataDReg(p))
  val m_mux = Module(new GenSMux(p, UInt(0.W), UInt((p.nDataByte * 8).W)))

  m_reg.io.b_port <> io.b_port

  io.o_back := m_reg.io.o_back

  m_mux.io.b_din <> m_reg.io.b_dout
  if (p.useFieldSlct) m_mux.io.i_slct.get := io.i_slct.get
  io.b_sout <> m_mux.io.b_sout

  // ******************************
  //             DEBUG
  // ******************************
  if (p.debug) {
    
  } 
}

object Mb4sReqDReg extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Mb4sReqDReg(Mb4sConfig6, true), args)
}

object Mb4sReqSReg extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Mb4sReqSReg(Mb4sConfig6, true), args)
}

object Mb4sDataDReg extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Mb4sDataDReg(Mb4sConfig6), args)
}

object Mb4sDataSReg extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Mb4sDataSReg(Mb4sConfig6), args)
}