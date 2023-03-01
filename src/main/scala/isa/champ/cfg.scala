/*
 * File: cfg.scala
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-03-01 09:44:14 am
 * Modified By: Mathieu Escouteloup
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.isa.champ

import chisel3._
import chisel3.util._


// ******************************
//            STATUS
// ******************************
class Status (nDataBit: Int, useOrder: Boolean) extends Bundle {
  val valid   = Bool()
  val lock    = Bool()
  val update  = Bool()
  val size    = Bool()
  val atl     = Vec(2, Bool())
  val order   = UInt(5.W)

  def toUInt: UInt = {
    return Cat(order, 0.U(17.W), atl.asUInt, 0.U(4.W), size, update, lock, valid) 
  }
  def fromUInt (in: UInt) = {
    valid   := in(0)
    lock    := in(1)
    update  := in(2)
    size    := in(3)

    when (~in(8)) {
      atl(0) := in(8)
    }
    when (~in(9)) {
      atl(1) := in(9)
    }

    if (useOrder) {
      if (nDataBit == 64) {
        order := Cat(0.U(1.W), in(30, 27))
      } else {
        order := Cat(0.U(2.W), in(29, 27))
      }      
    } else {
      order := 0.U
    }
  }
  def fromUIntData (in: UInt) = {
    size  := in(3)

    when (~in(8)) {
      atl(0) := in(8)
    }
    when (~in(9)) {
      atl(1) := in(9)
    }
    
    if (useOrder) {
      if (nDataBit == 64) {
        order := Cat(0.U(1.W), in(30, 27))
      } else {
        order := Cat(0.U(2.W), in(29, 27))
      }      
    } else {
      order := 0.U
    }
  }
}

// ******************************
//          CAPABILITIES
// ******************************
class Capabilities (nDataBit: Int, nTrapLvl: Int, useFr: Boolean) extends Bundle {
  val featl     = Vec(CST.MAXTL, Bool())
  val feafr     = Bool()
  val feacbo    = Bool()
  val secmie    = Bool()
  val seccst    = Bool()

  def toUInt: UInt = {
    if (nDataBit == 64) {
      return Cat(0.U(46.W), seccst, secmie, 0.U(10.W), feacbo, feafr, 0.U(2.W), featl.asUInt)
    } else {
      return Cat(0.U(14.W), seccst, secmie, 0.U(10.W), feacbo, feafr, 0.U(2.W), featl.asUInt)
    }
  }
  def fromUInt (in: UInt) = {
    for (tl <- 0 until CST.MAXTL) {
      if (tl < nTrapLvl) featl(tl) := in(tl) else featl(tl) := false.B
    }
    feacbo    := in(5)
    secmie    := in(16)
    seccst    := in(17)
  }
}

// ******************************
//           INSTANCE
// ******************************
class Instance(nDataBit: Int, useFr: Boolean) extends Bundle {
  val muldiv  = Bool()
  val weight  = UInt(3.W)
  val fr      = Bool()

  def toUInt: UInt = {
    if (nDataBit == 64) {
      return Cat(0.U(59.W), fr, muldiv, weight)
    } else {
      return Cat(0.U(27.W), fr, muldiv, weight)
    }
  }
  def fromUInt (in: UInt) = {
    weight  := in(2, 0)
    muldiv  := in(3)
    if (useFr) fr := in(4) else fr := 0.U
  }
}

// ******************************
//          IDENTIFIER
// ******************************
class Identifier(nDataBit: Int, useRange: Boolean) extends Bundle {
  val part = MixedVec(
    for (pa <- 1 to nRange) yield {
      if ((pa * log2Ceil(nDataBit)) < nDataBit) {
        UInt(log2Ceil(nDataBit).W)
      } else {
        UInt((nDataBit % log2Ceil(nDataBit)).W)
      }
    }
  )

  def nRange: Int = {
    if ((nDataBit % log2Ceil(nDataBit)) != 0) {
      return (nDataBit / log2Ceil(nDataBit)) + 1
    } else {
      return (nDataBit / log2Ceil(nDataBit))
    }
  }
  def toUInt: UInt = {
    if (useRange) {
      part.asUInt
    } else {
      part(0)
    }    
  }
  def toPart(slct: UInt) = {
    val w_part = Wire(UInt(log2Ceil(nDataBit).W))
    w_part := part(0)

    for (pa <- 1 until nRange) {
      when (pa.U === slct) {
        w_part := part(pa)
      }
    }  

    w_part
  }
  def fromUInt (in: UInt) = {
    if (useRange) {
      for (pa <- 0 until nRange) yield {
        if (((pa + 1) * log2Ceil(nDataBit) - 1) < nDataBit) {
          part(pa) := in(((pa + 1) * log2Ceil(nDataBit) - 1), pa * log2Ceil(nDataBit))
        } else {
          part(pa) := in(nDataBit - 1, pa * log2Ceil(nDataBit))
        }
      }
    } else {
      part(0) := in
      for (pa <- 1 until nRange) yield {
        part(pa) := 0.U
      }
    }    
  }
}

// ******************************
//          CONFIGURATION
// ******************************
class DomeCfgBus(p: DomeCfgParams) extends Bundle {
  val status  = new Status(p.nDataBit, p.useRange)
  val id      = new Identifier(p.nDataBit, p.useRange)
  val entry   = UInt(p.nDataBit.W)
  val table   = UInt(p.nDataBit.W)
  val cap     = new Capabilities(p.nDataBit, p.nTrapLvl, p.useFr)
  val inst    = new Instance(p.nDataBit, p.useFr)

  def toVec: Vec[UInt] = {
    val w_vec = Wire(Vec(6, UInt(p.nDataBit.W)))

    w_vec(0) := status.toUInt
    w_vec(1) := id.toUInt
    w_vec(2) := entry
    w_vec(3) := table
    w_vec(4) := cap.toUInt
    w_vec(5) := inst.toUInt

    w_vec
  }
  def toUInt: UInt = toVec.asUInt
}

// ******************************
//          REGISTER FILE
// ******************************
class RegFileBus(nChampReg: Int, p: DomeCfgParams) extends Bundle {
  val chf = UInt(log2Ceil(nChampReg).W)
  val phf = UInt(log2Ceil(nChampReg).W)
  val frv = Bool()
  val frhf = UInt(log2Ceil(nChampReg).W)
  val hf = Vec(nChampReg, new DomeCfgBus(p))
}

class RegFilePtrBus(nChampReg: Int, p: DomeCfgParams) extends Bundle {
  val valid = Bool()
  val addr = UInt(log2Ceil(nChampReg).W)
  val hf = new DomeCfgBus(p)
} 

class RegFileStateBus(nChampReg: Int, p: DomeCfgParams) extends Bundle {
  val cur = new RegFilePtrBus(nChampReg, p)
  val prev = new RegFilePtrBus(nChampReg, p)
  val fr = new RegFilePtrBus(nChampReg, p)
}