/*
 * File: consts.scala
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-28 10:29:58 pm
 * Modified By: Mathieu Escouteloup
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.isa.riscv

import chisel3._
import chisel3.util._


// ******************************
//            REGISTERS
// ******************************
object REG {
  // ------------------------------
  //              GPR
  // ------------------------------
  val X0 = "b00000"
  val X1 = "b00001"
  val X5 = "b00101"
}

// ******************************
//            CSR
// ******************************
object CBIE {
  def ILL   = 0.U(2.W)
  def FLUSH = 1.U(2.W)
  def INV   = 3.U(2.W)
}
