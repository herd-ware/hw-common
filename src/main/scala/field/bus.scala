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


package herd.common.field

import chisel3._
import chisel3.util._


// ******************************
//            FIELD
// ******************************
class FieldIO(nAddrBit: Int, nDataBit: Int) extends Bundle {
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
class RsrcIO (nHart: Int, nField: Int, nRsrc: Int) extends Bundle {
  val valid = Input(Bool())
  val flush = Input(Bool())
  val free = Output(Bool())
  val hart = Input(UInt(log2Ceil(nHart).W))
  val field = Input(UInt(log2Ceil(nField).W))
  val port = Input(UInt(log2Ceil(nRsrc).W))

  def fromMaster(rsrc: RsrcIO): Unit = {
    valid := rsrc.valid
    flush := rsrc.flush
    hart  := rsrc.hart 
    field  := rsrc.field 
    port  := rsrc.port 
  }
}

class NRsrcIO (nHart: Int, nField: Int, nRsrc: Int) extends Bundle {
  val weight = Input(Vec(nField, UInt(log2Ceil(nRsrc + 1).W)))
  val state = Vec(nRsrc, new RsrcIO(nHart, nField, nRsrc))
}

// ******************************
//          FIELD SELECT
// ******************************
class SlctBus(nField: Int, nPart: Int, nStep: Int) extends Bundle {
  val field = UInt(log2Ceil(nField).W)
  val next = UInt(log2Ceil(nField).W)
  val step = UInt(log2Ceil(nStep).W)
}