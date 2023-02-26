/*
 * File: consts.scala                                                          *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:27:12 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.mem.axi4

import chisel3._
import chisel3.util._

import herd.common.mem.mb4s.{SIZE => MB4SSIZE}


object SIZE {
  def NBIT  = 3

  def B1    = 0
  def B2    = 1
  def B4    = 2
  def B8    = 3
  def B16   = 4

  def fromMb4s(mb4s: UInt): UInt = {
    val w_size = Wire(UInt(SIZE.NBIT.W))
    
    w_size := B1.U
    switch (mb4s) {
      is (MB4SSIZE.B1.U)  {w_size := B1.U}
      is (MB4SSIZE.B2.U)  {w_size := B2.U}
      is (MB4SSIZE.B4.U)  {w_size := B4.U}
      is (MB4SSIZE.B8.U)  {w_size := B8.U}
      is (MB4SSIZE.B16.U) {w_size := B16.U}
    }

    return w_size
  }
}

object BURST {
  def NBIT  = 2
  def NONE  = 0.U(NBIT.W)

  def FIXED = 0.U(NBIT.W)
  def INC   = 1.U(NBIT.W)
  def WRAP  = 2.U(NBIT.W)
}

object CACHE {
  def NBIT  = 4
  def NONE  = 0.U(NBIT.W)
}

object PROT {
  def NBIT  = 3
  def NONE  = 0.U(NBIT.W)

  def PRIV  = 1.U(NBIT.W)
  def UNSEC = 2.U(NBIT.W)
  def INSTR = 4.U(NBIT.W)
}

object QOS {
  def NBIT  = 4
  def NONE  = 0.U(NBIT.W)
}

object RESP {
  def NBIT  = 2
  def NONE  = 0.U(NBIT.W)
}
