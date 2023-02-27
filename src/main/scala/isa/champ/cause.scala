/*
 * File: cause.scala                                                           *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-27 05:13:58 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.isa.champ

import chisel3._
import chisel3.util._


object EXC {
  def IADDRMIS    = 0
  def IACCFAULT   = 1
  def IINSTR      = 2
  def BREAK       = 3
  def LADDRMIS    = 4
  def LACCFAULT   = 5
  def SADDRMIS    = 6
  def SACCFAULT   = 7
  def DLADDRMIS   = 8
  def DSADDRMIS   = 9
  def DSWITCH     = 256
  def DRET        = 257
}