/*
 * File: policy.scala                                                          *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:39:16 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.mem.replace

import scala._
import chisel3._
import chisel3.util._

import herd.common.dome._


class LineIO extends Bundle {
  val av = Input(Bool())
  val flush = Input(Bool())
  val free = Output(Bool())
}

class AccessIO (nLine: Int) extends Bundle {
  val valid = Input(Bool())
  val line = Input(UInt(log2Ceil(nLine).W))
}

class ReplaceIO (useDome: Boolean, nDome: Int, nLine: Int) extends Bundle {
  val valid = Input(Bool())
  val dome = if (useDome) Some(Input(UInt(log2Ceil(nDome).W))) else None
  val fixed = Input(UInt(log2Ceil(nLine).W))
  val done = Output(Bool())
  val line = Output(UInt(log2Ceil(nLine).W))
}

abstract class ReplacePolicy (useDome: Boolean, nDome: Int, nAccess: Int, nLine: Int) extends Module {
  // ******************************
  //             IOs
  // ******************************
  val io = IO(new Bundle {
    val b_rsrc = if (useDome) Some(new NRsrcIO(1, nDome, nLine)) else None

    val b_line = Vec(nLine, new LineIO())
    val b_acc = Vec(nAccess, new AccessIO(nLine))
    val b_rep = new ReplaceIO(useDome, nDome, nLine)
  })

  // ******************************
  //            INTERFACE
  // ******************************
  val w_line_flush = Wire(Vec(nLine, Bool()))
  val w_line_av = Wire(Vec(nLine, Bool()))

  for (l <- 0 until nLine) {
    if (useDome) {
      w_line_flush(l) := io.b_line(l).flush | io.b_rsrc.get.state(l).flush
      w_line_av(l) := io.b_line(l).av & ~w_line_flush(l) & io.b_rsrc.get.state(l).valid & (io.b_rep.dome.get === io.b_rsrc.get.state(l).dome)
    } else {
      w_line_flush(l) := io.b_line(l).flush
      w_line_av(l) := io.b_line(l).av & ~w_line_flush(l)
    }
  }
}
