/*
 * File: mux.scala                                                             *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:25:51 pm                                       *
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


class GenSMux[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends Module {  
  // ******************************
  //             I/Os
  // ******************************
  val io = IO(new Bundle {
    val b_din = Flipped(new GenDRVIO(p, tc, td))

    val i_slct = if (p.useDomeSlct) Some(Input(new SlctBus(p.nDome, p.nPart, 1))) else None

    val b_sout = new GenSRVIO(p, tc, td)
  })

  if (p.useDomeSlct) {
    io.b_sout := DontCare
    io.b_sout.valid := true.B
    io.b_sout.dome.get := io.i_slct.get.dome
    for (ds <- 0 until p.nDomeSlct) {
      io.b_din.ready(ds) := io.b_sout.ready & (ds.U === io.i_slct.get.dome)

      when (ds.U === io.i_slct.get.dome) {
        io.b_sout.valid := io.b_din.valid(ds)
        if (tc.getWidth > 0) io.b_sout.ctrl.get := io.b_din.ctrl.get(ds)
        if (td.getWidth > 0) io.b_sout.data.get := io.b_din.data.get(ds)
      }
    }
  } else {
    io.b_din.ready(0) := io.b_sout.ready

    io.b_sout.valid := io.b_din.valid(0)
    if (p.useDome) io.b_sout.dome.get := io.b_din.dome.get
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

    val i_slct = if (p.useDomeSlct) Some(Input(new SlctBus(p.nDome, p.nPart, 1))) else None

    val b_dout = new GenDRVIO(p, tc, td)
  })

  if (p.useDomeSlct) {
    io.b_sin.ready := false.B
    for (ds <- 0 until p.nDomeSlct) {
      when (ds.U === io.i_slct.get.dome) {
        io.b_sin.ready := io.b_dout.ready(ds)
      }

      io.b_dout.valid(ds) := io.b_sin.valid & (ds.U === io.i_slct.get.dome)
      if (tc.getWidth > 0) io.b_dout.ctrl.get(ds) := io.b_sin.ctrl.get
      if (td.getWidth > 0) io.b_dout.data.get(ds) := io.b_sin.data.get
    }
  } else {
    io.b_sin.ready := io.b_dout.ready(0)
    
    io.b_dout.valid(0) := io.b_sin.valid
    if (p.useDomeTag) io.b_dout.dome.get := io.b_sin.dome.get
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