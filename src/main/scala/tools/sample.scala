/*
 * File: sample.scala                                                          *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:26:53 pm                                       *
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


class Sample [T <: Data](gen: T, default: T) extends Module {
  val io = IO(new Bundle {
    val i_data = Input(gen)
    val o_data = Output(gen)
  })

  // Initialisation
  val w_sample = Wire(Vec(2, gen))
  w_sample(0) := default
  w_sample(1) := default

  val r_sample = RegInit(w_sample)

  // Connection
  r_sample(0) := io.i_data
  r_sample(1) := r_sample(0)
  io.o_data := r_sample(1)
}

object Sample extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Sample(UInt(8.W), DontCare), args)
}
