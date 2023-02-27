/*
 * File: slct.scala                                                            *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:25:05 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.dome

import chisel3._
import chisel3.util._


// ******************************
//         STATIC SELECT
// ******************************
class StaticSlct (nDome: Int, nPart: Int, nStep: Int) extends Module {
  val io = IO(new Bundle {
    val i_weight = Input(Vec(nDome, UInt(log2Ceil(nPart + 1).W)))

    val o_slct = Output(new SlctBus(nDome, nPart, nStep))
  })

  // ******************************
  //          INITIALIZE
  // ******************************
  val init_dome = Wire(UInt(log2Ceil(nDome).W))
  val init_turn = Wire(UInt(log2Ceil(nPart + 1).W))
  val init_step = Wire(UInt(log2Ceil(nStep).W))

  init_dome := 0.U
  init_turn := 0.U  
  init_step := 0.U  

  val r_next = RegInit(init_dome)
  val r_dome = RegInit(init_dome)
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
  if (nDome > 1) {
    // ------------------------------
    //          ACTIVE DOME
    // ------------------------------
    val w_dome_act = Wire(Vec(nDome, Bool()))

    for (d <- 0 until nDome) {
      w_dome_act(d) := (io.i_weight(d) =/= 0.U)
    }

    // ------------------------------
    //             SLCT
    // ------------------------------
    val w_next_low = Wire(Vec(nDome + 1, Bool()))
    val w_next_high = Wire(Vec(nDome + 1, Bool()))
    val w_next = Wire(UInt(log2Ceil(nDome).W))

    w_next_low(0) := false.B
    w_next_high(0) := false.B
    w_next := 0.U

    for (d <- 0 until nDome) {
      when (d.U <= r_next) {
        w_next_high(d + 1) := false.B
        when (~w_next_low(d)) {
          w_next_low(d + 1) := w_dome_act(d)
          w_next := d.U
        }.otherwise {
          w_next_low(d + 1) := w_next_low(d)
        }
      }.otherwise {
        w_next_low(d + 1) := w_next_low(d)
        when (~w_next_high(d) & ~w_next_low(d)) {
          w_next_high(d + 1) := w_dome_act(d)
          w_next := d.U
        }.elsewhen (~w_next_high(d) & w_next_low(d) & w_dome_act(d)) {
          w_next_high(d + 1) := true.B
          w_next := d.U
        }.otherwise {
          w_next_high(d + 1) := w_next_high(d)
        }
      }
    }

    when (w_last_turn & w_last_step) {
      r_next := w_next
    }
  }

  // ******************************
  //             DOME
  // ******************************
  when (w_last_step) {
    r_dome := r_next
  }  

  // ******************************
  //            OUTPUT
  // ******************************
  io.o_slct.dome := r_dome
  io.o_slct.next := r_next
  io.o_slct.step := r_step
}

object StaticSlct extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new StaticSlct(4, 4, 8), args)
}