/*
 * File: bitplru.scala                                                         *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:39:12 pm                                       *
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


class BitPLruPolicy (useDome: Boolean, nDome: Int, nAccess: Int, nLine: Int) extends ReplacePolicy(useDome, nDome, nAccess, nLine) {
  val r_mru = RegInit(VecInit(Seq.fill(nLine)(false.B)))

  // ******************************
  //            ACCESS
  // ******************************
  val w_mru = Wire(Vec(nLine, Bool()))
  w_mru := r_mru

  for (a <- 0 until nAccess) {
    for (l <- 0 until nLine) {
      when (io.b_acc(a).valid & (l.U === io.b_acc(a).line)) {
        w_mru(l) := true.B
      }
    }
  }

  // ******************************
  //            REPLACE
  // ******************************
  val w_zero = Wire(Vec(nLine, Bool()))
  val w_av = Wire(Vec(nLine, Bool()))
  val w_rep_line = Wire(UInt(log2Ceil(nLine).W))

  w_zero(0) := io.b_rep.valid & w_line_av(0) & ~r_mru(0)
  w_av(0) := io.b_rep.valid & w_line_av(0)
  w_rep_line := 0.U

  for (l <- 1 until nLine) {
    when(io.b_rep.valid & ~w_av(l - 1) & w_line_av(l)) {
      w_zero(l) := ~r_mru(l)
      w_av(l) := true.B
      w_rep_line := l.U
    }.elsewhen (io.b_rep.valid & w_av(l - 1) & w_line_av(l) & ~w_zero(l - 1) & ~r_mru(l)) {
      w_zero(l) := true.B
      w_av(l) := true.B
      w_rep_line := l.U
    }.otherwise {
      w_zero(l) := w_zero(l - 1)
      w_av(l) := w_av(l - 1)
    }
  }

  when (w_av(nLine - 1)) {
    w_mru(w_rep_line) := true.B
  }

  // ******************************
  //  MOST RECENTLY-USED REGISTERS
  // ******************************
  for (l <- 0 until nLine) {
    r_mru(l) := Mux(w_mru.asUInt.andR | w_line_flush(l), false.B, w_mru(l))
  }

  // ******************************
  //            OUTPUTS
  // ******************************
  io.b_rep.done := w_av(nLine - 1)
  io.b_rep.line := w_rep_line

  for (l <- 0 until nLine) {
    io.b_line(l).free := ~r_mru(l)
    if (useDome) io.b_rsrc.get.state(l).free := ~r_mru(l)
  }
}

object BitPLruPolicy extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new BitPLruPolicy(true, 1, 2, 8), args)
}
