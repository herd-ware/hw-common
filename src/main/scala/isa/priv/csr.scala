/*
 * File: csr.scala                                                             *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:38:44 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.isa.priv

import chisel3._
import chisel3.util._


// ******************************
//            ADDRESS
// ******************************
object CSR {
  // ------------------------------
  //         MACHINE INFOS
  // ------------------------------
  def MHARTID   = "hf14"

  // ------------------------------
  //       MACHINE TRAP SETUP
  // ------------------------------
  def MSTATUS   = "h300"
  def MEDELEG   = "h302"
  def MIDELEG   = "h303"
  def MIE       = "h304"
  def MTVEC     = "h305"
  def MSTATUSH  = "h310"

  // ------------------------------
  //      MACHINE TRAP HANDLING
  // ------------------------------
  def MSCRATCH  = "h340"
  def MEPC      = "h341"
  def MCAUSE    = "h342"
  def MTVAL     = "h343"
  def MIP       = "h344"

  // ------------------------------
  //     MACHINE CONFIGURATION
  // ------------------------------
  def MENVCFG   = "h30a"
  def MENVCFGH  = "h31a"
}

// ******************************
//             BUS
// ******************************
class CsrBus(nDataBit: Int) extends Bundle {
  // ------------------------------
  //         MACHINE INFOS
  // ------------------------------
  val mhartid   = UInt(nDataBit.W)

  // ------------------------------
  //       MACHINE TRAP SETUP
  // ------------------------------
  val mstatus   = UInt(64.W)
  val medeleg   = UInt(nDataBit.W)
  val mideleg   = UInt(nDataBit.W)
  val mtvec     = UInt(nDataBit.W)
  val mie       = UInt(nDataBit.W)
  
  // ------------------------------
  //      MACHINE TRAP HANDLING
  // ------------------------------  
  val mscratch  = UInt(nDataBit.W)
  val mepc      = UInt(nDataBit.W)
  val mcause    = UInt(nDataBit.W)
  val mtval     = UInt(nDataBit.W) 
  val mip       = UInt(nDataBit.W)

  // ------------------------------
  //     MACHINE CONFIGURATION
  // ------------------------------
  val menvcfg   = UInt(64.W)
}
