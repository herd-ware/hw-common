/*
 * File: cross.scala                                                           *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:28:39 pm                                       *
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

import herd.common.gen._
import herd.common.dome._


class Mb4sCrossbarReq (p: Mb4sCrossbarParams) extends Module {
  require ((p.useMem || (p.nMaster >= p.nSlave)), "Number of masters must be greater or equal than the number of slaves.")
  
  val io = IO(new Bundle {
    val i_slct = if (p.useDomeSlct) Some(Input(new SlctBus(p.nDome, p.nPart, 1))) else None

    val b_m = MixedVec(
      for (ma <- p.pMaster) yield {
        val port = Flipped(new Mb4sReqIO(ma))
        port
      }
    )
    val b_s = Vec(p.nSlave, new Mb4sReqIO(p.pSlave))
    
    val b_mnode = Vec(p.nMaster, new GenDRVIO(p, new Mb4sNodeBus(p.nSlave), UInt(0.W)))
    val b_snode = Vec(p.nSlave, new GenDRVIO(p, new Mb4sNodeBus(p.nMaster), UInt(0.W)))
  })

  val w_addr = Wire(Vec(p.nMaster, UInt(p.nAddrBit.W)))
  val w_mem = Wire(Vec(p.nMaster, Vec(p.nMem, Bool())))
  val w_def = Wire(Vec(p.nMaster, Vec(p.nDefault, Bool())))
  val w_zero = Wire(Vec(p.nMaster, Bool()))
  val w_slave = Wire(Vec(p.nMaster, Vec(p.nSlave, Bool())))
  val w_mreq = Wire(Vec(p.nMaster, Vec(p.nSlave, Bool())))
  val w_sreq = Wire(Vec(p.nSlave, Vec(p.nMaster, Bool())))
  val w_mnode = Wire(Vec(p.nMaster, UInt(log2Ceil(p.nSlave).W)))
  val w_snode = Wire(Vec(p.nSlave, UInt(log2Ceil(p.nMaster).W)))

  // ******************************
  //            MEMORY
  // ******************************
  w_slave := DontCare
  
  for (ma <- 0 until p.nMaster) {
    for (me <- 0 until p.nMem) {
      w_mem(ma)(me) := (w_addr(ma) >= BigInt(p.pMem(me).nAddrBase, 16).U) & (w_addr(ma) < (BigInt(p.pMem(me).nAddrBase, 16) + BigInt(p.pMem(me).nByte, 16)).U)
      w_slave(ma)(me) := w_mem(ma)(me)
    }

    for (d <- 0 until p.nDefault) {
      w_def(ma)(d) := ~w_mem(ma).asUInt.orR
      w_slave(ma)(p.nMem + d) := w_def(ma)(d)
    }

    w_zero(ma) := ~w_slave(ma).asUInt.orR
  }
  
  // ******************************
  //           MULTI DOME
  // ******************************
  if (p.useDomeSlct) {
    // ------------------------------
    //            DEFAULT
    // ------------------------------
    val m_req = for (master <- p.pMaster) yield {
      val m_req = Module(new Mb4sReqSReg(master, false))
      m_req
    } 

    for (ma <- 0 until p.nMaster) {
      m_req(ma).io.b_port <> io.b_m(ma)
      if (p.pMaster(ma).useDomeSlct) m_req(ma).io.i_slct.get := io.i_slct.get
      m_req(ma).io.b_sout.ready := false.B   

      io.b_mnode(ma) := DontCare
      for (ds <- 0 until p.nDomeSlct) {
        io.b_mnode(ma).valid(ds) := false.B
      } 
    }

    for (s <- 0 until p.nSlave) {
      io.b_s(s).valid := false.B
      io.b_s(s).dome.get := io.i_slct.get.dome
      io.b_s(s).ctrl := DontCare  

      io.b_snode(s) := DontCare
      for (ds <- 0 until p.nDomeSlct) {
        io.b_snode(s).valid(ds) := false.B
      }
    }

    // ------------------------------
    //            ADDRESS
    // ------------------------------
    for (ma <- 0 until p.nMaster) {
      w_addr(ma) := m_req(ma).io.b_sout.ctrl.get.addr
    }

    // ------------------------------
    //            SELECT
    // ------------------------------
    for (ma <- 0 until p.nMaster) {
      for (s <- 0 until p.nSlave) {
        if (p.useMem) {
          if (p.pMaster(ma).useDomeSlct) {   
            w_mreq(ma)(s) := m_req(ma).io.b_sout.valid & w_slave(ma)(s)
          } else {
            w_mreq(ma)(s) := m_req(ma).io.b_sout.valid & (m_req(ma).io.b_sout.dome.get === io.i_slct.get.dome) & w_slave(ma)(s)     
          }
        } else {
          if (p.pMaster(ma).useDomeSlct) {   
            w_mreq(ma)(s) := m_req(ma).io.b_sout.valid  
          } else {
            w_mreq(ma)(s) := m_req(ma).io.b_sout.valid & (m_req(ma).io.b_sout.dome.get === io.i_slct.get.dome)     
          } 
        }
      }
    }

    for (ma0 <- 0 until p.nMaster) {
      for (s0 <- 0 until p.nSlave) {
        for (s1 <- (s0 + 1) until p.nSlave) {
          when (w_mreq(ma0)(s0)) {
            w_mreq(ma0)(s1) := false.B
          }
        }
        for (ma1 <- (ma0 + 1) until p.nMaster) {
          when (w_mreq(ma0)(s0)) {
            w_mreq(ma1)(s0) := false.B
          }
        }
      }
    }

    for (ma <- 0 until p.nMaster) {
      w_mnode(ma) := PriorityEncoder(w_mreq(ma).asUInt)
    }

    for (s <- 0 until p.nSlave) {
      for (ma <- 0 until p.nMaster) {
        w_sreq(s)(ma) := w_mreq(ma)(s)
      }
      w_snode(s) := PriorityEncoder(w_sreq(s).asUInt)
    }

    // ------------------------------
    //            CONNECT
    // ------------------------------
    for (ma <- 0 until p.nMaster) {
      // Default if no mem
      if (p.useMem) {
        when (w_zero(ma)) {
          m_req(ma).io.b_sout.ready := io.b_mnode(ma).ready(io.i_slct.get.dome)
        }
      }

      // Normal
      for (s <- 0 until p.nSlave) {
        when ((ma.U === w_snode(s)) & w_sreq(s)(ma)) {
          m_req(ma).io.b_sout.ready := io.b_s(s).ready(io.i_slct.get.dome) & io.b_snode(s).ready(io.i_slct.get.dome) & io.b_mnode(ma).ready(io.i_slct.get.dome)
          
          io.b_s(s).valid := io.b_snode(s).ready(io.i_slct.get.dome) & io.b_mnode(ma).ready(io.i_slct.get.dome)   
          io.b_s(s).ctrl.hart := m_req(ma).io.b_sout.ctrl.get.hart
          io.b_s(s).ctrl.op := m_req(ma).io.b_sout.ctrl.get.op
          if (p.pMaster(ma).useAmo) io.b_s(s).ctrl.amo.get := m_req(ma).io.b_sout.ctrl.get.amo.get
          io.b_s(s).ctrl.size := m_req(ma).io.b_sout.ctrl.get.size
          io.b_s(s).ctrl.addr := m_req(ma).io.b_sout.ctrl.get.addr 
        }
      }
    }

    // ------------------------------
    //             NODE
    // ------------------------------  
    for (ma <- 0 until p.nMaster) {
      // Default if no mem
      if (p.useMem) {
        for (ds <- 0 until p.nDomeSlct) {  
          when (m_req(ma).io.b_sout.valid & w_zero(ma) & (ds.U === io.i_slct.get.dome)) {
            io.b_mnode(ma).valid(ds) := true.B
            if (!p.readOnly) io.b_mnode(ma).ctrl.get(ds).op := NODE.fromMb4s(p.pMaster(ma), m_req(ma).io.b_sout.ctrl.get.op)
            io.b_mnode(ma).ctrl.get(ds).zero := true.B
          }
        }
      }

      // Normal
      for (s <- 0 until p.nSlave) {
        for (ds <- 0 until p.nDomeSlct) {          
          when (w_mreq(ma).asUInt.orR & (ds.U === io.i_slct.get.dome) & (ma.U === w_snode(s)) & (s.U === w_mnode(ma))) {
            io.b_mnode(ma).valid(ds) := io.b_s(s).ready(ds) & io.b_snode(s).ready(ds)
            if (!p.readOnly) io.b_mnode(ma).ctrl.get(ds).op := NODE.fromMb4s(p.pMaster(ma), m_req(ma).io.b_sout.ctrl.get.op)
            io.b_mnode(ma).ctrl.get(ds).zero := false.B
            io.b_mnode(ma).ctrl.get(ds).node := w_mnode(ma)

            io.b_snode(s).valid(ds) := io.b_s(s).ready(ds) & io.b_mnode(ma).ready(ds) 
            if (!p.readOnly) io.b_snode(s).ctrl.get(ds).op := NODE.fromMb4s(p.pMaster(ma), m_req(ma).io.b_sout.ctrl.get.op)    
            io.b_snode(s).ctrl.get(ds).node := w_snode(s)       
          }
        }
      }
    }

  // ******************************
  //            NORMAL
  // ******************************
  } else {
    // ------------------------------
    //            DEFAULT
    // ------------------------------
    for (ma <- 0 until p.nMaster) {
      io.b_m(ma) := DontCare
      io.b_m(ma).ready(0) := false.B    

      io.b_mnode(ma) := DontCare
      io.b_mnode(ma).valid(0) := false.B      
    }

    for (s <- 0 until p.nSlave) {
      io.b_s(s) := DontCare
      io.b_s(s).valid := false.B

      io.b_snode(s) := DontCare
      io.b_snode(s).valid(0) := false.B      
    }    

    // ------------------------------
    //            ADDRESS
    // ------------------------------
    for (ma <- 0 until p.nMaster) {
      w_addr(ma) := io.b_m(ma).ctrl.addr
    }

    // ------------------------------
    //            SELECT
    // ------------------------------
    for (ma <- 0 until p.nMaster) {
      for (s <- 0 until p.nSlave) {
        if (p.useMem) {
          w_mreq(ma)(s) := io.b_m(ma).valid & w_slave(ma)(s)
        } else {
          w_mreq(ma)(s) := io.b_m(ma).valid   
        }
      }
    }

    for (ma0 <- 0 until p.nMaster) {
      for (s0 <- 0 until p.nSlave) {
        for (s1 <- (s0 + 1) until p.nSlave) {
          when (w_mreq(ma0)(s0)) {
            w_mreq(ma0)(s1) := false.B
          }
        }
        for (ma1 <- (ma0 + 1) until p.nMaster) {
          when (w_mreq(ma0)(s0)) {
            w_mreq(ma1)(s0) := false.B
          }
        }
      }
    }

    for (ma <- 0 until p.nMaster) {
      w_mnode(ma) := PriorityEncoder(w_mreq(ma).asUInt)
    }

    for (s <- 0 until p.nSlave) {
      for (ma <- 0 until p.nMaster) {
        w_sreq(s)(ma) := w_mreq(ma)(s)
      }
      w_snode(s) := PriorityEncoder(w_sreq(s).asUInt)
    }
    
    // ------------------------------
    //            CONNECT
    // ------------------------------
    for (ma <- 0 until p.nMaster) {
      // Default if no mem
      if (p.useMem) {
        when (w_zero(ma)) {
          io.b_m(ma).ready(0) := io.b_mnode(ma).ready(0)
        }
      }

      // Normal
      for (s <- 0 until p.nSlave) {
        when ((ma.U === w_snode(s)) & w_sreq(s)(ma)) {
          io.b_m(ma).ready(0) := io.b_s(s).ready(0) & io.b_snode(s).ready(0) & io.b_mnode(ma).ready(0)

          io.b_s(s).valid := io.b_snode(s).ready(0) & io.b_mnode(ma).ready(0)
          io.b_s(s).ctrl.hart := io.b_m(ma).ctrl.hart
          io.b_s(s).ctrl.op := io.b_m(ma).ctrl.op
          if (p.pMaster(ma).useAmo) io.b_s(s).ctrl.amo.get := io.b_m(ma).ctrl.amo.get
          io.b_s(s).ctrl.size := io.b_m(ma).ctrl.size
          io.b_s(s).ctrl.addr := io.b_m(ma).ctrl.addr
        }
      }
    }

    // ------------------------------
    //             NODE
    // ------------------------------  
    for (ma <- 0 until p.nMaster) {
      // Default if no mem
      if (p.useMem) {
        when (io.b_m(ma).valid & w_zero(ma)) {
          io.b_mnode(ma).valid(0) := true.B
          if (!p.readOnly) io.b_mnode(ma).ctrl.get(0).op := NODE.fromMb4s(p.pMaster(ma), io.b_m(ma).ctrl.op)
          io.b_mnode(ma).ctrl.get(0).zero := true.B
        }        
      }

      // Normal
      for (s <- 0 until p.nSlave) {
        when (w_mreq(ma).asUInt.orR & (ma.U === w_snode(s)) & (s.U === w_mnode(ma))) {
          io.b_mnode(ma).valid(0) := io.b_s(s).ready(0) & io.b_snode(s).ready(0)
          if (p.useDomeTag) io.b_mnode(ma).dome.get := io.b_m(ma).dome.get
          if (!p.readOnly) io.b_mnode(ma).ctrl.get(0).op := NODE.fromMb4s(p.pMaster(ma), io.b_m(ma).ctrl.op)
          io.b_mnode(ma).ctrl.get(0).zero := false.B
          io.b_mnode(ma).ctrl.get(0).node := w_mnode(ma)

          io.b_snode(s).valid(0) := io.b_s(s).ready(0) & io.b_mnode(ma).ready(0)
          if (!p.readOnly) io.b_snode(s).ctrl.get(0).op := NODE.fromMb4s(p.pMaster(ma), io.b_m(ma).ctrl.op)
          io.b_snode(s).ctrl.get(0).node := w_snode(s)
        }        
      }
    }
  }

  // ******************************
  //             DEBUG
  // ******************************
  if (p.debug) {
    dontTouch(io.b_snode)
    dontTouch(io.b_mnode)
    if (p.useMem) {
      for (me <- 0 until p.nMem) {
        var v_start: String = "%08x".format(BigInt(p.pMem(me).nAddrBase, 16))
        var v_end: String = "%08x".format(BigInt(p.pMem(me).nAddrBase, 16) + BigInt(p.pMem(me).nByte, 16))

        if (p.nAddrBit > 32) {
          v_start = v_start.takeRight(16)
          v_end = v_end.takeRight(16)
        } else {
          v_start = v_start.takeRight(8)
          v_end = v_end.takeRight(8)
        }

        println(("Mem " + me + " range : 0x" + v_start + " | 0x" + v_end))
      }

      for (d <- 0 until p.nDefault) {
        var v_start: String = ""
        var v_end: String = ""

        if (p.nDataBit == 64) {
          v_start = "0000000000000000"
          v_end = "ffffffffffffffff"
        } else {
          v_start = "00000000"
          v_end = "ffffffff"
        }

        println(("Mem " + (p.nMem + d) + " range : 0x" + v_start + " | 0x" + v_end))
      }
    }
  } 
}

class Mb4sCrossbarWrite (p: Mb4sCrossbarParams) extends Module {
  val io = IO(new Bundle {
    val i_slct = if (p.useDomeSlct) Some(Input(new SlctBus(p.nDome, p.nPart, 1))) else None

    val b_m = MixedVec(
      for (ma <- p.pMaster) yield {
        val port = Flipped(new Mb4sDataIO(ma))
        port
      }
    )
    val b_s = Vec(p.nSlave, new Mb4sDataIO(p.pSlave))

    val b_mnode = Vec(p.nMaster, Flipped(new GenDRVIO(p, new Mb4sNodeBus(p.nSlave), UInt(0.W))))
    val b_snode = Vec(p.nSlave, Flipped(new GenDRVIO(p, new Mb4sNodeBus(p.nMaster), UInt(0.W))))
  })

  val w_mnode = Wire(Vec(p.nMaster, UInt(log2Ceil(p.nSlave).W)))
  val w_snode = Wire(Vec(p.nSlave, UInt(log2Ceil(p.nMaster).W)))
  
  // ******************************
  //           MULTI DOME
  // ******************************
  if (p.useDomeSlct) {  
    // ------------------------------
    //            DEFAULT
    // ------------------------------
    val m_data = for (master <- p.pMaster) yield {
      val m_data = Module(new Mb4sDataSReg(master))
      m_data
    } 

    for (ma <- 0 until p.nMaster) {
      m_data(ma).io.b_port <> io.b_m(ma)
      if (p.pMaster(ma).useDomeSlct) m_data(ma).io.i_slct.get := io.i_slct.get
      m_data(ma).io.b_sout.ready := false.B

      for (ds <- 0 until p.nDomeSlct) { 
        io.b_mnode(ma).ready(ds) := false.B
      }
    }

    for (s <- 0 until p.nSlave) {
      io.b_s(s) := DontCare
      io.b_s(s).valid := false.B

      for (ds <- 0 until p.nDomeSlct) { 
        io.b_snode(s).ready(ds) := false.B
      }
    }

    // ------------------------------
    //             NODE
    // ------------------------------
    for (ma <- 0 until p.nMaster) {
      w_mnode(ma) := io.b_mnode(ma).ctrl.get(io.i_slct.get.dome).node
    }

    for (s <- 0 until p.nSlave) {
      w_snode(s) := io.b_snode(s).ctrl.get(io.i_slct.get.dome).node
    }

    // ------------------------------
    //            CONNECT
    // ------------------------------
    if (!p.readOnly) { 
      for (ma <- 0 until p.nMaster) {
        when (io.b_mnode(ma).ctrl.get(io.i_slct.get.dome).w) {
          // Default if no mem
          if (p.useMem) {
            when (io.b_mnode(ma).ctrl.get(io.i_slct.get.dome).zero) {
              if (p.pMaster(ma).multiDome) {
                m_data(ma).io.b_sout.ready := io.b_mnode(ma).valid(io.i_slct.get.dome)
              } else {
                m_data(ma).io.b_sout.ready := io.b_mnode(ma).valid(io.i_slct.get.dome) & (m_data(ma).io.b_sout.dome.get === io.i_slct.get.dome)
              }
              io.b_mnode(ma).ready(io.i_slct.get.dome) := m_data(ma).io.b_sout.valid
            }
          }

          // Normal
          for (s <- 0 until p.nSlave) {
            for (ds <- 0 until p.nDomeSlct) {
              when ((ds.U === io.i_slct.get.dome) & (ma.U === w_snode(s) & (s.U === w_mnode(ma)) & ~io.b_mnode(ma).ctrl.get(ds).zero)) {
                when (io.b_s(s).ready(ds) & io.b_mnode(ma).valid(ds)) {
                  if (p.pMaster(ma).multiDome) {
                    m_data(ma).io.b_sout.ready := io.b_s(s).ready(ds)

                    io.b_s(s).valid := m_data(ma).io.b_sout.valid
                    io.b_s(s).dome.get := ds.U
                    io.b_s(s).data := m_data(ma).io.b_sout.data.get
                  } else {
                    m_data(ma).io.b_sout.ready := io.b_s(s).ready(ds) & (ds.U === m_data(ma).io.b_sout.dome.get)

                    io.b_s(s).valid := m_data(ma).io.b_sout.valid & (ds.U === m_data(ma).io.b_sout.dome.get)
                    io.b_s(s).dome.get := ds.U
                    io.b_s(s).data := m_data(ma).io.b_sout.data.get
                  }
                }
                
                if (p.pMaster(ma).multiDome) {
                  io.b_mnode(ma).ready(ds) := m_data(ma).io.b_sout.valid & io.b_s(s).ready(ds) & io.b_snode(s).valid(ds)
                  io.b_snode(s).ready(ds) := m_data(ma).io.b_sout.valid & io.b_s(s).ready(ds) & io.b_mnode(ma).valid(ds)
                } else {
                  io.b_mnode(ma).ready(ds) := m_data(ma).io.b_sout.valid & io.b_s(s).ready(ds) & io.b_snode(s).valid(ds) & (ds.U === m_data(ma).io.b_sout.dome.get)
                  io.b_snode(s).ready(ds) := m_data(ma).io.b_sout.valid & io.b_s(s).ready(ds) & io.b_mnode(ma).valid(ds) & (ds.U === m_data(ma).io.b_sout.dome.get)
                }
              }
            }
          }
        } 
      }
    }

  // ******************************
  //            NORMAL
  // ******************************
  } else {
    // ------------------------------
    //            DEFAULT
    // ------------------------------
    for (ma <- 0 until p.nMaster) {
      io.b_m(ma).ready(0) := false.B   

      io.b_mnode(ma).ready(0) := false.B        
    }

    for (s <- 0 until p.nSlave) {
      io.b_s(s).valid := false.B
      if (p.useDome) io.b_s(s).dome.get := io.b_m(0).dome.get
      io.b_s(s).data := io.b_m(0).data

      io.b_snode(s).ready(0) := false.B
    }

    // ------------------------------
    //             NODE
    // ------------------------------
    for (ma <- 0 until p.nMaster) {
      w_mnode(ma) := io.b_mnode(ma).ctrl.get(0).node
    }

    for (s <- 0 until p.nSlave) {
      w_snode(s) := io.b_snode(s).ctrl.get(0).node
    }

    // ------------------------------
    //            CONNECT
    // ------------------------------
    if (!p.readOnly) { 
      for (ma <- 0 until p.nMaster) {
        when (io.b_mnode(ma).ctrl.get(0).w) {
          // Default if no mem
          if (p.useMem) {
            when (io.b_mnode(ma).ctrl.get(0).zero) {
              io.b_m(ma).ready(0) := io.b_mnode(ma).valid(0)
              io.b_mnode(ma).ready(0) := io.b_m(ma).valid
            }
          }

          // Normal
          for (s <- 0 until p.nSlave) {
            when ((ma.U === w_snode(s)) & (s.U === w_mnode(ma)) & ~io.b_mnode(ma).ctrl.get(0).zero) {
              when (io.b_mnode(ma).valid(0) & io.b_snode(s).valid(0)) {
                io.b_m(ma).ready(0) := io.b_s(s).ready(0)

                io.b_s(s).valid := io.b_m(ma).valid
                if (p.useDomeTag) io.b_s(s).dome.get := io.b_m(ma).dome.get
                io.b_s(s).data := io.b_m(ma).data
              }

              io.b_mnode(ma).ready(0) := io.b_m(ma).valid & io.b_s(s).ready(0) & io.b_snode(s).valid(0)
              io.b_snode(s).ready(0) := io.b_m(ma).valid & io.b_s(s).ready(0) & io.b_mnode(ma).valid(0)
            }            
          }
        } 
      }
    }
  }

  // ******************************
  //             DEBUG
  // ******************************
  if (p.debug) {
    dontTouch(io.b_mnode)  
    dontTouch(io.b_snode)    
  } 
}

class Mb4sCrossbarRead (p: Mb4sCrossbarParams) extends Module {
  val io = IO(new Bundle {
    val i_slct = if (p.useDomeSlct) Some(Input(new SlctBus(p.nDome, p.nPart, 1))) else None

    val b_m = MixedVec(
      for (ma <- p.pMaster) yield {
        val port = new Mb4sDataIO(ma)
        port
      }
    )
    val b_s = Vec(p.nSlave, Flipped(new Mb4sDataIO(p.pSlave)))

    val b_mnode = Vec(p.nMaster, Flipped(new GenDRVIO(p, new Mb4sNodeBus(p.nSlave), UInt(0.W))))
    val b_snode = Vec(p.nSlave, Flipped(new GenDRVIO(p, new Mb4sNodeBus(p.nMaster), UInt(0.W))))
  })

  val w_mnode = Wire(Vec(p.nMaster, UInt(log2Ceil(p.nSlave).W)))
  val w_snode = Wire(Vec(p.nSlave, UInt(log2Ceil(p.nMaster).W)))
  
  // ******************************
  //           MULTI DOME
  // ******************************
  if (p.useDomeSlct) {  
    // ------------------------------
    //            DEFAULT
    // ------------------------------
    val m_data = for (s <- 0 until p.nSlave) yield {
      val m_data = Module(new Mb4sDataSReg(p.pSlave))
      m_data
    } 

    for (ma <- 0 until p.nMaster) {
      io.b_m(ma) := DontCare
      io.b_m(ma).valid := false.B

      for (ds <- 0 until p.nDomeSlct) { 
        io.b_mnode(ma).ready(ds) := false.B
      }
    }

    for (s <- 0 until p.nSlave) {
      m_data(s).io.b_port <> io.b_s(s)
      m_data(s).io.i_slct.get := io.i_slct.get
      m_data(s).io.b_sout.ready := false.B

      for (ds <- 0 until p.nDomeSlct) { 
        io.b_snode(s).ready(ds) := false.B
      }
    }

    // ------------------------------
    //             NODE
    // ------------------------------
    for (ma <- 0 until p.nMaster) {
      w_mnode(ma) := io.b_mnode(ma).ctrl.get(io.i_slct.get.dome).node
    }

    for (s <- 0 until p.nSlave) {
      w_snode(s) := io.b_snode(s).ctrl.get(io.i_slct.get.dome).node
    }

    // ------------------------------
    //            CONNECT
    // ------------------------------
    for (ma <- 0 until p.nMaster) {
      when (io.b_mnode(ma).ctrl.get(io.i_slct.get.dome).r) {
        // Default if no mem
        if (p.useMem) {
          when (io.b_mnode(ma).ctrl.get(io.i_slct.get.dome).zero) {
            io.b_m(ma).valid := io.b_mnode(ma).valid(io.i_slct.get.dome)
            io.b_mnode(ma).ready(io.i_slct.get.dome) := io.b_m(ma).ready(io.i_slct.get.dome)
          }
        }

        // Normal
        for (s <- 0 until p.nSlave) {
          for (ds <- 0 until p.nDomeSlct) {
            when ((ds.U === io.i_slct.get.dome) & (ma.U === w_snode(s) & (s.U === w_mnode(ma)) & ~io.b_mnode(ma).ctrl.get(ds).zero)) {
              when (io.b_mnode(ma).valid(ds) & io.b_snode(s).valid(ds)) {
                io.b_m(ma).valid := m_data(s).io.b_sout.valid
                io.b_m(ma).dome.get := ds.U
                io.b_m(ma).data := m_data(s).io.b_sout.data.get

                m_data(s).io.b_sout.ready := io.b_m(ma).ready(ds) & io.b_mnode(ma).valid(ds) & io.b_snode(s).valid(ds)
              }

              io.b_mnode(ma).ready(ds) := io.b_m(ma).ready(ds) & m_data(s).io.b_sout.valid & io.b_snode(s).valid(ds)
              io.b_snode(s).ready(ds) := io.b_m(ma).ready(ds) & m_data(s).io.b_sout.valid & io.b_mnode(ma).valid(ds)
            }
          }
        }
      } 
    }    

  // ******************************
  //            NORMAL
  // ******************************
  } else {
    // ------------------------------
    //            DEFAULT
    // ------------------------------
    for (ma <- 0 until p.nMaster) {
      io.b_m(ma).valid := false.B
      if (p.useDome) io.b_m(ma).dome.get := io.b_s(0).dome.get
      io.b_m(ma).data := io.b_s(0).data

      io.b_mnode(ma).ready(0) := false.B        
    }

    for (s <- 0 until p.nSlave) {
      io.b_s(s).ready(0) := false.B   

      io.b_snode(s).ready(0) := false.B
    }

    // ------------------------------
    //             NODE
    // ------------------------------
    for (ma <- 0 until p.nMaster) {
      w_mnode(ma) := io.b_mnode(ma).ctrl.get(0).node
    }

    for (s <- 0 until p.nSlave) {
      w_snode(s) := io.b_snode(s).ctrl.get(0).node
    }

    // ------------------------------
    //            CONNECT
    // ------------------------------
    for (ma <- 0 until p.nMaster) {
      when (io.b_mnode(ma).ctrl.get(0).r) {
        // Default if no mem
        if (p.useMem) {
          when (io.b_mnode(ma).ctrl.get(0).zero) {
            io.b_m(ma).valid := io.b_mnode(ma).valid(0)
            io.b_mnode(ma).ready(0) := io.b_m(ma).ready(0)
          }
        }

        // Normal
        for (s <- 0 until p.nSlave) {
          when ((ma.U === w_snode(s)) & (s.U === w_mnode(ma)) & ~io.b_mnode(ma).ctrl.get(0).zero) {
            when (io.b_snode(s).valid(0) & io.b_mnode(ma).valid(0)) {
              io.b_m(ma).valid := io.b_s(s).valid
              if (p.useDomeTag) io.b_m(ma).dome.get := io.b_s(s).dome.get
              io.b_m(ma).data := io.b_s(s).data

              io.b_s(s).ready(0) := io.b_m(ma).ready(0)
            }
            io.b_mnode(ma).ready(0) := io.b_m(ma).ready(0) & io.b_s(s).valid & io.b_snode(s).valid(0)
            io.b_snode(s).ready(0) := io.b_m(ma).ready(0) & io.b_s(s).valid & io.b_mnode(ma).valid(0)
          }            
        }
      } 
    }    
  }

  // ******************************
  //             DEBUG
  // ******************************
  if (p.debug) {
    dontTouch(io.b_mnode)  
    dontTouch(io.b_snode)    
  } 
}

class Mb4sCrossbar (p: Mb4sCrossbarParams) extends Module {
  val io = IO(new Bundle {   
    val b_dome = if (p.useDome) Some(Vec(p.nDome, new DomeIO(p.nAddrBit, p.nDataBit))) else None

    val i_slct_req = if (p.useDomeSlct) Some(Input(new SlctBus(p.nDome, p.nPart, 1))) else None
    val i_slct_write = if (p.useDomeSlct) Some(Input(new SlctBus(p.nDome, p.nPart, 1))) else None
    val i_slct_read = if (p.useDomeSlct) Some(Input(new SlctBus(p.nDome, p.nPart, 1))) else None

    val b_m = MixedVec(
      for (ma <- p.pMaster) yield {
        val port = Flipped(new Mb4sIO(ma))
        port
      }
    )
    val b_s = Vec(p.nSlave, new Mb4sIO(p.pSlave))
  })

  val m_req = Module(new Mb4sCrossbarReq(p))
  val m_mnode = Seq.fill(p.nMaster){Module(new GenDFifo(p, new Mb4sNodeBus(p.nSlave), UInt(0.W), 3, p.nDepth, 1, 1))}
  val m_snode = Seq.fill(p.nSlave){Module(new GenDFifo(p, new Mb4sNodeBus(p.nMaster), UInt(0.W), 3, p.nDepth, 1, 1))}
  val m_write = Module(new Mb4sCrossbarWrite(p))
  val m_read = Module(new Mb4sCrossbarRead(p))  

  val init_mdone = Wire(Vec(p.nMaster, Vec(p.nDomeSlct, Vec(2, Bool()))))
  val init_sdone = Wire(Vec(p.nSlave, Vec(p.nDomeSlct, Vec(2, Bool()))))

  for (ds <- 0 until p.nDomeSlct) {
    for (ma <- 0 until p.nMaster) {
      init_mdone(ma)(ds)(0) := 0.B
      init_mdone(ma)(ds)(1) := 0.B
    }
    for (s <- 0 until p.nSlave) {
      init_sdone(s)(ds)(0) := 0.B
      init_sdone(s)(ds)(1) := 0.B
    }
  }


  val r_mdone = RegInit(init_mdone)
  val r_sdone = RegInit(init_sdone)
  
  // ******************************
  //            MASTER
  // ******************************
  for (ma <- 0 until p.nMaster) {
    m_req.io.b_m(ma) <> io.b_m(ma).req
    m_write.io.b_m(ma) <> io.b_m(ma).write
    m_read.io.b_m(ma) <> io.b_m(ma).read
  }

  // ******************************
  //             SLAVE
  // ******************************
  for (s <- 0 until p.nSlave) {
    m_req.io.b_s(s) <> io.b_s(s).req
    m_write.io.b_s(s) <> io.b_s(s).write
    m_read.io.b_s(s) <> io.b_s(s).read
  }

  // ******************************
  //             NODE
  // ******************************
  // ------------------------------
  //            MASTER
  // ------------------------------
  for (ma <- 0 until p.nMaster) {
    for (ds <- 0 until p.nDomeSlct) {
      m_mnode(ma).io.i_flush(ds) := false.B
    }

    m_mnode(ma).io.b_din(0) <> m_req.io.b_mnode(ma)
    m_write.io.b_mnode(ma) <> m_mnode(ma).io.b_dout(0)
    m_read.io.b_mnode(ma) <> m_mnode(ma).io.b_dout(0)

    for (ds <- 0 until p.nDomeSlct) {
      m_write.io.b_mnode(ma).valid(ds) := m_mnode(ma).io.b_dout(0).valid(ds) & m_mnode(ma).io.b_dout(0).ctrl.get(ds).w & ~r_mdone(ma)(ds)(0)
      m_read.io.b_mnode(ma).valid(ds) := m_mnode(ma).io.b_dout(0).valid(ds) & m_mnode(ma).io.b_dout(0).ctrl.get(ds).r & ~r_mdone(ma)(ds)(1)
      m_mnode(ma).io.b_dout(0).ready(ds) := (~m_mnode(ma).io.b_dout(0).ctrl.get(ds).w | r_mdone(ma)(ds)(0) | m_write.io.b_mnode(ma).ready(ds)) & (~m_mnode(ma).io.b_dout(0).ctrl.get(ds).r | r_mdone(ma)(ds)(1) | m_read.io.b_mnode(ma).ready(ds))

      when (m_mnode(ma).io.b_dout(0).valid(ds) & m_mnode(ma).io.b_dout(0).ctrl.get(ds).a) {
        when (r_mdone(ma)(ds)(1) | m_read.io.b_mnode(ma).ready(ds)) {
          r_mdone(ma)(ds)(0) := false.B
        }.otherwise {
          r_mdone(ma)(ds)(0) := r_mdone(ma)(ds)(0) | m_write.io.b_mnode(ma).ready(ds)
        }
        when (r_mdone(ma)(ds)(0) | m_write.io.b_mnode(ma).ready(ds)) {
          r_mdone(ma)(ds)(1) := false.B
        }.otherwise {
          r_mdone(ma)(ds)(1) := r_mdone(ma)(ds)(1) | m_read.io.b_mnode(ma).ready(ds)
        }
      }.otherwise {
        r_mdone(ma)(ds)(0) := false.B
        r_mdone(ma)(ds)(1) := false.B
      }
    } 
  }

  // ------------------------------
  //             SLAVE
  // ------------------------------
  for (s <- 0 until p.nSlave) {
    for (ds <- 0 until p.nDomeSlct) {
      m_snode(s).io.i_flush(ds) := false.B
    }

    m_snode(s).io.b_din(0) <> m_req.io.b_snode(s)
    m_write.io.b_snode(s) <> m_snode(s).io.b_dout(0)
    m_read.io.b_snode(s) <> m_snode(s).io.b_dout(0)

    for (ds <- 0 until p.nDomeSlct) {
      m_write.io.b_snode(s).valid(ds) := m_snode(s).io.b_dout(0).valid(ds) & m_snode(s).io.b_dout(0).ctrl.get(ds).w & ~r_sdone(s)(ds)(0)
      m_read.io.b_snode(s).valid(ds) := m_snode(s).io.b_dout(0).valid(ds) & m_snode(s).io.b_dout(0).ctrl.get(ds).r & ~r_sdone(s)(ds)(1)
      m_snode(s).io.b_dout(0).ready(ds) := (~m_snode(s).io.b_dout(0).ctrl.get(ds).w | r_sdone(s)(ds)(0) | m_write.io.b_snode(s).ready(ds)) & (~m_snode(s).io.b_dout(0).ctrl.get(ds).r | r_sdone(s)(ds)(1) | m_read.io.b_snode(s).ready(ds))

      when (m_snode(s).io.b_dout(0).valid(ds) & m_snode(s).io.b_dout(0).ctrl.get(ds).a) {
        when (r_sdone(s)(ds)(1) | m_read.io.b_snode(s).ready(ds)) {
          r_sdone(s)(ds)(0) := false.B
        }.otherwise {
          r_sdone(s)(ds)(0) := r_sdone(s)(ds)(0) | m_write.io.b_snode(s).ready(ds)
        }
        
        when (r_sdone(s)(ds)(0) | m_write.io.b_snode(s).ready(ds)) {
          r_sdone(s)(ds)(1) := false.B
        }.otherwise {
          r_sdone(s)(ds)(1) := r_sdone(s)(ds)(1) | m_read.io.b_snode(s).ready(ds)
        }
      }.otherwise {
        r_sdone(s)(ds)(0) := false.B
        r_sdone(s)(ds)(1) := false.B
      }
    } 
  }

  // ******************************
  //          SELECT DOME
  // ******************************
  if (p.useDomeSlct) {
    m_req.io.i_slct.get := io.i_slct_req.get
    m_write.io.i_slct.get := io.i_slct_write.get  
    m_read.io.i_slct.get := io.i_slct_read.get     
  }

  if (p.useDome) {
    for (d <- 0 until p.nDome) {
      io.b_dome.get(d).free := true.B
    }
  }
  
  // ******************************
  //          DIRECT CONNECT
  // ******************************
  if (p.useDirect && !p.useMem && (p.nMaster == p.nSlave)) {
    for (s <- 0 until p.nSlave) {
      io.b_m(s) <> io.b_s(s)
    }    
  }

  // ******************************
  //             DEBUG
  // ******************************
  if (p.debug) {
    
  } 
}

object Mb4sCrossbarReq extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Mb4sCrossbarReq(Mb4sCrossbarConfigBase), args)
}

object Mb4sCrossbarWrite extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Mb4sCrossbarWrite(Mb4sCrossbarConfigBase), args)
}

object Mb4sCrossbarRead extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Mb4sCrossbarRead(Mb4sCrossbarConfigBase), args)
}

object Mb4sCrossbar extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Mb4sCrossbar(Mb4sCrossbarConfigBase), args)
}