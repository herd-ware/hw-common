/*
 * File: configs.scala                                                         *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:28:30 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.mem.mb4s

import chisel3._
import chisel3.util._
import scala.math._


// ******************************
//              BUS
// ******************************
object Mb4sConfig0 extends Mb4sConfig (
  debug = true,  
  readOnly = false,
  nHart = 1,
  nAddrBit = 32,
  useAmo = false,
  nDataByte = 4,

  useDome = false,
  nDome = 1,
  multiDome = false
) 

object Mb4sConfig1 extends Mb4sConfig (
  debug = true,  
  readOnly = false,
  nHart = 1,
  nAddrBit = 32,
  useAmo = false,
  nDataByte = 8,

  useDome = false,
  nDome = 1,
  multiDome = false
)

object Mb4sConfig5 extends Mb4sConfig (
  debug = true,  
  readOnly = false,
  nHart = 2,
  nAddrBit = 32,
  useAmo = false,
  nDataByte = 4,

  useDome = true,
  nDome = 2,
  multiDome = false
)

object Mb4sConfig6 extends Mb4sConfig (
  debug = true,  
  readOnly = true,
  nHart = 2,
  nAddrBit = 32,
  useAmo = false,
  nDataByte = 4,

  useDome = true,
  nDome = 2,
  multiDome = true
)

object Mb4sConfig7 extends Mb4sConfig (
  debug = true,  
  readOnly = false,
  nHart = 2,
  nAddrBit = 64,
  useAmo = false,
  nDataByte = 8,

  useDome = true,
  nDome = 2,
  multiDome = true
)

// ******************************
//            MEMORY
// ******************************
object Mb4sMemConfig0 extends Mb4sMemConfig (
  pPort = Array(Mb4sConfig6),
  nAddrBase = "00",
  nByte = "10"
)

object Mb4sMemConfig1 extends Mb4sMemConfig (
  pPort = Array(Mb4sConfig6),
  nAddrBase = "10",
  nByte = "30"
)

object Mb4sMemConfig2 extends Mb4sMemConfig (
  pPort = Array(Mb4sConfig6),
  nAddrBase = "40",
  nByte = "10"
)

// ******************************
//          INTERCONNECT
// ******************************
object Mb4sCrossbarConfigBase extends Mb4sCrossbarConfig (
  pMaster = Array(Mb4sConfig5, Mb4sConfig5, Mb4sConfig5),
  useMem = false,
  pMem = Array(Mb4sMemConfig0, Mb4sMemConfig1),
  nDefault = 1,
  nBus = 2,
  
  debug = true,  
  multiDome = true,
  nDepth = 2,
  useDirect = false
)

// ******************************
//             AXI4
// ******************************
object Mb4sAxi4ConfigBase extends Mb4sAxi4Config (
  pMb4s = Mb4sConfig5,

  nDataDepth = 4,
  nRespDepth = 4
)