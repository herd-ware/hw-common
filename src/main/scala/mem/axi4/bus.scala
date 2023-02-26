/*
 * File: bus.scala                                                             *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:27:08 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.mem.axi4

import chisel3._
import chisel3.util._


// ******************************
//             WRITE
// ******************************
class Axi4WriteAddrBus (p: Axi4Params) extends Bundle {
  val id = UInt(log2Ceil(p.nId).W)
  val addr = UInt(p.nAddrBit.W)
  val len = UInt(8.W)
  val size = UInt(SIZE.NBIT.W)
  val burst = UInt(BURST.NBIT.W)
  val lock = Bool()
  val cache = UInt(CACHE.NBIT.W)
  val prot = UInt(PROT.NBIT.W)
  val qos = UInt(QOS.NBIT.W)
}

class Axi4WriteAddrIO (p: Axi4Params) extends Bundle {
  val ready = Input(Bool())
  val valid = Output(Bool())
  val ctrl = Output(new Axi4WriteAddrBus(p))
}

class Axi4WriteDataBus (p: Axi4Params) extends Bundle {
  val last = Bool()
  val strb = UInt(p.nDataByte.W)
}

class Axi4WriteDataIO (p: Axi4Params) extends Bundle {
  val ready = Input(Bool())
  val valid = Output(Bool())
  val ctrl = Output(new Axi4WriteDataBus(p))
  val data = Output(UInt((p.nDataByte * 8).W))
}

class Axi4WriteRespBus (p: Axi4Params) extends Bundle {
  val id = UInt(log2Ceil(p.nId).W)
  val resp = UInt(RESP.NBIT.W)
}

class Axi4WriteRespIO (p: Axi4Params) extends Bundle {
  val ready = Input(Bool())
  val valid = Output(Bool())
  val ctrl = Output(new Axi4WriteRespBus(p))
}

class Axi4WriteIO (p: Axi4Params) extends Bundle {
  val aw = new Axi4WriteAddrIO(p)
  val dw = new Axi4WriteDataIO(p)
  val bw = Flipped(new Axi4WriteRespIO(p))
}

// ******************************
//             READ
// ******************************
class Axi4ReadAddrBus (p: Axi4Params) extends Bundle {
  val id = UInt(log2Ceil(p.nId).W)
  val addr = UInt(p.nAddrBit.W)
  val len = UInt(8.W)
  val size = UInt(SIZE.NBIT.W)
  val burst = UInt(BURST.NBIT.W)
  val lock = Bool()
  val cache = UInt(CACHE.NBIT.W)
  val prot = UInt(PROT.NBIT.W)
  val qos = UInt(QOS.NBIT.W)
}

class Axi4ReadAddrIO (p: Axi4Params) extends Bundle {
  val ready = Input(Bool())
  val valid = Output(Bool())
  val ctrl = Output(new Axi4ReadAddrBus(p))
}

class Axi4ReadDataBus (p: Axi4Params) extends Bundle {
  val id = UInt(log2Ceil(p.nId).W)
  val resp = UInt(RESP.NBIT.W)
  val last = Bool()
}

class Axi4ReadDataIO (p: Axi4Params) extends Bundle {
  val ready = Input(Bool())
  val valid = Output(Bool())
  val ctrl = Output(new Axi4ReadDataBus(p))
  val data = Output(UInt((p.nDataByte * 8).W))
}

class Axi4ReadIO (p: Axi4Params) extends Bundle {
  val ar = new Axi4ReadAddrIO(p)
  val dr = Flipped(new Axi4ReadDataIO(p))
}

// ******************************
//             FULL
// ******************************
class Axi4IO (p: Axi4Params) extends Bundle {
  val aw = new Axi4WriteAddrIO(p)
  val dw = new Axi4WriteDataIO(p)
  val bw = Flipped(new Axi4WriteRespIO(p))
  
  val ar = new Axi4ReadAddrIO(p)
  val dr = Flipped(new Axi4ReadDataIO(p))
}

