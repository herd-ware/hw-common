/*
 * File: rsrc.scala                                                            *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:25:02 pm                                       *
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


class Part2Rsrc(nHart: Int, nDome: Int, nPart: Int, nRsrc: Int) extends Module {
  // ******************************
  //       INTERNAL PARAMETERS
  // ******************************
  var nPartRsrc = new Array[Int](nPart)
  var nPartPort = new Array[Int](nPart)

  for (np <- 0 until nPart) {
    if ((nRsrc % nPart) > np) {
      nPartRsrc(np) = (nRsrc / nPart) + 1
    } else {
      nPartRsrc(np) = (nRsrc / nPart)
    }

    require(nPartRsrc(np) > 0, "All parts must have at least one resource.")    
  }
 
  nPartPort(0) = 0
  for (np <- 1 until nPart) {
    nPartPort(np) = nPartPort(np - 1) + nPartRsrc(np - 1)    
  }

  // ******************************
  //             I/Os
  // ******************************
  val io = IO(new Bundle {
    val b_part = new NRsrcIO(nHart, nDome, nPart)
    val b_rsrc = Flipped(new NRsrcIO(nHart, nDome, nRsrc))
  })

  // ******************************
  //          INTERCONNECT
  // ******************************
  val w_rsrc = Wire(Output(new NRsrcIO(nHart, nDome, nRsrc)))

  // ------------------------------
  //        STATE (FROM PART)
  // ------------------------------
  for (np <- 0 until nPart) {
    for (npr <- 0 until nPartRsrc(np)) {
      w_rsrc.state(nPartPort(np) + npr).valid  := io.b_part.state(np).valid
      w_rsrc.state(nPartPort(np) + npr).flush  := io.b_part.state(np).flush
      w_rsrc.state(nPartPort(np) + npr).hart   := io.b_part.state(np).hart
      w_rsrc.state(nPartPort(np) + npr).dome   := io.b_part.state(np).dome
    }
  }

  // ------------------------------
  //        STATE (FROM RSRC)
  // ------------------------------
  for (nr <- 0 until nRsrc) {
    w_rsrc.state(nr).free := io.b_rsrc.state(nr).free
  }

  for (np <- 0 until nPart) {
    val w_free = Wire(Vec(nPartRsrc(np), Bool()))

    for (npr <- 0 until nPartRsrc(np)) {
      w_free(npr) := w_rsrc.state(nPartPort(np) + npr).free
    }

    io.b_part.state(np).free := w_free.asUInt.andR
  }

  // ------------------------------
  //         WEIGHT & PORT
  // ------------------------------
  val w_port = Wire(Vec(nDome, Vec(nRsrc + 1, UInt((log2Ceil(nRsrc) + 1).W))))

  for (d <- 0 until nDome) {
    w_port(d)(0) := 0.U
    for (nr <- 1 to nRsrc) {
      w_port(d)(nr) := Mux((d.U === w_rsrc.state(nr - 1).dome), w_port(d)(nr - 1) + 1.U, w_port(d)(nr - 1))
    }
  }  

  for (nr <- 0 until nRsrc) {
    w_rsrc.state(nr).port := w_port(w_rsrc.state(nr).dome)(nr)
  }
  
  for (d <- 0 until nDome) {
    w_rsrc.weight(d) := w_port(d)(nRsrc)
  }


  // ******************************
  //            OUTPUTS
  // ******************************
  io.b_rsrc.weight := w_rsrc.weight
  for (nr <- 0 until nRsrc) {
    io.b_rsrc.state(nr).fromMaster(w_rsrc.state(nr))
  }
}

object Part2Rsrc extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Part2Rsrc(2, 1, 2, 5), args)
}
