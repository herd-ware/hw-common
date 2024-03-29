/*
 * File: instr.scala                                                           *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-04-21 11:01:56 am                                       *
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


object INSTR {
  def URET        = BitPat("b00000000001000000000000001110011")
  def MRET        = BitPat("b00110000001000000000000001110011")
  def WFI         = BitPat("b00010000010100000000000001110011")
}
