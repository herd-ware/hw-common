/*
 * File: csr.scala                                                             *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-03-02 06:39:58 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.isa.hpc

import chisel3._
import chisel3.util._


// ******************************
//            ADDRESS
// ******************************
object CSR {
  def CYCLE     = "hc00"
  def TIME      = "hc01"
  def INSTRET   = "hc02"
  def BR        = "hc03"
  def MISPRED   = "hc04"
  def L1IMISS   = "hc05"
  def L1DMISS   = "hc06"
  def L2MISS    = "hc07"
  
  def CYCLEH    = "hc80"
  def TIMEH     = "hc81"
  def INSTRETH  = "hc82"
  def BRH       = "hc83"
  def MISPREDH  = "hc84"
  def L1IMISSH  = "hc85"
  def L1DMISSH  = "hc86"
  def L2MISSH   = "hc87"
}

// ******************************
//             BUS
// ******************************
class HpcInstrBus extends Bundle {
  val instret = Bool()
  val alu = Bool()
  val ld = Bool()
  val st = Bool()
  val br = Bool()
  val mispred = Bool()
}

class HpcPipelineBus extends Bundle {
  val instret = UInt(64.W)
  val alu = UInt(64.W)
  val ld = UInt(64.W)
  val st = UInt(64.W)
  val br = UInt(64.W)
  val mispred = UInt(64.W)
}

class HpcMemoryBus extends Bundle {
  val l1imiss = UInt(64.W)
  val l1dmiss = UInt(64.W)
  val l2miss = UInt(64.W)
}

class CsrBus extends Bundle {
  val cycle     = UInt(64.W)
  val time      = UInt(64.W)
  val instret   = UInt(64.W)
  val alu       = UInt(64.W)
  val ld        = UInt(64.W)
  val st        = UInt(64.W)
  val br        = UInt(64.W)
  val mispred   = UInt(64.W)
  val l1imiss   = UInt(64.W)
  val l1dmiss   = UInt(64.W)
  val l2miss    = UInt(64.W)
}