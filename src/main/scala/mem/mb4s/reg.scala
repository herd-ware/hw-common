/*
 * File: reg.scala                                                             *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:28:43 pm                                       *
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
import herd.common.dome._


class Mb4sReqDReg(p: Mb4sReqParams, useReg: Boolean) extends Module {
  val io = IO(new Bundle {  
    val b_port = Flipped(new Mb4sReqIO(p))

    val o_back = Output(Vec(p.nDomeSlct, Bool()))

    val b_dout = new GenDRVIO(p, new Mb4sReqBus(p), UInt(0.W))
  })  

  val w_lock = Wire(Vec(p.nDomeSlct, Bool()))

  // ******************************
  //          BACK REGISTER
  // ******************************
  val m_back = Module(new GenDReg(p, new Mb4sReqBus(p), UInt(0.W), false, false, false))

  val w_back = Wire(new GenDVBus(p, new Mb4sReqBus(p), UInt(0.W)))  

  for (ds <- 0 until p.nDomeSlct) {
    io.b_port.ready(ds) := m_back.io.b_din.ready(ds)

    // Write
    m_back.io.i_flush(ds) := false.B
    if (p.useDomeSlct) {
      m_back.io.b_din.valid(ds) := io.b_port.valid & (ds.U === io.b_port.dome.get) & w_lock(ds)
    } else {
      m_back.io.b_din.valid(0) := io.b_port.valid & w_lock(0)
    }
    if (p.useDomeTag) m_back.io.b_din.dome.get := io.b_port.dome.get
    m_back.io.b_din.ctrl.get(ds) := io.b_port.ctrl

    // Read
    m_back.io.b_dout.ready(ds) := ~w_lock(ds)
    when (m_back.io.b_dout.valid(ds)) {
      w_back.valid(ds) := true.B
      if (p.useDomeTag) w_back.dome.get := m_back.io.b_dout.dome.get
      w_back.ctrl.get(ds) := m_back.io.b_dout.ctrl.get(ds)
    }.otherwise {
      if (p.useDomeSlct) {
        w_back.valid(ds) := io.b_port.valid & (ds.U === io.b_port.dome.get)
      } else {
        w_back.valid(0) := io.b_port.valid
      }
      if (p.useDomeTag) w_back.dome.get := m_back.io.b_dout.dome.get
      w_back.ctrl.get(ds) := io.b_port.ctrl
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
    for (ds <- 0 until p.nDomeSlct) {
      m_reg.io.i_flush(ds) := false.B

      w_lock(ds) := ~m_reg.io.b_din.ready(ds)

      m_reg.io.b_din.valid(ds) := w_back.valid(ds)
      if (p.useDomeTag) m_reg.io.b_din.dome.get := w_back.dome.get
      m_reg.io.b_din.ctrl.get(ds) := w_back.ctrl.get(ds)
    } 
      
    // Outputs
    io.b_dout <> m_reg.io.b_dout
      
  // ------------------------------
  //            WITHOUT
  // ------------------------------
  } else {
    for (ds <- 0 until p.nDomeSlct) {
      w_lock(ds) := ~io.b_dout.ready(ds)
    } 

    // Outputs
    io.b_dout.valid := w_back.valid
    if (p.useDomeTag) io.b_dout.dome.get := w_back.dome.get
    io.b_dout.ctrl.get := w_back.ctrl.get        
  }

  // ******************************
  //        EXTERNAL ACCESS
  // ******************************
  for (ds <- 0 until p.nDomeSlct) {
    io.o_back(ds) := m_back.io.o_val.valid(ds)
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

    val o_back = Output(Vec(p.nDomeSlct, Bool()))

    val i_slct = if (p.useDomeSlct) Some(Input(new SlctBus(p.nDome, p.nPart, 1))) else None 
    val b_sout = new GenSRVIO(p, new Mb4sReqBus(p), UInt(0.W))  
  })  

  val m_reg = Module(new Mb4sReqDReg(p, useReg))
  val m_mux = Module(new GenSMux(p, new Mb4sReqBus(p), UInt(0.W)))

  m_reg.io.b_port <> io.b_port

  io.o_back := m_reg.io.o_back

  m_mux.io.b_din <> m_reg.io.b_dout
  if (p.useDomeSlct) m_mux.io.i_slct.get := io.i_slct.get
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

    val o_back = Output(Vec(p.nDomeSlct, Bool()))

    val b_dout = new GenDRVIO(p, UInt(0.W), UInt((p.nDataByte * 8).W))
  })

  val w_lock = Wire(Vec(p.nDomeSlct, Bool()))

  // ******************************
  //         OUTPUT REGISTER
  // ******************************
  val m_back = Module(new GenDReg(p, UInt(0.W), UInt((p.nDataByte * 8).W), false, false, false))

  // Write
  for (ds <- 0 until p.nDomeSlct) {
    m_back.io.i_flush(ds) := false.B

    io.b_port.ready(ds) := m_back.io.b_din.ready(ds)

    if (p.useDomeSlct) {
      m_back.io.b_din.valid(ds) := io.b_port.valid & w_lock(ds) & (ds.U === io.b_port.dome.get)
    } else {
      m_back.io.b_din.valid(0) := io.b_port.valid & w_lock(ds)
    }    
    if (p.useDomeTag) m_back.io.b_din.dome.get := io.b_port.dome.get
    m_back.io.b_din.data.get(ds) := io.b_port.data
  }

  // Read
  for (ds <- 0 until p.nDomeSlct) {
    w_lock(ds) := ~io.b_dout.ready(ds)

    m_back.io.b_dout.ready(ds) := ~w_lock(ds) 
    when (m_back.io.b_dout.valid(ds)) {
      io.b_dout.valid(ds) := true.B
      if (p.useDomeTag) io.b_dout.dome.get := m_back.io.b_dout.dome.get
      io.b_dout.data.get(ds) := m_back.io.b_dout.data.get(ds)
    }.otherwise {
      if (p.useDomeSlct) {
        io.b_dout.valid(ds) := io.b_port.valid & (io.b_port.dome.get === ds.U)
      } else {
        io.b_dout.valid(0) := io.b_port.valid
      }  
      if (p.useDomeTag) io.b_dout.dome.get := io.b_port.dome.get
      io.b_dout.data.get(ds) := io.b_port.data
    }
  } 

  // ******************************
  //        EXTERNAL ACCESS
  // ******************************
  for (ds <- 0 until p.nDomeSlct) {
    io.o_back(ds) := m_back.io.o_val.valid(ds)
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

    val o_back = Output(Vec(p.nDomeSlct, Bool()))

    val i_slct = if (p.useDomeSlct) Some(Input(new SlctBus(p.nDome, p.nPart, 1))) else None 
    val b_sout = new GenSRVIO(p, UInt(0.W), UInt((p.nDataByte * 8).W))
  })  

  val m_reg = Module(new Mb4sDataDReg(p))
  val m_mux = Module(new GenSMux(p, UInt(0.W), UInt((p.nDataByte * 8).W)))

  m_reg.io.b_port <> io.b_port

  io.o_back := m_reg.io.o_back

  m_mux.io.b_din <> m_reg.io.b_dout
  if (p.useDomeSlct) m_mux.io.i_slct.get := io.i_slct.get
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