/*
 * File: conv.scala                                                            *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:26:57 pm                                       *
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


class ByteToBit(nInByte : Int, nOutByte : Int) extends Module {
  val io = IO(new Bundle {
    val i_in = Input(Vec(nInByte, UInt(8.W)))
    val o_out = Output(UInt((nOutByte * 8).W))
  })

  val w_concat = Wire(Vec(nOutByte, UInt((nOutByte * 8).W)))

  w_concat(0) := io.i_in(0)
  if (nOutByte > 1 && nInByte >= nOutByte) {
    for (i <- 1 until nOutByte) {
      w_concat(i) := Cat(io.i_in(i), w_concat(i - 1)(i*8 - 1,0))
    }
  } else if (nOutByte > 1) {
    for (i <- 1 until nOutByte) {
      if (i < nInByte) {
        w_concat(i) := Cat(io.i_in(i), w_concat(i - 1)(i*8 - 1,0))
      }else {
        w_concat(i) := Cat(0.U(8.W), w_concat(i - 1)(i*8 - 1,0))
      }
    }
  } else {
    w_concat(nOutByte - 1) := io.i_in(0)
  }

  io.o_out := w_concat(nOutByte - 1)
}


class BitToByte(nInByte : Int, nOutByte : Int) extends Module {
  val io = IO(new Bundle {
    val i_in = Input(UInt((nInByte * 8).W))
    val o_out = Output(Vec(nOutByte, UInt(8.W)))
  })

  if (nInByte > nOutByte) {
    for (i <- 0 until nOutByte) {
      io.o_out(i) := io.i_in(i*8 + 7, i*8)
    }
  } else {
    for (i <- 0 until nOutByte) {
      if (i < nInByte) {
        io.o_out(i) := io.i_in(i*8 + 7, i*8)
      } else {
        io.o_out(i) := 0.U
      }
    }
  }
}

class ResizeVec(nIn : Int, nOut : Int) extends Module {
  val io = IO(new Bundle {
    val i_in = Input(Vec(nIn, UInt(8.W)))
    val o_out = Output(Vec(nOut, UInt(8.W)))
  })

  if (nIn > nOut) {
    for (i <- 0 until nOut) {
      io.o_out(i) := io.i_in(i)
    }
  } else if (nIn == nOut) {
    io.o_out := io.i_in
  } else {
    for (i <- 0 until nOut) {
      if (i < nIn) {
        io.o_out(i) := io.i_in(i)
      } else {
        io.o_out(i) := 0.U
      }
    }
  }
}
