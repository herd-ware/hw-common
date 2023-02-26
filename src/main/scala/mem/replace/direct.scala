/*
 * File: direct.scala                                                          *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:39:14 pm                                       *
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


class DirectPolicy (useDome: Boolean, nDome: Int, nAccess: Int, nLine: Int) extends ReplacePolicy(useDome, nDome, nAccess, nLine) {
  require((isPow2(nLine)), "Set must have a power of 2 number of lines for Direct replace policy.")
  require((!useDome || (nDome == 1)), "Direct replace ploicy is not possible with multiple domes.")

  // ******************************
  //             POLICY
  // ******************************
  if (nLine > 1) {
    io.b_rep.done := w_line_av(io.b_rep.fixed)
    io.b_rep.line := io.b_rep.fixed
  } else {
    io.b_rep.done := w_line_av(0)
    io.b_rep.line := 0.U
  }

  // ******************************
  //              FREE
  // ******************************
  for (l <- 0 until nLine) {
    io.b_line(l).free := true.B
    if (useDome) io.b_rsrc.get.state(l).free := true.B
  }  
}

object DirectPolicy extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new DirectPolicy(false, 1, 2, 8), args)
}
