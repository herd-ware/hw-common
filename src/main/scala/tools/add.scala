/*
 * File: add.scala                                                             *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:27:00 pm                                       *
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


class AdderTree(nIn: Int, nInBit: Int) extends Module {
  val io = IO(new Bundle {
    val i_in = Input(Vec(nIn, UInt(nInBit.W)))
    val o_out = Output(UInt((nInBit + log2Ceil(nIn)).W))
  })

  val w_tmp = Wire(Vec(2 * pow(2, log2Ceil(nIn)).toInt - 1, UInt((nInBit + log2Ceil(nIn)).W)))

  var nadder = 0
  for (i <- log2Ceil(nIn) to 0 by -1) {
    for (n <- 0 until pow(2, i).toInt) {
      if (i == log2Ceil(nIn)) {
        if (n < nIn) {
          w_tmp(n) := io.i_in(n)
        } else {
          w_tmp(n) := 0.U((nInBit + log2Ceil(nIn)).W)
        }
      } else {
        w_tmp(nadder + n) := w_tmp(nadder - pow(2, i + 1).toInt + 2 * n) + w_tmp(nadder - pow(2, i + 1).toInt + 2 * n + 1)
      }
    }

    nadder = nadder + pow(2, i).toInt
  }

  io.o_out := w_tmp(2 * pow(2, log2Ceil(nIn)).toInt - 2)
}

object AdderTree extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new AdderTree(6, 2), args)
}
