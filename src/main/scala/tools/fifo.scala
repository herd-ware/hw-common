/*
 * File: fifo.scala                                                            *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:27:04 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description: FIFO with optional registers:                                  
 * nSeqLvl = 0: cross read, combinatorial free, combinatorial individual full  
 * nSeqLvl = 1: sync. read, combinatorial free, combinatorial individual full   
 * nSeqLvl = 2: sync. read, synchronous free, combinatorial individual full      
 * nSeqLvl = 3: sync. read, synchronous free, synchronous individual full        
 * nSeqLvl = 4: sync. read, synchronous free, synchronous global full                                                                 *
 */
 

package herd.common.tools

import chisel3._
import chisel3.util._


class Fifo[T <: Data](nSeqLvl: Int, gen: T, depth: Int, nWritePort: Int, nReadPort: Int) extends Module {
  val io = IO(new Bundle {
    val i_flush = Input(Bool())

    val b_write = Vec(nWritePort, Flipped(Decoupled(gen)))

    val o_pt = Output(UInt((log2Ceil(depth + 1)).W))
    val o_reg = Output(Vec(depth, Valid(gen)))
    
    val b_read = Vec(nReadPort, Decoupled(gen))
  })

  val r_pt = RegInit(0.U((log2Ceil(depth + 1)).W))
  val r_fifo = Reg(Vec(depth, gen))

  // ******************************
  //              READ
  // ******************************
  val w_read_pt = Wire(Vec(nReadPort + 1, UInt((log2Ceil(depth + 1)).W)))
  val w_read_in_pt = Wire(Vec(nReadPort + 1, UInt((log2Ceil(nWritePort + 1)).W)))

  w_read_pt(0) := 0.U
  w_read_in_pt(0) := nWritePort.U
  if (nSeqLvl <= 0) {
    for (w <- 0 until nWritePort) {
      when (io.b_write(nWritePort - 1 - w).valid) {
        w_read_in_pt(0) := (nWritePort - 1 - w).U
      }
    }
  }

  // ------------------------------
  //       SUPPORT CROSS READ
  // ------------------------------
  if (nSeqLvl <= 0) {
    for (r <- 0 until nReadPort) {
      io.b_read(r).valid := (w_read_pt(r) < r_pt) | (w_read_in_pt(r) < nWritePort.U)
      io.b_read(r).bits := r_fifo(0)
      w_read_pt(r + 1) := w_read_pt(r)
      w_read_in_pt(r + 1) := w_read_in_pt(r)

      for (d <- 0 until depth) {
        when(w_read_pt(r) < r_pt & d.U === w_read_pt(r)) {
          io.b_read(r).bits := r_fifo(d)
        }
      }

      for (w <- 0 until nWritePort) {
        when((w_read_pt(r) >= r_pt) & (w_read_in_pt(r) < nWritePort.U) & w.U === w_read_in_pt(r)) {
          io.b_read(r).bits := io.b_write(w).bits
        }
      }

      when(io.b_read(r).ready & (w_read_pt(r) < r_pt)) {
        w_read_pt(r + 1) := w_read_pt(r) + 1.U
      }

      when(io.b_read(r).ready & (w_read_pt(r) >= r_pt) & (w_read_in_pt(r) < nWritePort.U)) {
        w_read_in_pt(r + 1) := nWritePort.U
        for (w <- 0 until nWritePort) {
          when (io.b_write(nWritePort - 1 - w).valid & ((nWritePort - 1 - w).U > w_read_in_pt(r))) {
            w_read_in_pt(r + 1) := (nWritePort - 1 - w).U
          }
        }
      }
    }

  // ------------------------------
  //         NO CROSS READ
  // ------------------------------
  } else {
    for (r <- 0 until nReadPort) {
      io.b_read(r).valid := (w_read_pt(r) < r_pt)
      io.b_read(r).bits := r_fifo(0)
      w_read_pt(r + 1) := w_read_pt(r)
      w_read_in_pt(r + 1) := w_read_in_pt(r)

      for (d <- 0 until depth) {
        when(d.U === w_read_pt(r)) {
          io.b_read(r).bits := r_fifo(d)
        }
      }

      when(io.b_read(r).ready & (w_read_pt(r) < r_pt)) {
        w_read_pt(r + 1) := w_read_pt(r) + 1.U
      }
    }
  }

  // ******************************
  //             SHIFT
  // ******************************
  for (r <- 0 to nReadPort) {
    when (r.U === w_read_pt(nReadPort)) {
      for (s <- 0 until depth - r) {
        r_fifo(s) := r_fifo(s + r)
      }
    }
  }

  // ******************************
  //             WRITE
  // ******************************
  val w_write_pt = Wire(Vec(nWritePort + 1, UInt((log2Ceil(depth + 1)).W)))
  val w_full_pt = Wire(Vec(nWritePort + 1, UInt((log2Ceil(depth + 1)).W)))

  w_write_pt(0) := r_pt - w_read_pt(nReadPort)
  if (nSeqLvl <= 1) {
    w_full_pt(0) := r_pt - w_read_pt(nReadPort)
  } else if ((nSeqLvl == 2) || (nSeqLvl == 3)) {
    w_full_pt(0) := r_pt
  } else {
    when((depth.U - r_pt) >= nWritePort.U) {
      w_full_pt(0) := r_pt
    }.otherwise {
      w_full_pt(0) := depth.U
    }
  }

  for (w <- 0 until nWritePort) {
    w_write_pt(w + 1) := w_write_pt(w)

    if (nSeqLvl <= 0) {
      io.b_write(w).ready := (w_full_pt(w) < depth.U) | (w_read_in_pt(nReadPort) > w.U)

      w_full_pt(w + 1) := w_full_pt(w)

      when(io.b_write(w).valid & (w_full_pt(w) < depth.U) & (w_read_in_pt(nReadPort) <= w.U)) {
        for (d <- 0 until depth) {
          when(d.U === w_write_pt(w)) {
            r_fifo(d) := io.b_write(w).bits
          }
        }
        w_write_pt(w + 1) := w_write_pt(w) + 1.U
        w_full_pt(w + 1) := w_full_pt(w) + 1.U
      }
    } else {
      io.b_write(w).ready := (w_full_pt(w) < depth.U)

      if (nSeqLvl == 3) {
        w_full_pt(w + 1) := w_full_pt(w) + 1.U
      } else if (nSeqLvl >= 4) {
        w_full_pt(w + 1) := w_full_pt(0)
      } else {
        w_full_pt(w + 1) := w_full_pt(w)
      }

      when(io.b_write(w).valid & (w_full_pt(w) < depth.U)) {
        for (d <- 0 until depth) {
          when(d.U === w_write_pt(w)) {
            r_fifo(d) := io.b_write(w).bits
          }
        }
        w_write_pt(w + 1) := w_write_pt(w) + 1.U
        if (nSeqLvl <= 2) w_full_pt(w + 1) := w_full_pt(w) + 1.U
      }
    }
  }

  // ******************************
  //           REGISTER
  // ******************************
  when(io.i_flush) {
    r_pt := 0.U
  }.otherwise {
    r_pt := w_write_pt(nWritePort)
  }

  // ******************************
  //        EXTERNAL ACCESS
  // ******************************
  io.o_pt := r_pt
  for (d <- 0 until depth) {
    io.o_reg(d).valid := (d.U < r_pt)
    io.o_reg(d).bits := r_fifo(d)
  }
}

object Fifo extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Fifo(2, UInt(8.W), 4, 2, 2), args)
}
