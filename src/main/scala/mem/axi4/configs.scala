/*
 * File: configs.scala                                                         *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:27:10 pm                                       *
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


object Axi4ConfigBase extends Axi4Config (
  debug = true,
  nAddrBit = 32,
  nDataByte = 4,
  nId = 1
)

