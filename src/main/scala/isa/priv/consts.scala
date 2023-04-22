/*
 * File: consts.scala                                                          *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-04-21 10:55:56 am                                       *
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


object PRIV {
  def U = 0.U(2.W)
  def S = 1.U(2.W)
  def H = 2.U(2.W)
  def M = 3.U(2.W)
}

object EXC {
  def IADDRMIS  = 0
  def IACCFAULT = 1
  def IINSTR    = 2
  def BREAK     = 3
  def LADDRMIS  = 4
  def LACCFAULT = 5
  def SADDRMIS  = 6
  def SACCFAULT = 7
  def UCALL     = 8
  def MCALL     = 11
}

object IRQ {
  def SSI   = 1  
  def MSI   = 3

  def STI   = 5
  def MTI   = 7
  
  def SEI   = 9
  def MEI   = 11
}