/*
 * File: consts.scala                                                          *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:27:18 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.mem.cbo

import chisel3._
import chisel3.util._


object OP {
  val NBIT  = 4
  def X     = 0.U(NBIT.W)

  def CLEAN = 0.U(NBIT.W)
  def INVAL = 1.U(NBIT.W)
  def FLUSH = 2.U(NBIT.W)
  def ZERO  = 3.U(NBIT.W)
  def PFTCH = 4.U(NBIT.W)
}

object SORT {
  val NBIT  = 2

  def A = 0.U(NBIT.W)
  def E = 1.U(NBIT.W)
  def R = 2.U(NBIT.W)
  def W = 3.U(NBIT.W)
}

object BLOCK {
  val NBIT  = 2

  def X     = 0.U(NBIT.W)
  def LINE  = 1.U(NBIT.W)
  def SET   = 2.U(NBIT.W)
  def FULL  = 3.U(NBIT.W)
}
