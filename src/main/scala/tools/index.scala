/*
 * File: index.scala                                                           *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:26:55 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.tools

import chisel3._
import chisel3.util._
import scala.math._


class SlctIndex(nBit: Int) extends Module {
  val io = IO(new Bundle {
    val i_index = Input(UInt(nBit.W))
    val i_max = Input(UInt(nBit.W))
    val o_slct = Output(UInt(nBit.W))
  })

  val w_here = Wire(Vec(nBit, Bool()))
  val w_index = Wire(Vec(nBit, UInt(nBit.W)))

  for (b <- 1 to nBit) {
    w_here(b - 1) := false.B
    w_index(b - 1) := 0.U
    for (i <- pow(2, b - 1).toInt until pow(2, b).toInt) {
      when(i.U === io.i_index(b - 1, 0) & io.i_max > io.i_index(b - 1, 0)) {
        w_here(b - 1) := true.B
        w_index(b - 1) := io.i_index(b - 1, 0)
      }
    }
  }

  val w_find = Wire(Vec(nBit + 1, Bool()))
  w_find(0) := false.B

  io.o_slct := 0.U
  for (b <- 0 until nBit) {
    when (w_here(nBit - 1 - b) & ~w_find(b)) {
      w_find(b + 1) := true.B
      io.o_slct := w_index(nBit - 1 - b)
    }.otherwise {
      w_find(b + 1) := w_find(b)
    }
  }
}

object SlctIndex extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new SlctIndex(4), args)
}
