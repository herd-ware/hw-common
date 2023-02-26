/*
 * File: data.scala                                                            *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:39:01 pm                                       *
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

import herd.common.dome._
import herd.common.mem.mb4s._


class DataRamSv (INITFILE: String, NDATA: Int, NDATABYTE: Int)
  extends BlackBox(Map( "INITFILE" -> INITFILE,
                        "NDATA" -> NDATA,
                        "NDATABYTE" -> NDATABYTE)) with HasBlackBoxResource {
  val io = IO(new Bundle() {
    val clock = Input(Clock())
    val reset = Input(Reset())

    val i_p1_en = Input(Bool())
    val i_p1_wen = Input(UInt(NDATABYTE.W))
    val i_p1_addr = Input(UInt(log2Ceil(NDATA).W))
    val i_p1_wdata = Input(UInt((NDATABYTE * 8).W))
    val o_p1_rdata = Output(UInt((NDATABYTE * 8).W))

    val i_p2_en = Input(Bool())
    val i_p2_wen = Input(UInt(NDATABYTE.W))
    val i_p2_addr = Input(UInt(log2Ceil(NDATA).W))
    val i_p2_wdata = Input(UInt((NDATABYTE * 8).W))
    val o_p2_rdata = Output(UInt((NDATABYTE * 8).W))
  })

  addResource("/sv/ram/data.sv")
}

class DataRam (initFile: String, nData: Int, nDataByte: Int) extends Module {
  val io = IO(new Bundle() {
    val b_port = Vec(2, new DataRamIO(nDataByte, log2Ceil(nData)))
  })

  val m_ram = Module(new DataRamSv(initFile, nData, nDataByte))

  m_ram.io.clock := clock
  m_ram.io.reset := reset

  m_ram.io.i_p1_en := io.b_port(0).en
  m_ram.io.i_p1_wen := io.b_port(0).wen
  m_ram.io.i_p1_addr := io.b_port(0).addr
  m_ram.io.i_p1_wdata := io.b_port(0).wdata
  io.b_port(0).rdata := m_ram.io.o_p1_rdata

  m_ram.io.i_p2_en := io.b_port(1).en
  m_ram.io.i_p2_wen := io.b_port(1).wen
  m_ram.io.i_p2_addr := io.b_port(1).addr
  m_ram.io.i_p2_wdata := io.b_port(1).wdata
  io.b_port(1).rdata := m_ram.io.o_p2_rdata
}

class DataRamCtrl (p: RamCtrlParams) extends Module {
  require((p.nPort == 1) || (p.nPort == 2), "BRAM has only one or two ports.")

  val io = IO(new Bundle {    
    val i_slct_read = if (p.useDomeSlct) Some(Input(new SlctBus(p.nDome, p.nPart, 1))) else None 
    val b_read = Vec(p.nPort, new CtrlReadIO(p.useDome, p.nDome, p.nAddrBit, p.nDataByte))

    val i_slct_write = if (p.useDomeSlct) Some(Input(new SlctBus(p.nDome, p.nPart, 1))) else None 
    val b_write = Vec(p.nPort, new CtrlWriteIO(p.useDome, p.nDome, p.nAddrBit, p.nDataByte))

    val b_port = Flipped(Vec(2, new DataRamIO(p.nDataByte, p.nAddrBit)))
  })

  // ******************************
  //          MULTI DOME
  // ******************************
  for (po <- 0 until 2) {
    io.b_port(po) := DontCare
    io.b_port(po).en := false.B
  }

  if ((p.useDomeSlct) && (!p.isRom)) {
    // ------------------------------
    //             READ
    // ------------------------------
    val w_rvalid = Wire(Vec(p.nPort, Bool()))
    val r_roffset = Reg(UInt(log2Ceil(p.nDataByte).W))

    // Valid
    for (po <- 0 until p.nPort) {
      w_rvalid(po) := io.b_read(po).valid & (io.b_read(po).dome.get === io.i_slct_read.get.dome)
    }

    // Select
    io.b_read(0).ready := w_rvalid(0)

    when (w_rvalid(0)) {
      io.b_port(0).fromRead(io.b_read(0))
    }

    if (p.nPort > 1) {
      io.b_read(1).ready := ~w_rvalid(0) & w_rvalid(1)

      when (~w_rvalid(0) & w_rvalid(1)) {
        io.b_port(0).fromRead(io.b_read(1))
      }
    }

    // Format
    r_roffset := io.b_read(0).addr(log2Ceil(p.nDataByte) - 1, 0)
    if (p.nPort > 1) {
      when (~w_rvalid(0)) {
        r_roffset := io.b_read(1).addr(log2Ceil(p.nDataByte) - 1, 0)
      }
    }

    io.b_read(0).data := (io.b_port(0).rdata >> (r_roffset << 3.U))
    if (p.nPort > 1) {
      io.b_read(1).data := (io.b_port(0).rdata >> (r_roffset << 3.U))
    }

    // ------------------------------
    //             WRITE
    // ------------------------------
    val w_wvalid = Wire(Vec(p.nPort, Bool()))

    // Valid
    for (po <- 0 until p.nPort) {
      w_wvalid(po) := io.b_write(po).valid & (io.b_write(po).dome.get === io.i_slct_write.get.dome)
    }

    // Select
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

      // ------------------------------
      //           READ DATA
      // ------------------------------
      val r_roffset = Reg(UInt(log2Ceil(p.nDataByte).W))

      r_roffset := io.b_read(po).addr(log2Ceil(p.nDataByte) - 1, 0)

      io.b_read(po).data := (io.b_port(po).rdata >> (r_roffset << 3.U))

      // ------------------------------
      //             WRITE
      // ------------------------------
      when (io.b_write(po).valid) {
        io.b_port(po).fromWrite(io.b_write(po))

      // ------------------------------
      //             READ
      // ------------------------------      
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

class Mb4sDataRam (p: Mb4sRamParams) extends Module {
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
  val m_intf = Module(new DataRamCtrl(p))
  val m_ram = Module(new DataRam(p.initFile, p.nData, p.nDataByte))

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

object DataRam extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new DataRam("", 2048, 4), args)
}

object DataRamCtrl extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new DataRamCtrl(RamCtrlConfigBase), args)
}

object Mb4sDataRam extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Mb4sDataRam(Mb4sRamConfigBase), args)
}