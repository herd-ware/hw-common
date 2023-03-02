/*
 * File: bus.scala                                                             *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:28:27 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.mem.mb4s

import chisel3._
import chisel3.util._


// ******************************
//             REQ
// ******************************
class Mb4sReqBus(p: Mb4sReqParams) extends Bundle {
  val hart = UInt(log2Ceil(p.nHart).W)
  val op = UInt(p.nOpBit.W)
  val amo = if (p.useAmo) Some(UInt(AMO.NBIT.W)) else None
  val size = UInt(SIZE.NBIT.W)
  val addr = UInt(p.nAddrBit.W)

  def ro: Bool = {
    if (p.useAmo) {
      return (op === OP.R) | (op === OP.LR)
    } else {
      return (op === 0.B)
    }
  }
  def wo: Bool = {
    if (p.useAmo) {
      return (op === OP.W) | (op === OP.SC)
    } else {
      return (op === 1.B)
    }
  }
  def a: Bool = {
    if (p.useAmo) {
      return (op === OP.AMO)
    } else {
      return 0.B
    }
  }
  def ra: Bool = this.ro | this.a
  def wa: Bool = this.wo | this.a
}

class Mb4sReqIO(p: Mb4sReqParams) extends Bundle {
  val ready = Input(Vec(p.nReadyBit, Bool()))
  val valid = Output(Bool())
  val field = if (p.useField) Some(Output(UInt(log2Ceil(p.nField).W))) else None
  val ctrl = Output(new Mb4sReqBus(p))
}

// ******************************
//             ACK
// ******************************
class Mb4sDataIO(p: Mb4sDataParams) extends Bundle {
  val ready = Input(Vec(p.nReadyBit, Bool()))
  val valid = Output(Bool())
  val field = if (p.useField) Some(Output(UInt(log2Ceil(p.nField).W))) else None
  val data = Output(UInt((p.nDataByte * 8).W))
}

class Mb4sAckIO(p: Mb4sDataParams) extends Bundle {
  val write = new Mb4sDataIO(p)
  val read = Flipped(new Mb4sDataIO(p))
}

// ******************************
//             FULL
// ******************************
class Mb4sIO(p: Mb4sParams) extends Bundle {
  val req = new Mb4sReqIO(p)
  val write = new Mb4sDataIO(p)
  val read = Flipped(new Mb4sDataIO(p))
}

// ******************************
//            MODULES
// ******************************
class Mb4sNodeBus(nInst: Int) extends Bundle {
  val op = UInt(NODE.NBIT.W)
  val zero = Bool()
  val node = UInt(nInst.W)

  def r: Bool = (op === NODE.R) | (op === NODE.AMO)
  def w: Bool = (op === NODE.W) | (op === NODE.AMO) 
  def a: Bool = (op === NODE.AMO) 
}