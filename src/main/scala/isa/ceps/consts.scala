/*
 * File: consts.scala                                                          *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:26:10 pm                                       *
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
//           CONSTANTS
// ******************************
object CST {
  def MAXCFG  = 32
  def MAXTL   = 2
}

// ******************************
//          INTERRUPTS
// ******************************
object IRQ {
  def L0SI   = 0
  def L1SI   = 1  

  def L0TI   = 4
  def L1TI   = 5
  
  def L0EI   = 8
  def L1EI   = 9
}

// ******************************
//            FIELD
// ******************************
object CONF {
  def STATUS = 0x00
  def ID     = 0x01
  def ENTRY  = 0x02
  def TABLE  = 0x03
  def CAP    = 0x04
  def INST   = 0x70
}

// ******************************
//         CAPABILITIES
// ******************************
object CAP {
  def FEA = 0x00000033
  def SEC = 0x00030000
}

// ******************************
//         INSTANCE WEIGHT
// ******************************
object WEIGHT {
  def MIN   = 0
  def EQU   = 1
  def MAX   = 2
  def FULL  = 3 
}
