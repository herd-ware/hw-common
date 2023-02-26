/*
 * File: params.scala                                                          *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:39:07 pm                                       *
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
import scala.math._

import herd.common.gen._
import herd.common.dome._
import herd.common.mem.mb4s.{Mb4sParams, Mb4sMemParams, Mb4sConfig}


trait RamCtrlParams extends DomeParams {
  def nPort: Int
  def debug: Boolean

  def useDome: Boolean
  def nDome: Int
  def multiDome: Boolean
  def nPart: Int

  def isRom: Boolean
  def nByte: String
  def nDataByte: Int
  def nData: Int = (BigInt(nByte, 16) / nDataByte).toInt
  def nAddrBit: Int = log2Ceil(BigInt(nByte, 16))
}

case class RamCtrlConfig (
  nPort: Int,
  debug: Boolean,

  useDome: Boolean,
  nDome: Int,
  multiDome: Boolean,
  nPart: Int,

  isRom: Boolean,
  nByte: String,
  nDataByte: Int
) extends RamCtrlParams

trait Mb4sCtrlParams extends GenParams {
  def pPort: Mb4sParams
  def debug: Boolean

  def isRom: Boolean
  def readOnly: Boolean = pPort.readOnly
  def nAddrBit: Int = pPort.nAddrBit
  def nDataByte: Int = pPort.nDataByte

  def useDome: Boolean = pPort.useDome
  def nDome: Int = pPort.nDome
  def multiDome: Boolean = pPort.multiDome
  def nPart: Int = pPort.nPart

  def useReqReg: Boolean
}

case class Mb4sCtrlConfig (
  pPort: Mb4sParams,  
  debug: Boolean,
  isRom: Boolean,
  useReqReg: Boolean
) extends Mb4sCtrlParams

trait Mb4sRamParams extends RamCtrlParams with Mb4sMemParams {
  def pPort: Array[Mb4sParams]
  def nPort: Int = pPort.size
  def debug: Boolean

  def useDome: Boolean = {
    var use: Boolean = pPort(0).useDome    
    for (po <- pPort) {
      require((po.useDome == use), "All the ports must use domes to allow its support.")
    }
    return use
  }
  def nDome: Int = {
    var ndome: Int = pPort(0).nDome
    for (po <- pPort) {
      if (po.nDome > ndome) {
        ndome = po.nDome
      }
    }
    return ndome
  }
  def multiDome: Boolean = {
    var multi: Boolean = pPort(0).multiDome
    for (po <- pPort) {
      if (po.multiDome) {
        multi = true
      }
    }
    return multi
  }
  def nPart: Int = {
    var npart: Int = pPort(0).nPart
    for (po <- pPort) {
      if (po.nPart > npart) {
        npart = po.nPart
      }
    }
    return npart
  }

  def initFile: String
  def isRom: Boolean
  def nAddrBase: String
  def useReqReg: Boolean
  def nByte: String
  def nDataByte: Int = {
    var nbyte: Int = 0
    for (po <- pPort) {
      nbyte = max(nbyte, po.nDataByte)
    }
    return nbyte
  }
  def nDataBit: Int = nDataByte * 8

  def pCtrl: Array[Mb4sCtrlParams] = {    
    var p = new Array[Mb4sCtrlParams](nPort)
    for (port <- 0 until nPort) {
      p(port) = new Mb4sCtrlConfig (
        pPort = pPort(port),
        debug = debug,
        isRom = isRom,
        useReqReg = useReqReg
      )
    }
    return p
  }
}

case class Mb4sRamConfig (
  pPort: Array[Mb4sParams],
  debug: Boolean,

  initFile: String,
  isRom: Boolean,
  nAddrBase: String,
  useReqReg: Boolean,
  nByte: String
) extends Mb4sRamParams