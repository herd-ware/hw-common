/*
 * File: hpc.scala                                                             *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-04-11 05:42:37 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.core

import chisel3._
import chisel3.util._


// ******************************
//             FULL
// ******************************
class HpcBus extends Bundle {
  val alu = UInt(64.W)
  val bru = UInt(64.W)
  val cycle = UInt(64.W)
  val instret = UInt(64.W)
  val l1ihit = UInt(64.W)
  val l1imiss = UInt(64.W)
  val l1ipftch = UInt(64.W)
  val l1dhit = UInt(64.W)
  val l1dmiss = UInt(64.W)
  val l1dpftch = UInt(64.W)
  val l2hit = UInt(64.W)
  val l2miss = UInt(64.W)
  val l2pftch = UInt(64.W)
  val ld = UInt(64.W)
  val mispred = UInt(64.W)
  val rdcycle = UInt(64.W)
  val st = UInt(64.W)
  val time = UInt(64.W)
  
  val ret = UInt(64.W)
  val call = UInt(64.W)
  val jal = UInt(64.W)
  val jalr = UInt(64.W)
  val srcdep = UInt(64.W)
  val cflush = UInt(64.W)

  val efetch = UInt(64.W)
  val ecommit = UInt(64.W)

  val hfflush = UInt(64.W)
  val hfswitch = UInt(64.W)
}

// ******************************
//            PARTIAL
// ******************************
class HpcInstrBus extends Bundle {
  val instret = Bool()
  val alu = Bool()
  val ld = Bool()
  val st = Bool()
  val bru = Bool()
  val mispred = Bool()
  val rdcycle = Bool()

  val ret = Bool()
  val call = Bool()
  val jal = Bool()
  val jalr = Bool()
  val cflush = Bool()
}

class HpcPipelineBus extends Bundle {
  val instret = Vec(16, Bool())
  val alu = Vec(16, Bool())
  val ld = Vec(16, Bool())
  val st = Vec(16, Bool())
  val bru = Vec(16, Bool())
  val mispred = Vec(16, Bool())
  val rdcycle = Vec(16, Bool())  
  
  val ret = Vec(16, Bool())
  val call = Vec(16, Bool())
  val jal = Vec(16, Bool())
  val jalr = Vec(16, Bool())
  val srcdep = Vec(16, Bool())
  val cflush = Vec(16, Bool())
}

class HpcCacheBus extends Bundle {
  val hit = Vec(16, Bool())
  val miss = Vec(16, Bool())
  val pftch = Vec(16, Bool())
}

class HpcMemoryBus extends Bundle {
  val l1ihit = Vec(16, Bool())
  val l1ipftch = Vec(16, Bool())
  val l1imiss = Vec(16, Bool())
  val l1dhit = Vec(16, Bool())
  val l1dpftch = Vec(16, Bool())
  val l1dmiss = Vec(16, Bool())
  val l2hit = Vec(16, Bool())
  val l2pftch = Vec(16, Bool())
  val l2miss = Vec(16, Bool())
}