/*
 * File: bidirect.scala                                                        *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:21:55 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.bus

import chisel3._
import chisel3.util._


class BiDirectIO[T <: Data](gen: T) extends Bundle {
  val in = Input(gen.cloneType)
  val out = Output(gen.cloneType)
  val eno = Output(gen.cloneType)
}

class BiDirectBus[T <: Data](gen: T) extends Bundle {
  val in = gen.cloneType
  val out = gen.cloneType
  val eno = gen.cloneType
}