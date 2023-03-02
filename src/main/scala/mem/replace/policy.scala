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

import herd.common.field._


class LineIO extends Bundle {
  val av = Input(Bool())
  val flush = Input(Bool())
  val free = Output(Bool())
}

class AccessIO (nLine: Int) extends Bundle {
  val valid = Input(Bool())
  val line = Input(UInt(log2Ceil(nLine).W))
}

class ReplaceIO (useField: Boolean, nField: Int, nLine: Int) extends Bundle {
  val valid = Input(Bool())
  val field = if (useField) Some(Input(UInt(log2Ceil(nField).W))) else None
  val fixed = Input(UInt(log2Ceil(nLine).W))
  val done = Output(Bool())
  val line = Output(UInt(log2Ceil(nLine).W))
}

abstract class ReplacePolicy (useField: Boolean, nField: Int, nAccess: Int, nLine: Int) extends Module {
  // ******************************
  //             IOs
  // ******************************
  val io = IO(new Bundle {
    val b_rsrc = if (useField) Some(new NRsrcIO(1, nField, nLine)) else None

    val b_line = Vec(nLine, new LineIO())
    val b_acc = Vec(nAccess, new AccessIO(nLine))
    val b_rep = new ReplaceIO(useField, nField, nLine)
  })

  // ******************************
  //            INTERFACE
  // ******************************
  val w_line_flush = Wire(Vec(nLine, Bool()))
  val w_line_av = Wire(Vec(nLine, Bool()))

  for (l <- 0 until nLine) {
    if (useField) {
      w_line_flush(l) := io.b_line(l).flush | io.b_rsrc.get.state(l).flush
      w_line_av(l) := io.b_line(l).av & ~w_line_flush(l) & io.b_rsrc.get.state(l).valid & (io.b_rep.field.get === io.b_rsrc.get.state(l).field)
    } else {
      w_line_flush(l) := io.b_line(l).flush
      w_line_av(l) := io.b_line(l).av & ~w_line_flush(l)
    }
  }
}
