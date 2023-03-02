/*
 * File: io.scala                                                              *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:27:21 pm                                       *
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


// ******************************
//              CBO
// ******************************
class CboIO (nHart: Int, useField: Boolean, nField: Int, nAddrBit: Int) extends Bundle {
  val ready = Input(Bool())
  val valid = Output(Bool())
  val hart = Output(UInt(log2Ceil(nHart).W))
  val field = if (useField) Some(Output(UInt(log2Ceil(nField).W))) else None
  val op = Output(UInt(OP.NBIT.W))
  val sort = Output(UInt(SORT.NBIT.W))
  val block = Output(UInt(BLOCK.NBIT.W))
  val addr = Output(UInt(nAddrBit.W))

  def cln: Bool = (op === OP.CLEAN) | (op === OP.FLUSH)
  def inv: Bool = (op === OP.INVAL) | (op === OP.FLUSH)
  def zero: Bool = (op === OP.ZERO)
  def hint: Bool = (op === OP.PFTCH)

  def any: Bool = (sort === SORT.A)
  def instr: Bool = (sort === SORT.A) | (sort === SORT.E)
  def data: Bool = (sort === SORT.A) | (sort === SORT.R) | (sort === SORT.W)
}

class CboBus (nHart: Int, useField: Boolean, nField: Int, nAddrBit: Int) extends Bundle {
  val valid = Bool()
  val hart = UInt(log2Ceil(nHart).W)
  val field = if (useField) Some(UInt(log2Ceil(nField).W)) else None
  val op = UInt(OP.NBIT.W)
  val sort = UInt(SORT.NBIT.W)
  val block = UInt(BLOCK.NBIT.W)
  val addr = UInt(nAddrBit.W)
  val ready = Bool()
}



