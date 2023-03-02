/*
 * File: params.scala                                                          *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:28:42 pm                                       *
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

import herd.common.gen._
import herd.common.mem.axi4.{Axi4Params, Axi4Config}


// ******************************
//              BASE
// ******************************
trait Mb4sBaseParams extends GenParams {
  def debug: Boolean

  def useField: Boolean
  def nField: Int
  def multiField: Boolean
  def nPart: Int = 1

  def nReadyBit: Int = nFieldSlct
}

case class Mb4sBaseConfig (
  debug: Boolean,
  
  useField: Boolean,
  nField: Int,
  multiField: Boolean
) extends Mb4sBaseParams

// ******************************
//              REQ
// ******************************
trait Mb4sReqParams extends Mb4sBaseParams {
  def debug: Boolean  
  def readOnly: Boolean
  def nHart: Int
  def nAddrBit: Int
  def useAmo: Boolean
  def nOpBit: Int = {
    if (useAmo) {
      return OP.NBIT
    } else {
      return 1 
    }
  }

  def useField: Boolean
  def nField: Int
  def multiField: Boolean
}

case class Mb4sReqConfig (
  debug: Boolean,  
  readOnly: Boolean,
  nHart: Int,
  nAddrBit: Int,
  useAmo: Boolean,
  
  useField: Boolean,
  nField: Int,
  multiField: Boolean
) extends Mb4sReqParams

// ******************************
//             DATA
// ******************************
trait Mb4sDataParams extends Mb4sBaseParams {
  def debug: Boolean  
  def readOnly: Boolean
  def nDataByte: Int
  def nDataBit: Int = nDataByte * 8
  
  def useField: Boolean
  def nField: Int
  def multiField: Boolean
}

case class Mb4sDataConfig (
  debug: Boolean,  
  readOnly: Boolean,
  nDataByte: Int,

  useField: Boolean,
  nField: Int,
  multiField: Boolean
) extends Mb4sDataParams

// ******************************
//              BUS
// ******************************
trait Mb4sParams extends Mb4sReqParams with Mb4sDataParams {
  def debug: Boolean  
  def readOnly: Boolean
  def nHart: Int
  def nAddrBit: Int
  def useAmo: Boolean
  def nDataByte: Int
  
  def useField: Boolean
  def nField: Int
  def multiField: Boolean
}

case class Mb4sConfig (
  debug: Boolean,  
  readOnly: Boolean,
  nHart: Int,
  nAddrBit: Int,
  useAmo: Boolean,
  nDataByte: Int,

  useField: Boolean,
  nField: Int,
  multiField: Boolean
) extends Mb4sParams

// ******************************
//              BUS
// ******************************

// ******************************
//            MEMORY
// ******************************
trait Mb4sMemParams {
  def pPort: Array[Mb4sParams]

  def nAddrBase: String
  def nByte: String
}

case class Mb4sMemConfig (
  pPort: Array[Mb4sParams],
  
  nAddrBase: String,
  nByte: String
) extends Mb4sMemParams

// ******************************
//          INTERCONNECT
// ******************************
trait Mb4sCrossbarParams extends GenParams {
  def pMaster: Array[Mb4sParams]

  def nMaster: Int = pMaster.size
  def useMem: Boolean
  def pMem: Array[Mb4sMemParams]
  def nMem: Int = pMem.size
  def nDefault: Int 
  def nBus: Int
  def useDirect: Boolean
  def nSlave: Int = {
    if (useMem) {
      return nMem + nDefault
    } else {
      return nBus
    }
  }
  def pSlave: Mb4sParams = MB4S.node(pMaster, multiField)
  
  def debug: Boolean  
  def readOnly: Boolean = pSlave.readOnly
  def nHart: Int = pSlave.nHart
  def nAddrBit: Int = pSlave.nAddrBit
  def useAmo: Boolean = pSlave.useAmo
  def nOpBit: Int = pSlave.nOpBit
  def nDataByte: Int = pSlave.nDataByte
  def nDataBit: Int = nDataByte * 8
  
  def useField: Boolean = pSlave.useField
  def nField: Int = pSlave.nField
  def multiField: Boolean
  override def useFieldTag: Boolean = useField && !multiField && pSlave.useFieldTag
  override def useFieldSlct: Boolean = (useField && multiField) || pSlave.useFieldSlct
  override def nFieldSlct: Int = {    
    if (useFieldSlct) {
      return nField
    } else {
      return 1
    }
  }
  def nPart: Int = 1

  def nDepth: Int
}

case class Mb4sCrossbarConfig (
  pMaster: Array[Mb4sParams],
  useMem: Boolean,
  pMem: Array[Mb4sMemParams],
  nDefault: Int,
  nBus: Int,
  
  debug: Boolean,  
  multiField: Boolean,
  nDepth: Int,
  useDirect: Boolean,
) extends Mb4sCrossbarParams

// ******************************
//             AXI4
// ******************************
trait Mb4sAxi4Params extends GenParams {
  def pMb4s: Mb4sParams
  def pAxi4: Axi4Params = new Axi4Config (
    debug = pMb4s.debug,
    nAddrBit = pMb4s.nAddrBit,
    nDataByte = pMb4s.nDataByte,
    nId = 1
  )
  
  def debug: Boolean = pMb4s.debug
  def readOnly: Boolean = pMb4s.readOnly
  def nHart: Int = pMb4s.nHart
  def nAddrBit: Int = pMb4s.nAddrBit
  def nDataByte: Int = pMb4s.nDataByte
  def nDataBit: Int = nDataByte * 8
  
  def useField: Boolean = pMb4s.useField
  def nField: Int = pMb4s.nField
  def multiField: Boolean = pMb4s.multiField
  def nPart: Int = 1

  def nDataDepth: Int
  def nRespDepth: Int
}

case class Mb4sAxi4Config (
  pMb4s: Mb4sParams,

  nDataDepth: Int,
  nRespDepth: Int
) extends Mb4sAxi4Params
