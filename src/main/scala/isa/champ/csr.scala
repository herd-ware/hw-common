/*
 * File: csr.scala                                                             *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-04-21 10:26:08 am                                       *
 * Modified By: Mathieu Escouteloup                                            *
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
//            ADDRESS
// ******************************
object CSR {
  // ------------------------------
  //             GLOBAL
  // ------------------------------
  def CHF         = "h000"
  def PHF         = "h001"
  
  // ------------------------------
  //        HART INFO: LEVEL 0
  // ------------------------------
  def HL0ID       = "h010"

  // ------------------------------
  //          TRAP: LEVEL 0
  // ------------------------------
  def TL0STATUS   = "h040"
  def TL0THF      = "h041"
  def TL0EDELEG   = "h042"
  def TL0IDELEG   = "h043"
  def TL0IE       = "h044"
  def TL0TVEC     = "h045"
  
  def TL0SCRATCH  = "h046"
  def TL0EHF      = "h047"
  def TL0EPC      = "h048"
  def TL0CAUSE    = "h049"
  def TL0TVAL     = "h04a"
  def TL0IP       = "h04b"

  // ------------------------------
  //          TRAP: LEVEL 1
  // ------------------------------
  def TL1STATUS   = "h140"
  def TL1THF      = "h141"
  def TL1EDELEG   = "h142"
  def TL1IDELEG   = "h143"
  def TL1IE       = "h144"
  def TL1TVEC     = "h145"

  def TL1SCRATCH  = "h146"
  def TL1EHF      = "h147"
  def TL1EPC      = "h148"
  def TL1CAUSE    = "h149"
  def TL1TVAL     = "h14a"
  def TL1IP       = "h14b"

  // ------------------------------
  //         CONFIGURATION
  // ------------------------------
  def ENVCFG      = "h300"
  def ENVCFGH     = "h380"
}

// ******************************
//           REGISTERS
// ******************************
class CsrTlxstatusBus(nDataBit: Int, nChampTrapLvl: Int) extends Bundle {
  val ie = Vec(4, Bool())
  val pie = Vec(4, Bool())

  def toUInt: UInt = {
    return Cat(pie.asUInt, ie.asUInt)   
  }

  def status(nLvl: Int): UInt = {
    val w_ie = Wire(Vec(4, Bool()))
    val w_pie = Wire(Vec(4, Bool()))
    for (l <- 0 until 4) {
      if (l >= nLvl) {
        w_ie(l) := ie(l)
        w_pie(l) := pie(l)
      } else {
        w_ie(l) := 0.B
        w_pie(l) := 0.B
      }
    }
    return Cat(w_pie.asUInt, w_ie.asUInt)
  }
}

class CsrTlxisaBus(nDataBit: Int) extends Bundle {
  val a = Bool()
  val b = Bool()
  val i = Bool()
  val m = Bool()
  val mxl = UInt(2.W)

  def toUInt: UInt = {
    return Cat(mxl, 0.U((nDataBit - 2 - 13).W), m, 0.U(3.W), i, 0.U(6.W), b, a)  
  }
}

class CsrTlxtvecBus(nDataBit: Int) extends Bundle {
  val mode = UInt(2.W)
  val base = (UInt((nDataBit - 2).W))

  def toUInt: UInt = {
    return Cat(base, mode)  
  }

  def addr: UInt = {
    return Cat(base, 0.U(2.W))
  }
}

class CsrTlxehfBus(nDataBit: Int) extends Bundle {
  val hf = UInt(2.W)
  val hw = Bool()

  def toUInt: UInt = {
    return Cat(hw, 0.U((nDataBit - 6).W), hf)  
  }
}

class CsrTlxieBus(nDataBit: Int) extends Bundle {
  val sie = Vec(4, Bool())
  val tie = Vec(4, Bool())
  val eie = Vec(4, Bool())

  def toUInt: UInt = {
    return Cat(0.U((nDataBit - 12).W), eie.asUInt, tie.asUInt, sie.asUInt)  
  }
}

class CsrTlxcauseBus(nDataBit: Int) extends Bundle {
  val code = UInt((nDataBit - 1).W)
  val irq = Bool()

  def toUInt: UInt = {
    return Cat(irq, code)  
  }

  def low: UInt = {
    return code(log2Ceil(nDataBit) - 1, 0)
  }

  def high: UInt = {
    return code(nDataBit - 2, log2Ceil(nDataBit) - 1)
  }
}

class CsrTlxipBus(nDataBit: Int) extends Bundle {
  val sip = Vec(4, Bool())
  val tip = Vec(4, Bool())
  val eip = Vec(4, Bool())

  def toUInt: UInt = {
    return Cat(eip.asUInt, tip.asUInt, sip.asUInt)  
  }

  def mask(nLvl: Int): UInt = {
    val w_sip = Wire(Vec(4, Bool()))
    val w_tip = Wire(Vec(4, Bool()))
    val w_eip = Wire(Vec(4, Bool()))
    for (l <- 0 until 4) {
      w_sip(l) := (l >= nLvl).B
      w_tip(l) := (l >= nLvl).B
      w_eip(l) := (l >= nLvl).B
    }
    return Cat(w_eip.asUInt, w_tip.asUInt, w_sip.asUInt)
  }
}

class CsrEnvcfgBus(nDataBit: Int) extends Bundle {
  val fiom = Bool()
  val cbie = UInt(2.W)
  val cbcfe = Bool()
  val cbze = Bool()

  def toUInt: UInt = {
    return Cat(cbze, cbcfe, cbie, 0.U(3.W), fiom)  
  }
}

// ******************************
//              BUS
// ******************************
class CsrBus(nDataBit: Int, nChampTrapLvl: Int) extends Bundle {
  // ------------------------------
  //             GLOBAL
  // ------------------------------
  val chf         = UInt(nDataBit.W)
  val phf         = UInt(nDataBit.W)

  // ------------------------------
  //           HART INFO
  // ------------------------------
  val hlxvendorid = UInt(32.W)
  val hlxarchid   = UInt(nDataBit.W)
  val hlximpid    = UInt(nDataBit.W) 
  val hlxid       = UInt(nDataBit.W)

  // ------------------------------
  //           TRAP SETUP
  // ------------------------------
  val tlxstatus   = new CsrTlxstatusBus(nDataBit, nChampTrapLvl)
  val tlxisa      = new CsrTlxisaBus(nDataBit)

  val tlxthf      = Vec(nChampTrapLvl, UInt(5.W))
  val tlxedeleg   = Vec(nChampTrapLvl, Vec(nDataBit, Bool()))
  val tlxideleg   = Vec(nChampTrapLvl, Vec(nDataBit, Bool()))
  val tlxie       = Vec(nChampTrapLvl, new CsrTlxieBus(nDataBit))
  val tlxtvec     = Vec(nChampTrapLvl, new CsrTlxtvecBus(nDataBit))

  // ------------------------------
  //          TRAP HANDLING
  // ------------------------------
  val tlxscratch  = Vec(nChampTrapLvl, UInt(nDataBit.W))
  val tlxehf      = Vec(nChampTrapLvl, new CsrTlxehfBus(nDataBit))
  val tlxepc      = Vec(nChampTrapLvl, UInt(nDataBit.W))
  val tlxcause    = Vec(nChampTrapLvl, new CsrTlxcauseBus(nDataBit))
  val tlxtval     = Vec(nChampTrapLvl, UInt(nDataBit.W))
  val tlxip       = Vec(nChampTrapLvl, new CsrTlxipBus(nDataBit))

  // ------------------------------
  //         CONFIGURATION
  // ------------------------------
  val envcfg   = new CsrEnvcfgBus(nDataBit)
}

// ******************************
//             DEBUG
// ******************************
class CsrDbgBus(nDataBit: Int, nChampTrapLvl: Int) extends Bundle {
  // ------------------------------
  //             GLOBAL
  // ------------------------------
  val chf         = UInt(nDataBit.W)
  val phf         = UInt(nDataBit.W)

  // ------------------------------
  //           HART INFO
  // ------------------------------
  val hlxid       = UInt(nDataBit.W)

  // ------------------------------
  //           TRAP SETUP
  // ------------------------------
  val tlxstatus   = Vec(nChampTrapLvl, UInt(nDataBit.W))
  val tlxisa      = Vec(nChampTrapLvl, UInt(nDataBit.W))
  
  val tlxthf      = Vec(nChampTrapLvl, UInt(nDataBit.W))
  val tlxedeleg   = Vec(nChampTrapLvl, UInt(nDataBit.W))
  val tlxideleg   = Vec(nChampTrapLvl, UInt(nDataBit.W))
  val tlxie       = Vec(nChampTrapLvl, UInt(nDataBit.W))
  val tlxtvec     = Vec(nChampTrapLvl, UInt(nDataBit.W))

  // ------------------------------
  //          TRAP HANDLING
  // ------------------------------
  val tlxscratch  = Vec(nChampTrapLvl, UInt(nDataBit.W))
  val tlxehf      = Vec(nChampTrapLvl, UInt(nDataBit.W))
  val tlxepc      = Vec(nChampTrapLvl, UInt(nDataBit.W))
  val tlxcause    = Vec(nChampTrapLvl, UInt(nDataBit.W))
  val tlxtval     = Vec(nChampTrapLvl, UInt(nDataBit.W))
  val tlxip       = Vec(nChampTrapLvl, UInt(nDataBit.W))

  // ------------------------------
  //         CONFIGURATION
  // ------------------------------
  val envcfg      = UInt(64.W)
}