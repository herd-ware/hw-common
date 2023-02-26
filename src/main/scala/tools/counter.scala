/*
 * File: counter.scala                                                         *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:27:02 pm                                       *
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


class Counter (nBit: Int) extends Module {
  val io = IO(new Bundle {
    val i_init = Input(Bool())
    val i_en = Input(Bool())
    val i_limit = Input(UInt(nBit.W))

    val o_flag = Output(Bool())
    val o_val = Output(UInt(nBit.W))
  })

  val r_counter = RegInit(0.U(nBit.W))
  val w_counter = Wire(UInt(nBit.W))
  val w_flag = Wire(Bool())

  w_flag := (r_counter === (io.i_limit - 1.U))

  when (io.i_init) {
    w_counter := 0.U
  }.elsewhen (io.i_en & w_flag) {
    w_counter := 0.U
  }.elsewhen (io.i_en) {
    w_counter := r_counter + 1.U
  }.otherwise {
    w_counter := r_counter
  }

  r_counter := w_counter

  io.o_flag := w_flag
  io.o_val := r_counter
}

object Counter extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Counter(32), args)
}
