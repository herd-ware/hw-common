/*
 * File: params.scala                                                          *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:27:14 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.mem.axi4

import chisel3._
import chisel3.experimental.IO
import chisel3.util._


trait Axi4Params {
  def debug: Boolean
  def nAddrBit: Int
  def nDataByte: Int
  def nDataBit: Int = nDataByte * 8
  def nId: Int
}

case class Axi4Config (
  debug: Boolean,
  nAddrBit: Int,
  nDataByte: Int,
  nId: Int
) extends Axi4Params

