/*
 * File: io.scala                                                              *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:39:05 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.mem.ram

import chisel3._
import chisel3.util._

import herd.common.mem.mb4s.{SIZE}


// ******************************
//             CTRL
// ******************************
class CtrlReadIO (useDome: Boolean, nDome: Int, nAddrBit: Int, nDataByte: Int) extends Bundle {
  val ready = Output(Bool())
  val valid = Input(Bool())
  val dome = if (useDome) Some(Input(UInt(log2Ceil(nDome).W))) else None
  val mask = Input(UInt(nDataByte.W))
  val addr = Input(UInt(nAddrBit.W))
  val data = Output(UInt((nDataByte * 8).W))
}

class CtrlWriteIO (useDome: Boolean, nDome: Int, nAddrBit: Int, nDataByte: Int) extends Bundle {
  val ready = Output(Bool())
  val valid = Input(Bool())
  val dome = if (useDome) Some(Input(UInt(log2Ceil(nDome).W))) else None
  val mask = Input(UInt(nDataByte.W))
  val addr = Input(UInt(nAddrBit.W))
  val data = Input(UInt((nDataByte * 8).W))
}

// ******************************
//             RAM
// ******************************
class DataRamIO (nDataByte: Int, nAddrBit: Int) extends Bundle {
  val en = Input(Bool())
  val wen = Input(UInt(nDataByte.W))
  val addr = Input(UInt(nAddrBit.W))
  val wdata = Input(UInt((nDataByte * 8).W))
  val rdata = Output(UInt((nDataByte * 8).W))

  def fromRead(read: CtrlReadIO): Unit = {
    val w_tag = read.addr(nAddrBit - 1, log2Ceil(nDataByte))

    en := read.valid
    wen := 0.U
    addr := read.addr(nAddrBit - 1, log2Ceil(nDataByte))
    wdata := 0.U
  }
  
  def fromWrite(write: CtrlWriteIO): Unit = {
    val w_offset = write.addr(log2Ceil(nDataByte) - 1, 0)
    val w_tag = write.addr(nAddrBit - 1, log2Ceil(nDataByte))

    en := write.valid
    wen := (write.mask << w_offset)
    addr := write.addr(nAddrBit - 1, log2Ceil(nDataByte))
    wdata := (write.data << (w_offset << 3.U))
  }
}

class ByteRamIO (nDataByte: Int, nAddrBit: Int) extends Bundle {
  val en = Input(Bool())
  val wen = Input(Bool())
  val mask = Input(UInt(nDataByte.W))
  val addr = Input(UInt(nAddrBit.W))
  val wdata = Input(UInt((nDataByte * 8).W))
  val rdata = Output(UInt((nDataByte * 8).W))

  def fromRead(read: CtrlReadIO): Unit = {
    en := read.valid
    wen := false.B
    mask := read.mask
    addr := read.addr
    wdata := 0.U
  }

  def fromWrite(write: CtrlWriteIO): Unit = {
    en := write.valid
    wen := true.B
    mask := write.mask
    addr := write.addr
    wdata := write.data
  }
}