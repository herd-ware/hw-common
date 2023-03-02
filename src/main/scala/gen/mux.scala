/*
 * File: mux.scala                                                             *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-03-02 08:53:06 am                                       *
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

import herd.common.field._


class GenSMux[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends Module {  
  // ******************************
  //             I/Os
  // ******************************
  val io = IO(new Bundle {
    val b_din = Flipped(new GenDRVIO(p, tc, td))

    val i_slct = if (p.useFieldSlct) Some(Input(new SlctBus(p.nField, p.nPart, 1))) else None

    val b_sout = new GenSRVIO(p, tc, td)
  })

  if (p.useFieldSlct) {
    io.b_sout := DontCare
    io.b_sout.valid := true.B
    io.b_sout.field.get := io.i_slct.get.field
    for (fs <- 0 until p.nFieldSlct) {
      io.b_din.ready(fs) := io.b_sout.ready & (fs.U === io.i_slct.get.field)

      when (fs.U === io.i_slct.get.field) {
        io.b_sout.valid := io.b_din.valid(fs)
        if (tc.getWidth > 0) io.b_sout.ctrl.get := io.b_din.ctrl.get(fs)
        if (td.getWidth > 0) io.b_sout.data.get := io.b_din.data.get(fs)
      }
    }
  } else {
    io.b_din.ready(0) := io.b_sout.ready

    io.b_sout.valid := io.b_din.valid(0)
    if (p.useField) io.b_sout.field.get := io.b_din.field.get
    if (tc.getWidth > 0) io.b_sout.ctrl.get := io.b_din.ctrl.get(0)
    if (td.getWidth > 0) io.b_sout.data.get := io.b_din.data.get(0)
  }
}

class GenSDemux[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends Module {  
  // ******************************
  //             I/Os
  // ******************************
  val io = IO(new Bundle {
    val b_sin = Flipped(new GenSRVIO(p, tc, td))

    val i_slct = if (p.useFieldSlct) Some(Input(new SlctBus(p.nField, p.nPart, 1))) else None

    val b_dout = new GenDRVIO(p, tc, td)
  })

  if (p.useFieldSlct) {
    io.b_sin.ready := false.B
    for (fs <- 0 until p.nFieldSlct) {
      when (fs.U === io.i_slct.get.field) {
        io.b_sin.ready := io.b_dout.ready(fs)
      }

      io.b_dout.valid(fs) := io.b_sin.valid & (fs.U === io.i_slct.get.field)
      if (tc.getWidth > 0) io.b_dout.ctrl.get(fs) := io.b_sin.ctrl.get
      if (td.getWidth > 0) io.b_dout.data.get(fs) := io.b_sin.data.get
    }
  } else {
    io.b_sin.ready := io.b_dout.ready(0)
    
    io.b_dout.valid(0) := io.b_sin.valid
    if (p.useFieldTag) io.b_dout.field.get := io.b_sin.field.get
    if (tc.getWidth > 0) io.b_dout.ctrl.get(0) := io.b_sin.ctrl.get
    if (td.getWidth > 0) io.b_dout.data.get(0) := io.b_sin.data.get
  }
}

object GenSMux extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new GenSMux(GenConfigBase, UInt(8.W), UInt(8.W)), args)
}

object GenSDemux extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new GenSDemux(GenConfigBase, UInt(8.W), UInt(8.W)), args)
}