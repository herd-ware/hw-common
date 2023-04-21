/*
 * File: csr.scala                                                             *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-04-21 10:14:31 am                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.isa.priv

import chisel3._
import chisel3.util._


// ******************************
//            ADDRESS
// ******************************
object CSR {
  // ------------------------------
  //         MACHINE INFOS
  // ------------------------------
  def MVENDORID = "hf11"
  def MARCHID   = "hf12"
  def MIMPID    = "hf13"
  def MHARTID   = "hf14"

  // ------------------------------
  //       MACHINE TRAP SETUP
  // ------------------------------
  def MSTATUS   = "h300"
  def MISA      = "h301"
  def MEDELEG   = "h302"
  def MIDELEG   = "h303"
  def MIE       = "h304"
  def MTVEC     = "h305"
  def MSTATUSH  = "h310"

  // ------------------------------
  //      MACHINE TRAP HANDLING
  // ------------------------------
  def MSCRATCH  = "h340"
  def MEPC      = "h341"
  def MCAUSE    = "h342"
  def MTVAL     = "h343"
  def MIP       = "h344"

  // ------------------------------
  //     MACHINE CONFIGURATION
  // ------------------------------
  def MENVCFG   = "h30a"
  def MENVCFGH  = "h31a"
}

// ******************************
//           REGISTERS
// ******************************
// ------------------------------
//              ALL
// ------------------------------
class CsrXstatusBus(nDataBit: Int) extends Bundle {
  val mie = Bool()
  val mpie = Bool()
  val mpp = UInt(2.W)

  def toUInt: UInt = {
    if (nDataBit == 64) {
      return Cat(mpp, 0.U(3.W), mpie, 0.U(3.W), mie, 0.U(3.W))
    } else {
      return Cat(mpp, 0.U(3.W), mpie, 0.U(3.W), mie, 0.U(3.W))
    }    
  }

  def m: UInt = {
    if (nDataBit == 64) {
      return Cat(mpp, 0.U(3.W), mpie, 0.U(3.W), mie, 0.U(3.W))
    } else {
      return Cat(mpp, 0.U(3.W), mpie, 0.U(3.W), mie, 0.U(3.W))
    } 
  }
}

class CsrXisaBus(nDataBit: Int) extends Bundle {
  val a = Bool()
  val b = Bool()
  val i = Bool()
  val m = Bool()
  val mxl = UInt(2.W)

  def toUInt: UInt = {
    return Cat(mxl, 0.U((nDataBit - 2 - 13).W), m, 0.U(3.W), i, 0.U(6.W), b, a)  
  }
}

// ------------------------------
//            MACHINE
// ------------------------------
class CsrMtvecBus(nDataBit: Int) extends Bundle {
  val mode = UInt(2.W)
  val base = (UInt((nDataBit - 2).W))

  def toUInt: UInt = {
    return Cat(base, mode)  
  }

  def addr: UInt = {
    return Cat(base, 0.U(2.W))
  }
}

class CsrMieBus(nDataBit: Int) extends Bundle {
  val msie = Bool()
  val mtie = Bool()
  val meie = Bool()

  def toUInt: UInt = {
    return Cat(0.U((nDataBit - 9).W), meie, 0.U(3.W), mtie, 0.U(3.W), msie)  
  }
}

class CsrMcauseBus(nDataBit: Int) extends Bundle {
  val code = UInt((nDataBit - 1).W)
  val irq = Bool()

  def toUInt: UInt = {
    return Cat(irq, code)  
  }
}

class CsrMipBus(nDataBit: Int) extends Bundle {
  val msip = Bool()
  val mtip = Bool()
  val meip = Bool()

  def toUInt: UInt = {
    return Cat(0.U(3.W), meip, 0.U(3.W), mtip, 0.U(3.W), msip)  
  }
}

class CsrMenvcfgBus(nDataBit: Int) extends Bundle {
  val fiom = Bool()
  val cbie = UInt(2.W)
  val cbcfe = Bool()
  val cbze = Bool()

  def toUInt: UInt = {
    return Cat(cbze, cbcfe, cbie, 0.U(3.W), fiom)  
  }
}

// ******************************
//             BUS
// ******************************
class CsrBus(nDataBit: Int) extends Bundle {
  // ------------------------------
  //         MACHINE INFOS
  // ------------------------------
  val mvendorid = UInt(32.W)
  val marchid   = UInt(nDataBit.W)
  val mimpid    = UInt(nDataBit.W) 
  val mhartid   = UInt(nDataBit.W)

  // ------------------------------
  //       MACHINE TRAP SETUP
  // ------------------------------
  val cp        = UInt(2.W)
  val xstatus   = new CsrXstatusBus(nDataBit)
  val xisa      = new CsrXisaBus(nDataBit)

  val medeleg   = Vec(nDataBit, Bool())
  val mideleg   = Vec(nDataBit, Bool())
  val mie       = new CsrMieBus(nDataBit)
  val mtvec     = new CsrMtvecBus(nDataBit)
  
  // ------------------------------
  //      MACHINE TRAP HANDLING
  // ------------------------------  
  val mscratch  = UInt(nDataBit.W)
  val mepc      = UInt(nDataBit.W)
  val mcause    = new CsrMcauseBus(nDataBit)
  val mtval     = UInt(nDataBit.W) 
  val mip       = new CsrMipBus(nDataBit)

  // ------------------------------
  //     MACHINE CONFIGURATION
  // ------------------------------
  val menvcfg   = new CsrMenvcfgBus(nDataBit)
}

// ******************************
//             DEBUG
// ******************************
class CsrDbgBus(nDataBit: Int) extends Bundle {
  // ------------------------------
  //         MACHINE INFOS
  // ------------------------------
  val mvendorid = UInt(32.W)
  val marchid   = UInt(nDataBit.W)
  val mimpid    = UInt(nDataBit.W) 
  val mhartid   = UInt(nDataBit.W)

  // ------------------------------
  //       MACHINE TRAP SETUP
  // ------------------------------
  val cp        = UInt(2.W)
  val mstatus   = UInt(64.W)
  val misa      = UInt(nDataBit.W)
  val medeleg   = UInt(nDataBit.W)
  val mideleg   = UInt(nDataBit.W)
  val mie       = UInt(nDataBit.W)
  val mtvec     = UInt(nDataBit.W)
  
  // ------------------------------
  //      MACHINE TRAP HANDLING
  // ------------------------------  
  val mscratch  = UInt(nDataBit.W)
  val mepc      = UInt(nDataBit.W)
  val mcause    = UInt(nDataBit.W)
  val mtval     = UInt(nDataBit.W) 
  val mip       = UInt(nDataBit.W)

  // ------------------------------
  //     MACHINE CONFIGURATION
  // ------------------------------
  val menvcfg   = UInt(64.W)
}
