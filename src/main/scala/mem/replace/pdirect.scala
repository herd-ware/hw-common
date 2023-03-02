/*
 * File: pdirect.scala                                                         *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:39:21 pm                                       *
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

import herd.common.tools._


class PDirectPolicy (useField: Boolean, nField: Int, nAccess: Int, nLine: Int) extends ReplacePolicy(useField, nField, nAccess, nLine) {
  require((useField && (nField > 1)) || (!isPow2(nLine)), "Pseudo direct policy must be used only with multiple fields or for a number of lines different to 2^n. Use direct replace policy instead.")

  // ******************************
  //             POLICY
  // ******************************
  if (useField && (nField > 1) && (nLine > 1)) {
    val slct = Module(new SlctIndex(log2Ceil(nLine)))
    slct.io.i_max := io.b_rsrc.get.weight(io.b_rep.field.get)
    slct.io.i_index := io.b_rep.fixed

    io.b_rep.done := false.B
    io.b_rep.line := 0.U
    for (l <- 0 until nLine) {
      when (w_line_av(l) & (io.b_rsrc.get.state(l).port === slct.io.o_slct)) {
        io.b_rep.done := true.B
        io.b_rep.line := l.U
      }
    }
  } else {
    io.b_rep.done := w_line_av(0)
    io.b_rep.line := 0.U
  }

  // ******************************
  //              FREE
  // ******************************
  for (l <- 0 until nLine) {
    io.b_line(l).free := true.B
    if (useField) io.b_rsrc.get.state(l).free := true.B
  }  
}

object PDirectPolicy extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new PDirectPolicy(false, 1, 2, 7), args)
}
