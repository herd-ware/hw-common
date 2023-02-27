/*
 * File: bus.scala                                                             *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:22:21 pm                                       *
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
//             DOME
// ******************************
class DomeIO(nAddrBit: Int, nDataBit: Int) extends Bundle {
  val valid = Input(Bool())
  val id = Input(UInt(nDataBit.W))
  val entry = Input(UInt(nAddrBit.W))
  val exe = Input(Bool())
  val flush = Input(Bool())
  val free = Output(Bool())
  val tl = Input(Vec(2, Bool()))
  val cbo = Input(Bool())
  val mie = Input(Bool())
}

// ******************************
//           RESOURCE
// ******************************
class RsrcIO (nHart: Int, nDome: Int, nRsrc: Int) extends Bundle {
  val valid = Input(Bool())
  val flush = Input(Bool())
  val free = Output(Bool())
  val hart = Input(UInt(log2Ceil(nHart).W))
  val dome = Input(UInt(log2Ceil(nDome).W))
  val port = Input(UInt(log2Ceil(nRsrc).W))

  def fromMaster(rsrc: RsrcIO): Unit = {
    valid := rsrc.valid
    flush := rsrc.flush
    hart  := rsrc.hart 
    dome  := rsrc.dome 
    port  := rsrc.port 
  }
}

class NRsrcIO (nHart: Int, nDome: Int, nRsrc: Int) extends Bundle {
  val weight = Input(Vec(nDome, UInt(log2Ceil(nRsrc + 1).W)))
  val state = Vec(nRsrc, new RsrcIO(nHart, nDome, nRsrc))
}

// ******************************
//           DOME SELECT
// ******************************
class SlctBus(nDome: Int, nPart: Int, nStep: Int) extends Bundle {
  val dome = UInt(log2Ceil(nDome).W)
  val next = UInt(log2Ceil(nDome).W)
  val step = UInt(log2Ceil(nStep).W)
}