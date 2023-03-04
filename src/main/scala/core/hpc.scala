/*
 * File: hpc.scala
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-03-03 06:52:00 pm
 * Modified By: Mathieu Escouteloup
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
  val br = UInt(64.W)
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
  val cflush = UInt(64.W)
  val efetch = UInt(64.W)
  val ecommit = UInt(64.W)
  val srcdep = UInt(64.W)

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
  val br = Bool()
  val mispred = Bool()
  val rdcycle = Bool()
}

class HpcCacheBus extends Bundle {
  val hit = UInt(4.W)
  val miss = UInt(4.W)
  val pftch = UInt(4.W)
}

class HpcPipelineBus extends Bundle {
  val instret = UInt(64.W)
  val alu = UInt(64.W)
  val ld = UInt(64.W)
  val st = UInt(64.W)
  val br = UInt(64.W)
  val mispred = UInt(64.W)
  val rdcycle = UInt(64.W)
}

class HpcMemoryBus extends Bundle {
  val l1ihit = UInt(64.W)
  val l1ipftch = UInt(64.W)
  val l1imiss = UInt(64.W)
  val l1dhit = UInt(64.W)
  val l1dpftch = UInt(64.W)
  val l1dmiss = UInt(64.W)
  val l2hit = UInt(64.W)
  val l2pftch = UInt(64.W)
  val l2miss = UInt(64.W)
}