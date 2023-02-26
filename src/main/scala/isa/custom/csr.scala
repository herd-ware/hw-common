/*
 * File: csr.scala                                                             *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:38:40 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.isa.custom

import chisel3._
import chisel3.util._


// ******************************
//            ADDRESS
// ******************************
object CSR {
  // ------------------------------
  //             HART
  // ------------------------------
  def HARTEN   = "h7c0"
}

// ******************************
//             BUS
// ******************************
class CsrBus(nDataBit: Int) extends Bundle {
  // ------------------------------
  //             HART
  // ------------------------------
  val harten = UInt(nDataBit.W)
}