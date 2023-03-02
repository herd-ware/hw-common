/*
 * File: lru.scala                                                             *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:39:18 pm                                       *
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


class LruPolicy (useField: Boolean, nField: Int, nAccess: Int, nLine: Int) extends ReplacePolicy(useField, nField, nAccess, nLine) {
  // ******************************
  //        HISTORY REGISTERS
  // ******************************
  val r_counter = Reg(Vec(nLine, UInt(log2Ceil(nLine).W)))

  // ******************************
  //            ZEROING
  // ******************************
  // ------------------------------
  //            ACCESS
  // ------------------------------
  val w_zav = Wire(Vec(nLine, Bool()))

  for (l <- 0 until nLine) {
    w_zav(l) := false.B
    for(a <- 0 until nAccess) {
      when(io.b_acc(a).valid & (l.U === io.b_acc(a).line)) {
        w_zav(l) := true.B
      }
    }
  }

  // ------------------------------
  //            REPLACE
  // ------------------------------
  val w_zrep_av = Wire(Vec(nLine, Bool()))
  val w_zrep_line = Wire(Vec(nLine, UInt(log2Ceil(nLine).W)))
  val w_zrep_counter = Wire(Vec(nLine, UInt(log2Ceil(nLine).W)))

  w_zrep_av(0) := io.b_rep.valid & w_line_av(0) & ~w_zav(0)
  w_zrep_line(0) := 0.U
  w_zrep_counter(0) := r_counter(0)
  for (l <- 1 until nLine) {
    when(io.b_rep.valid & w_line_av(l) & ~w_zav(l) & ~w_zrep_av(l - 1)) {
      w_zrep_av(l) := true.B
      w_zrep_line(l) := l.U
      w_zrep_counter(l) := r_counter(l)
    }.elsewhen (io.b_rep.valid & w_line_av(l) & ~w_zav(l) & (r_counter(l) > w_zrep_counter(l - 1))) {
      w_zrep_av(l) := true.B
      w_zrep_line(l) := l.U
      w_zrep_counter(l) := r_counter(l)
    }.otherwise {
      w_zrep_av(l) := w_zrep_av(l - 1)
      w_zrep_line(l) := w_zrep_line(l - 1)
      w_zrep_counter(l) := w_zrep_counter(l - 1)
    }
  }

  io.b_rep.done := w_zrep_av(nLine - 1)
  io.b_rep.line := w_zrep_line(nLine - 1)

  // ------------------------------
  //            FINALE
  // ------------------------------
  val w_znew = Wire(Vec(nLine, Bool()))
  for (l <- 0 until nLine) {
    w_znew(l) := (w_zav(l) | w_line_flush(l))
    when ((l.U === w_zrep_line(nLine - 1)) & w_zrep_av(nLine - 1)) {
      w_znew(l) := true.B
    }
  }

  // ******************************
  //        VALUE INVENTORY
  // ******************************
  val w_inventory = Wire(Vec(nLine, Bool()))
  for (l0 <- 0 until nLine) {
    w_inventory(l0) := false.B
    for (l1 <- 0 until nLine) {
      when(~w_znew(l1) & (l0.U === r_counter(l1))) {
        w_inventory(l0) := true.B
      }
    }
  }

  // ******************************
  //        UPDATE COUNTER
  // ******************************
  for (l0 <- 0 until nLine) {
    for (l1 <- 0 until nLine - 1) {
      when(w_znew(l0)) {
        r_counter(l0) := 0.U
      }.elsewhen ((l1.U === r_counter(l0)) & ~w_inventory(l1 + 1)) {
        r_counter(l0) := r_counter(l0) + 1.U
      }
    }
  }

  // ******************************
  //              FREE
  // ******************************
  for (l <- 0 until nLine) {
    io.b_line(l).free := (r_counter(l) === 0.U)
    if (useField) io.b_rsrc.get.state(l).free := (r_counter(l) === 0.U)
  }
}

object LruPolicy extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new LruPolicy(true, 1, 2, 8), args)
}
