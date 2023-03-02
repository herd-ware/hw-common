/*
 * File: slct.scala                                                            *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-03-02 01:50:24 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.field

import chisel3._
import chisel3.util._


// ******************************
//         STATIC SELECT
// ******************************
class StaticSlct (nField: Int, nPart: Int, nStep: Int) extends Module {
  val io = IO(new Bundle {
    val i_weight = Input(Vec(nField, UInt(log2Ceil(nPart + 1).W)))

    val o_slct = Output(new SlctBus(nField, nPart, nStep))
  })

  // ******************************
  //          INITIALIZE
  // ******************************
  val init_field = Wire(UInt(log2Ceil(nField).W))
  val init_turn = Wire(UInt(log2Ceil(nPart + 1).W))
  val init_step = Wire(UInt(log2Ceil(nStep).W))

  init_field := 0.U
  init_turn := 0.U  
  init_step := 0.U  

  val r_next = RegInit(init_field)
  val r_field = RegInit(init_field)
  val r_turn = RegInit(init_turn)
  val r_step = RegInit(init_step)

  // ******************************
  //          STEP & TURN
  // ******************************
  val w_last_step = Wire(Bool())
  val w_last_turn = Wire(Bool())

  w_last_step := (r_step === (nStep - 1).U)
  w_last_turn := (r_turn === (io.i_weight(r_next) - 1.U))

  when (w_last_step) {
    r_step := 0.U
  }.otherwise {
    r_step := r_step + 1.U
  }

  // ******************************
  //             NEXT
  // ******************************
  if (nField > 1) {
    // ------------------------------
    //          ACTIVEFIELD
    // ------------------------------
    val w_field_act = Wire(Vec(nField, Bool()))

    for (f <- 0 until nField) {
      w_field_act(f) := (io.i_weight(f) =/= 0.U)
    }

    // ------------------------------
    //             SLCT
    // ------------------------------
    val w_next_low = Wire(Vec(nField + 1, Bool()))
    val w_next_high = Wire(Vec(nField + 1, Bool()))
    val w_next = Wire(UInt(log2Ceil(nField).W))

    w_next_low(0) := false.B
    w_next_high(0) := false.B
    w_next := 0.U

    for (f <- 0 until nField) {
      when (f.U <= r_next) {
        w_next_high(f + 1) := false.B
        when (~w_next_low(f)) {
          w_next_low(f + 1) := w_field_act(f)
          w_next := f.U
        }.otherwise {
          w_next_low(f + 1) := w_next_low(f)
        }
      }.otherwise {
        w_next_low(f + 1) := w_next_low(f)
        when (~w_next_high(f) & ~w_next_low(f)) {
          w_next_high(f + 1) := w_field_act(f)
          w_next := f.U
        }.elsewhen (~w_next_high(f) & w_next_low(f) & w_field_act(f)) {
          w_next_high(f + 1) := true.B
          w_next := f.U
        }.otherwise {
          w_next_high(f + 1) := w_next_high(f)
        }
      }
    }

    when (w_last_turn & w_last_step) {
      r_next := w_next
    }
  }

  // ******************************
  //            FIELD
  // ******************************
  when (w_last_step) {
    r_field := r_next
  }  

  // ******************************
  //            OUTPUT
  // ******************************
  io.o_slct.field := r_field
  io.o_slct.next := r_next
  io.o_slct.step := r_step
}

object StaticSlct extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new StaticSlct(4, 4, 8), args)
}