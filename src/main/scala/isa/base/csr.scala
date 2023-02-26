/*
 * File: csr.scala                                                             *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:26:01 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.isa.base

import chisel3._
import chisel3.util._


// ******************************
//            ADDRESS
// ******************************
object CSR {
  def CYCLE     = "hc00"
  def TIME      = "hc01"
  def INSTRET   = "hc02"
  
  def CYCLEH    = "hc80"
  def TIMEH     = "hc81"
  def INSTRETH  = "hc82"
}

// ******************************
//           REGISTERS
// ******************************
class CsrBus extends Bundle {
  val cycle     = UInt(64.W)
  val time      = UInt(64.W)
  val instret   = UInt(64.W)
}