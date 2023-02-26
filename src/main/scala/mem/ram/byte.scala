/*
 * File: byte.scala                                                            *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:38:58 pm                                       *
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
import chisel3.experimental._

import herd.common.tools._
import herd.common.mem.mb4s._
import herd.common.dome._


class ByteRamSv (INITFILE: String, NBYTE: Int, NDATABYTE: Int)
  extends BlackBox(Map( "INITFILE" -> INITFILE,
                        "NBYTE" -> NBYTE,
                        "NDATABYTE" -> NDATABYTE)) with HasBlackBoxResource {
  val io = IO(new Bundle() {
    val clock = Input(Clock())
    val reset = Input(Reset())

    val i_p1_en = Input(Bool())
    val i_p1_wen = Input(Bool())
    val i_p1_mask = Input(UInt(NDATABYTE.W))
    val i_p1_addr = Input(UInt(log2Ceil(NBYTE).W))
    val i_p1_wdata = Input(UInt((NDATABYTE * 8).W))
    val o_p1_rdata = Output(UInt((NDATABYTE * 8).W))

    val i_p2_en = Input(Bool())
    val i_p2_wen = Input(Bool())
    val i_p2_mask = Input(UInt(NDATABYTE.W))
    val i_p2_addr = Input(UInt(log2Ceil(NBYTE).W))
    val i_p2_wdata = Input(UInt((NDATABYTE * 8).W))
    val o_p2_rdata = Output(UInt((NDATABYTE * 8).W))
  })

  addResource("/sv/ram/byte.sv")
}

class ByteRam (initFile: String, nByte: Int, nDataByte: Int) extends Module {
  val io = IO(new Bundle() {
    val b_port = Vec(2, new ByteRamIO(nDataByte, log2Ceil(nByte)))
  })

  val m_ram = Module(new ByteRamSv(initFile, nByte, nDataByte))

  m_ram.io.clock := clock
  m_ram.io.reset := reset

  m_ram.io.i_p1_en := io.b_port(0).en
  m_ram.io.i_p1_wen := io.b_port(0).wen
  m_ram.io.i_p1_mask := io.b_port(0).mask
  m_ram.io.i_p1_addr := io.b_port(0).addr
  m_ram.io.i_p1_wdata := io.b_port(0).wdata
  io.b_port(0).rdata := m_ram.io.o_p1_rdata

  m_ram.io.i_p2_en := io.b_port(1).en
  m_ram.io.i_p2_wen := io.b_port(1).wen
  m_ram.io.i_p2_mask := io.b_port(1).mask
  m_ram.io.i_p2_addr := io.b_port(1).addr
  m_ram.io.i_p2_wdata := io.b_port(1).wdata  
  io.b_port(1).rdata := m_ram.io.o_p2_rdata
}

class ByteRamCtrl (p: RamCtrlParams) extends Module {
  require((p.nPort == 1) || (p.nPort == 2), "ByteRAM has only one or two ports.")

  val io = IO(new Bundle {    
    val i_slct_read = if (p.useDomeSlct) Some(Input(new SlctBus(p.nDome, p.nPart, 1))) else None 
    val b_read = Vec(p.nPort, new CtrlReadIO(p.useDome, p.nDome, p.nAddrBit, p.nDataByte))

    val i_slct_write = if (p.useDomeSlct) Some(Input(new SlctBus(p.nDome, p.nPart, 1))) else None 
    val b_write = Vec(p.nPort, new CtrlWriteIO(p.useDome, p.nDome, p.nAddrBit, p.nDataByte))

    val b_port = Flipped(Vec(2, new ByteRamIO(p.nDataByte, log2Ceil(BigInt(p.nByte, 16)))))
  })

  for (po <- 0 until 2) {
    io.b_port(po) := DontCare
    io.b_port(po).en := false.B
  }

  // BUG 0: register ignored during simulation but necesary to create real registers.
  val r_rdata = Reg(Vec(p.nPort, UInt((p.nDataByte * 8).W)))
  for (po <- 0 until p.nPort) {
    io.b_read(po).data := r_rdata(po)
  }

  // ******************************
  //          MULTI DOME
  // ******************************
  if ((p.useDomeSlct) && (!p.isRom)) {
    // ------------------------------
    //             READ
    // ------------------------------
    val w_rvalid = Wire(Vec(p.nPort, Bool()))

    for (po <- 0 until p.nPort) {
      w_rvalid(po) := io.b_read(po).valid & (io.b_read(po).dome.get === io.i_slct_read.get.dome)
    }

    io.b_read(0).ready := w_rvalid(0)
    // BUG 0: io.b_read(0).data := io.b_port(0).rdata
    r_rdata(0) := io.b_port(0).rdata

    when (w_rvalid(0)) {
      io.b_port(0).fromRead(io.b_read(0))
    }

    if (p.nPort > 1) {
      io.b_read(1).ready := ~w_rvalid(0) & w_rvalid(1)
      // BUG 0: io.b_read(1).data := io.b_port(0).rdata
      r_rdata(1) := io.b_port(0).rdata

      when (~w_rvalid(0) & w_rvalid(1)) {
        io.b_port(0).fromRead(io.b_read(1))
      }
    }

    // ------------------------------
    //             WRITE
    // ------------------------------
    val w_wvalid = Wire(Vec(p.nPort, Bool()))

    for (po <- 0 until p.nPort) {
      w_wvalid(po) := io.b_write(po).valid & (io.b_write(po).dome.get === io.i_slct_write.get.dome)
    }

    io.b_write(0).ready := w_wvalid(0)

    when (w_wvalid(0)) {
      io.b_port(1).fromWrite(io.b_write(0))
    }

    if (p.nPort > 1) {
      io.b_write(1).ready := ~w_wvalid(0) & w_wvalid(1)

      when (~w_wvalid(0) & w_wvalid(1)) {
        io.b_port(1).fromWrite(io.b_write(1))
      }
    }

  // ******************************
  //         NO OR ONE DOME
  // ******************************
  } else {
    for (po <- 0 until p.nPort) {
      io.b_write(po).ready := true.B
      io.b_read(po).ready := ~io.b_write(po).valid 
      // BUG 0: io.b_read(po).data := io.b_port(po).rdata
      r_rdata(po) := io.b_port(po).rdata

      when (io.b_write(po).valid) {
        io.b_port(po).fromWrite(io.b_write(po))
      }.otherwise {
        io.b_port(po).fromRead(io.b_read(po))
      }
    }
  }
  
  // ******************************
  //             DEBUG
  // ******************************
  if (p.debug) {
    
  } 
}

class Mb4sByteRam (p: Mb4sRamParams) extends Module {
  val io = IO(new Bundle {    
    val b_dome = if (p.useDome) Some(Vec(p.nDome, new DomeIO(p.nAddrBit, p.nDataBit))) else None

    val i_slct = if (p.useDomeSlct) Some(Input(new SlctBus(p.nDome, p.nPart, 1))) else None 
    val b_port = MixedVec(
      for (po <- p.pPort) yield {
        val port = Flipped(new Mb4sIO(po))
        port
      }
    )
  })

  // ******************************
  //            MODULE
  // ******************************
  val m_ctrl = for (ctrl <- p.pCtrl) yield {
    val m_ctrl = Module(new Mb4sCtrl(ctrl))
    m_ctrl
  }
  val m_intf = Module(new ByteRamCtrl(p))
  val m_ram = Module(new ByteRam(p.initFile, BigInt(p.nByte, 16).toInt, p.nDataByte))

  m_ctrl(0).io.b_port <> io.b_port(0)
  m_ctrl(0).io.b_read <> m_intf.io.b_read(0)
  m_ctrl(0).io.b_write <> m_intf.io.b_write(0)

  if (p.nPort > 1) {
    m_ctrl(1).io.b_port <> io.b_port(1)
    m_ctrl(1).io.b_read <> m_intf.io.b_read(1)
    m_ctrl(1).io.b_write <> m_intf.io.b_write(1)
  }

  m_ram.io.b_port <> m_intf.io.b_port  

  // ******************************
  //             DOME
  // ******************************
  if (p.useDome) {
    for (d <- 0 until p.nDome) {
      io.b_dome.get(d).free := true.B
    }
  }
  
  if (p.useDomeSlct) {
    val r_slct = Reg(new SlctBus(p.nDome, p.nPart, 1))

    r_slct := io.i_slct.get

    if (p.pCtrl(0).useDomeSlct) {
      m_ctrl(0).io.i_slct_read.get := io.i_slct.get
      m_ctrl(0).io.i_slct_write.get := r_slct
    }
    
    if ((p.nPort > 1) && (p.pCtrl(1).useDomeSlct)) {
      m_ctrl(1).io.i_slct_read.get := io.i_slct.get
      m_ctrl(1).io.i_slct_write.get := r_slct
    }

    m_intf.io.i_slct_read.get := io.i_slct.get
    m_intf.io.i_slct_write.get := r_slct
  }
  
  // ******************************
  //             DEBUG
  // ******************************
  if (p.debug) {
    
  } 
}

object ByteRam extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new ByteRam("", 2048, 4), args)
}

object ByteRamCtrl extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new ByteRamCtrl(RamCtrlConfigBase), args)
}

object Mb4sByteRam extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Mb4sByteRam(Mb4sRamConfigBase), args)
}

