/*
 * File: csr.scala                                                             *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-27 05:08:02 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.isa.ceps

import chisel3._
import chisel3.util._


// ******************************
//            ADDRESS
// ******************************
object CSR {
  // ------------------------------
  //             GLOBAL
  // ------------------------------
  def CDC         = "h000"
  def PDC         = "h001"
  
  // ------------------------------
  //        HART INFO: LEVEL 0
  // ------------------------------
  def HL0ID       = "h010"

  // ------------------------------
  //          TRAP: LEVEL 0
  // ------------------------------
  def TL0STATUS   = "h040"
  def TL0TDC      = "h041"
  def TL0EDELEG   = "h042"
  def TL0IDELEG   = "h043"
  def TL0IE       = "h044"
  def TL0TVEC     = "h045"
  
  def TL0SCRATCH  = "h046"
  def TL0EDC      = "h047"
  def TL0EPC      = "h048"
  def TL0CAUSE    = "h049"
  def TL0TVAL     = "h04a"
  def TL0IP       = "h04b"

  // ------------------------------
  //          TRAP: LEVEL 1
  // ------------------------------
  def TL1STATUS   = "h140"
  def TL1TDC      = "h141"
  def TL1EDELEG   = "h142"
  def TL1IDELEG   = "h143"
  def TL1IE       = "h144"
  def TL1TVEC     = "h145"

  def TL1SCRATCH  = "h146"
  def TL1EDC      = "h147"
  def TL1EPC      = "h148"
  def TL1CAUSE    = "h149"
  def TL1TVAL     = "h14a"
  def TL1IP       = "h14b"

  // ------------------------------
  //         CONFIGURATION
  // ------------------------------
  def ENVCFG      = "h300"
  def ENVCFGH     = "h380"
}

// ******************************
//              BUS
// ******************************
class CsrBus(nDataBit: Int) extends Bundle {
  // ------------------------------
  //             GLOBAL
  // ------------------------------
  val cdc         = UInt(nDataBit.W)
  val pdc         = UInt(nDataBit.W)

  // ------------------------------
  //        HART INFO: LEVEL 0
  // ------------------------------
  val hl0id       = UInt(nDataBit.W)

  // ------------------------------
  //          TRAP: LEVEL 0
  // ------------------------------
  val tl0status   = UInt(nDataBit.W)
  val tl0tdc      = UInt(nDataBit.W)
  val tl0edeleg   = UInt(nDataBit.W)
  val tl0ideleg   = UInt(nDataBit.W)
  val tl0ie       = UInt(nDataBit.W)
  val tl0tvec     = UInt(nDataBit.W)

  val tl0scratch  = UInt(nDataBit.W)
  val tl0edc      = UInt(nDataBit.W)
  val tl0epc      = UInt(nDataBit.W)
  val tl0cause    = UInt(nDataBit.W)
  val tl0tval     = UInt(nDataBit.W)
  val tl0ip       = UInt(nDataBit.W)

  // ------------------------------
  //          TRAP: LEVEL 1
  // ------------------------------
  val tl1status   = UInt(nDataBit.W)
  val tl1tdc      = UInt(nDataBit.W)
  val tl1edeleg   = UInt(nDataBit.W)
  val tl1ideleg   = UInt(nDataBit.W)
  val tl1ie       = UInt(nDataBit.W)
  val tl1tvec     = UInt(nDataBit.W)

  val tl1scratch  = UInt(nDataBit.W)
  val tl1edc      = UInt(nDataBit.W)
  val tl1epc      = UInt(nDataBit.W)
  val tl1cause    = UInt(nDataBit.W)
  val tl1tval     = UInt(nDataBit.W)
  val tl1ip       = UInt(nDataBit.W)

  // ------------------------------
  //         CONFIGURATION
  // ------------------------------
  val envcfg      = UInt(64.W)
}