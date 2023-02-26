/*
 * File: reg.scala                                                             *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:25:57 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.gen

import chisel3._
import chisel3.util._

import herd.common.dome._


class GenReg[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD, useReset: Boolean, useUpdate: Boolean, isPipe: Boolean) extends Module {  
  // ******************************
  //             I/Os
  // ******************************
  val io = IO(new Bundle {
    val i_reset = if (useReset) Some(Input(new GenVBus(p, tc, td))) else None
    val i_flush = Input(Bool())

    val b_in = Flipped(new GenRVIO(p, tc, td))
    val i_up = if (useUpdate) Some(Input(new GenBus(p, tc, td))) else None

    val o_val = Output(new GenVBus(p, tc, td))
    val o_reg = Output(new GenVBus(p, tc, td))

    val b_out = new GenRVIO(p, tc, td)
  })

  // ******************************
  //         INIT REGISTERS
  // ******************************
  val init_reg = Wire(new GenVBus(p, tc, td))

  if (useReset) {
    init_reg := io.i_reset.get
  } else {
    init_reg := DontCare
    init_reg.valid := false.B
  }  

  val r_reg = RegInit(init_reg)

  // ******************************
  //            OUTPUT
  // ******************************  
  r_reg.valid := r_reg.valid & ~io.b_out.ready
  io.b_out.valid := r_reg.valid
  if (tc.getWidth > 0) io.b_out.ctrl.get := r_reg.ctrl.get
  if (td.getWidth > 0) io.b_out.data.get := r_reg.data.get

  // ******************************
  //            INPUT
  // ******************************
  val w_lock = Wire(Bool())

  if (isPipe) {
    w_lock := r_reg.valid & ~io.b_out.ready
  } else {
    w_lock := r_reg.valid
  }
  io.b_in.ready := ~w_lock
  
  when (io.b_in.valid & ~w_lock) {
    r_reg.valid := true.B
    if (tc.getWidth > 0) r_reg.ctrl.get := io.b_in.ctrl.get
    if (td.getWidth > 0) r_reg.data.get := io.b_in.data.get
  }.otherwise {
    if (useUpdate) {
      when (r_reg.valid & ~io.b_out.ready) {
        if (tc.getWidth > 0) r_reg.ctrl.get := io.i_up.get.ctrl.get
        if (td.getWidth > 0) r_reg.data.get := io.i_up.get.data.get
      }            
    }
  }   

  // ******************************
  //          FLUSH & ZERO
  // ******************************
  when (io.i_flush) {
    r_reg.valid := false.B
  }  

  // ******************************
  //        EXTERNAL ACCESS
  // ******************************
  io.o_val := r_reg
  io.o_reg := r_reg
}

class GenDReg[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD, useReset: Boolean, useUpdate: Boolean, isPipe: Boolean) extends Module {  
  // ******************************
  //             I/Os
  // ******************************
  val io = IO(new Bundle {
    val i_reset = if (useReset) Some(Input(new GenDVBus(p, tc, td))) else None
    val i_flush = Input(Vec(p.nDomeSlct, Bool()))

    val b_din = Flipped(new GenDRVIO(p, tc, td))
    val i_up = if (useUpdate) Some(Input(new GenDBus(p, tc, td))) else None

    val o_val = Output(new GenDVBus(p, tc, td))
    val o_reg = Output(new GenDVBus(p, tc, td))
    
    val b_dout = new GenDRVIO(p, tc, td)
  })

  // ******************************
  //         INIT REGISTERS
  // ******************************  
  val init_reg = Wire(new GenDVBus(p, tc, td))

  if (useReset) {
    init_reg := io.i_reset.get
  } else {
    init_reg := DontCare
    for (ds <- 0 until p.nDomeSlct) {    
      init_reg.valid(ds) := false.B
    }
  }  

  val r_reg = RegInit(init_reg)

  // ******************************
  //            OUTPUT
  // ******************************  
  for (ds <- 0 until p.nDomeSlct) {
    r_reg.valid(ds) := r_reg.valid(ds) & ~io.b_dout.ready(ds)
    io.b_dout.valid(ds) := r_reg.valid(ds)
    if (p.useDomeTag)     io.b_dout.dome.get := r_reg.dome.get
    if (tc.getWidth > 0)  io.b_dout.ctrl.get(ds) := r_reg.ctrl.get(ds)
    if (td.getWidth > 0)  io.b_dout.data.get(ds) := r_reg.data.get(ds)
  }

  // ******************************
  //             INPUT
  // ******************************
  val w_lock = Wire(Vec(p.nDomeSlct, Bool()))

  if (p.useDomeSlct) {
    for (ds <- 0 until p.nDomeSlct) {
      if (isPipe) {
        w_lock(ds) := r_reg.valid(ds) & ~io.b_dout.ready(ds)
      } else {
        w_lock(ds) := r_reg.valid(ds)
      }

      io.b_din.ready(ds) := ~w_lock(ds)
    
      when (io.b_din.valid(ds) & ~w_lock(ds)) {
        r_reg.valid(ds) := true.B
        if (tc.getWidth > 0) r_reg.ctrl.get(ds) := io.b_din.ctrl.get(ds)
        if (td.getWidth > 0) r_reg.data.get(ds) := io.b_din.data.get(ds)
      }.otherwise {
        if (useUpdate) {
          for (ds <- 0 until p.nDomeSlct) {
            when (r_reg.valid(ds) & ~io.b_dout.ready(ds)) {
              if (tc.getWidth > 0) r_reg.ctrl.get(ds) := io.i_up.get.ctrl.get(ds)
              if (td.getWidth > 0) r_reg.data.get(ds) := io.i_up.get.data.get(ds)
            }
          }    
        }
      } 
    }
  } else {
    if (isPipe) {
      w_lock(0) := r_reg.valid(0) & ~io.b_dout.ready(0)
    } else {
      w_lock(0) := r_reg.valid(0)
    }

    io.b_din.ready(0) := ~w_lock(0)
    
    when (io.b_din.valid(0) & ~w_lock(0)) {
      r_reg.valid(0) := true.B
      if (p.useDomeTag) r_reg.dome.get := io.b_din.dome.get
      if (tc.getWidth > 0) r_reg.ctrl.get(0) := io.b_din.ctrl.get(0)
      if (td.getWidth > 0) r_reg.data.get(0) := io.b_din.data.get(0)
    }.otherwise {
      if (useUpdate) {
        when (r_reg.valid(0) & ~io.b_dout.ready(0)) {
          if (tc.getWidth > 0) r_reg.ctrl.get(0) := io.i_up.get.ctrl.get(0)
          if (td.getWidth > 0) r_reg.data.get(0) := io.i_up.get.data.get(0)
        }            
      }
    } 
  }

  // ******************************
  //          FLUSH & ZERO
  // ******************************
  for (ds <- 0 until p.nDomeSlct) {
    when (io.i_flush(ds)) {
      r_reg.valid(ds) := false.B
    }
  }

  // ******************************
  //        EXTERNAL ACCESS
  // ******************************
  io.o_val := r_reg
  io.o_reg := r_reg
}

class GenSReg[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD, useReset: Boolean, useUpdate: Boolean, isPipe: Boolean) extends Module {  
  // ******************************
  //             I/Os
  // ******************************
  val io = IO(new Bundle {
    val i_reset = if (useReset) Some(Input(new GenDVBus(p, tc, td))) else None
    val i_flush = Input(Vec(p.nDomeSlct, Bool()))

    val i_slct_in = if (p.useDomeSlct) Some(Input(new SlctBus(p.nDome, p.nPart, 1))) else None
    val b_sin = Flipped(new GenSRVIO(p, tc, td))
    val i_up = if (useUpdate) Some(Input(new GenDBus(p, tc, td))) else None

    val o_val = Output(new GenDVBus(p, tc, td))
    val o_reg = Output(new GenDVBus(p, tc, td))
    
    val i_slct_out = if (p.useDomeSlct) Some(Input(new SlctBus(p.nDome, p.nPart, 1))) else None
    val b_sout = new GenSRVIO(p, tc, td)
  })

  val m_in = Module(new GenSDemux(p, tc, td))
  val m_reg = Module(new GenDReg(p, tc, td, useReset, useUpdate, isPipe))
  val m_out = Module(new GenSMux(p, tc, td))

  if (useReset) m_reg.io.i_reset.get := io.i_reset.get
  m_reg.io.i_flush := io.i_flush

  // Write
  if (p.useDomeSlct) m_in.io.i_slct.get := io.i_slct_in.get
  m_in.io.b_sin <> io.b_sin

  // Register
  m_reg.io.b_din <> m_in.io.b_dout
  if (useUpdate) m_reg.io.i_up.get := io.i_up.get
  io.o_val := m_reg.io.o_val
  io.o_reg := m_reg.io.o_reg

  // Read
  if (p.useDomeSlct) m_out.io.i_slct.get := io.i_slct_out.get
  m_out.io.b_din <> m_reg.io.b_dout
  io.b_sout <> m_out.io.b_sout
}


object GenReg extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new GenReg(GenConfigBase, UInt(8.W), UInt(8.W), false, false, false), args)
}

object GenDReg extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new GenDReg(GenConfigBase, UInt(8.W), UInt(8.W), false, false, true), args)
}

object GenSReg extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new GenSReg(GenConfigBase, UInt(8.W), UInt(8.W), false, false, true), args)
}

