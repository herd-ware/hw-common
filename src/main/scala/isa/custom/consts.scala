/*
 * File: consts.scala                                                          *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:38:38 pm                                       *
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
//           CONSTANTS
// ******************************
object CST {
  def MAXCTIMER = 4
}

// ******************************
//          INTERRUPTS
// ******************************
object IRQ {
  def CTIMER0 = 16
  def CTIMER1 = 17 
  def CTIMER2 = 18
  def CTIMER3 = 19
}

