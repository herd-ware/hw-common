/*
 * File: configs.scala                                                         *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:39:03 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.mem.ram

import chisel3._
import chisel3.util._

import herd.common.mem.mb4s._


object RamCtrlConfigBase extends RamCtrlConfig (
  nPort = 2,
  debug = true, 

  useDome = true,
  nDome = 2,
  multiDome = true,
  nPart = 1,

  isRom = false,
  nByte = "100",
  nDataByte = 4
)

object Mb4sCtrlConfigBase extends Mb4sCtrlConfig (
  pPort = Mb4sConfig5,
  debug = true,   
  isRom = false,
  useReqReg = false
)

object Mb4sRamConfigBase extends Mb4sRamConfig (
  pPort = Array(Mb4sConfig5, Mb4sConfig6),
  debug = true, 

  initFile = "",
  isRom = false,
  nAddrBase = "0000",
  useReqReg = false,
  nByte = "100"
)