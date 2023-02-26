/*
 * File: fifo.scala                                                            *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:25:49 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.gen

import chisel3._
import chisel3.util._

import herd.common.dome._


class GenFifo[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD, nSeqLvl: Int, depth: Int, nInPort: Int, nOutPort: Int) extends Module {
  // ******************************
  //             I/Os
  // ******************************
  val io = IO(new Bundle {
    val i_flush = Input(Bool())

    val b_in = Vec(nInPort, Flipped(new GenRVIO(p, tc, td)))

    val o_pt = Output(UInt((log2Ceil(depth + 1)).W))
    val o_val = Output(Vec(depth, new GenVBus(p, tc, td)))
    
    val b_out = Vec(nOutPort, new GenRVIO(p, tc, td))
  })

  val r_pt = RegInit(0.U((log2Ceil(depth + 1)).W))
  val r_fifo = Reg(Vec(depth, new GenBus(p, tc, td)))
  
  val w_out_pt = Wire(Vec(nOutPort + 1, UInt((log2Ceil(depth + 1)).W)))
  val w_out_in_pt = Wire(Vec(nOutPort + 1, UInt((log2Ceil(nInPort + 1)).W)))

  val w_in_pt = Wire(Vec(nInPort + 1, UInt((log2Ceil(depth + 1)).W)))
  val w_full_pt = Wire(Vec(nInPort + 1, UInt((log2Ceil(depth + 1)).W)))

  // ******************************
  //              READ
  // ******************************
  w_out_pt(0) := 0.U
  w_out_in_pt(0) := nInPort.U
  if (nSeqLvl <= 0) {
    for (i <- 0 until nInPort) {
      when (io.b_in(nInPort - 1 - i).valid) {
        w_out_in_pt(0) := (nInPort - 1 - i).U
      }
    }
  }

  // ------------------------------
  //       SUPPORT CROSS READ
  // ------------------------------
  if (nSeqLvl <= 0) {
    for (o <- 0 until nOutPort) {
      io.b_out(o).valid := (w_out_pt(o) < r_pt) | (w_out_in_pt(o) < nInPort.U)
      if (tc.getWidth > 0)  io.b_out(o).ctrl.get := r_fifo(0).ctrl.get
      if (td.getWidth > 0)  io.b_out(o).data.get := r_fifo(0).data.get
      w_out_pt(o + 1) := w_out_pt(o)
      w_out_in_pt(o + 1) := w_out_in_pt(o)

      for (d <- 0 until depth) {
        when((w_out_pt(o) < r_pt) & (d.U === w_out_pt(o))) {
          if (tc.getWidth > 0)  io.b_out(o).ctrl.get := r_fifo(d).ctrl.get
          if (td.getWidth > 0)  io.b_out(o).data.get := r_fifo(d).data.get
        }
      }

      for (i <- 0 until nInPort) {
        when((w_out_pt(o) >= r_pt) & (w_out_in_pt(o) < nInPort.U) & i.U === w_out_in_pt(o)) {
          if (tc.getWidth > 0)  io.b_out(o).ctrl.get := io.b_in(i).ctrl.get
          if (td.getWidth > 0)  io.b_out(o).data.get := io.b_in(i).data.get
        }
      }

      when(io.b_out(o).ready & (w_out_pt(o) < r_pt)) {
        w_out_pt(o + 1) := w_out_pt(o) + 1.U
      }

      when(io.b_out(o).ready & (w_out_pt(o) >= r_pt) & (w_out_in_pt(o) < nInPort.U)) {
        w_out_in_pt(o + 1) := nInPort.U
        for (i <- 0 until nInPort) {
          when (io.b_in(nInPort - 1 - i).valid & ((nInPort - 1 - i).U > w_out_in_pt(o))) {
            w_out_in_pt(o + 1) := (nInPort - 1 - i).U
          }
        }
      }
    }

  // ------------------------------
  //         NO CROSS READ
  // ------------------------------
  } else {
    for (o <- 0 until nOutPort) {
      if (nSeqLvl >= 5) {
        io.b_out(o).valid := (o.U < r_pt)
      } else {
        io.b_out(o).valid := (w_out_pt(o) < r_pt)
      }      
      if (tc.getWidth > 0)  io.b_out(o).ctrl.get := r_fifo(0).ctrl.get
      if (td.getWidth > 0)  io.b_out(o).data.get := r_fifo(0).data.get
      w_out_pt(o + 1) := w_out_pt(o)
      w_out_in_pt(o + 1) := w_out_in_pt(o)

      for (d <- 0 until depth) {
        when(d.U === w_out_pt(o)) {
          if (tc.getWidth > 0)  io.b_out(o).ctrl.get := r_fifo(d).ctrl.get
          if (td.getWidth > 0)  io.b_out(o).data.get := r_fifo(d).data.get
        }
      }

      when(io.b_out(o).ready & (w_out_pt(o) < r_pt)) {
        w_out_pt(o + 1) := w_out_pt(o) + 1.U
      }
    }
  }

  // ******************************
  //             SHIFT
  // ******************************
  for (o <- 0 to nOutPort) {
    when (o.U === w_out_pt(nOutPort)) {
      for (s <- 0 until depth - o) {
        r_fifo(s) := r_fifo(s + o)
      }
    }
  }

  // ******************************
  //             WRITE
  // ******************************
  w_in_pt(0) := r_pt - w_out_pt(nOutPort)
  if (nSeqLvl <= 1) {
    w_full_pt(0) := r_pt - w_out_pt(nOutPort)
  } else if ((nSeqLvl == 2) || (nSeqLvl == 3)) {
    w_full_pt(0) := r_pt
  } else {
    when((depth.U - r_pt) >= nInPort.U) {
      w_full_pt(0) := r_pt
    }.otherwise {
      w_full_pt(0) := depth.U
    }
  }

  for (i <- 0 until nInPort) {
    w_in_pt(i + 1) := w_in_pt(i)

    if (nSeqLvl <= 0) {
      io.b_in(i).ready := (w_full_pt(i) < depth.U) | (w_out_in_pt(nOutPort) > i.U)

      w_full_pt(i + 1) := w_full_pt(i)

      when(io.b_in(i).valid & (w_full_pt(i) < depth.U) & (w_out_in_pt(nOutPort) <= i.U)) {
        for (d <- 0 until depth) {
          when(d.U === w_in_pt(i)) {
            if (tc.getWidth > 0)  r_fifo(d).ctrl.get := io.b_in(i).ctrl.get
            if (td.getWidth > 0)  r_fifo(d).data.get := io.b_in(i).data.get
          }
        }
        w_in_pt(i + 1) := w_in_pt(i) + 1.U
        w_full_pt(i + 1) := w_full_pt(i) + 1.U
      }
    } else {
      io.b_in(i).ready := (w_full_pt(i) < depth.U)

      if (nSeqLvl == 3) {
        w_full_pt(i + 1) := w_full_pt(i) + 1.U
      } else if (nSeqLvl >= 4) {
        w_full_pt(i + 1) := w_full_pt(0)
      } else {
        w_full_pt(i + 1) := w_full_pt(i)
      }

      when(io.b_in(i).valid & (w_full_pt(i) < depth.U)) {
        for (d <- 0 until depth) {
          when(d.U === w_in_pt(i)) {
            if (tc.getWidth > 0)  r_fifo(d).ctrl.get := io.b_in(i).ctrl.get
            if (td.getWidth > 0)  r_fifo(d).data.get := io.b_in(i).data.get
          }
        }
        w_in_pt(i + 1) := w_in_pt(i) + 1.U
        if (nSeqLvl <= 2) w_full_pt(i + 1) := w_full_pt(i) + 1.U
      }
    }
  }

  // ******************************
  //           REGISTER
  // ******************************
  when(io.i_flush) {
    r_pt := 0.U
  }.otherwise {
    r_pt := w_in_pt(nInPort)
  }

  // ******************************
  //        EXTERNAL ACCESS
  // ******************************
  io.o_pt := r_pt
  for (d <- 0 until depth) {
    io.o_val(d).valid := (d.U < r_pt)
    if (tc.getWidth > 0)  io.o_val(d).ctrl.get := r_fifo(d).ctrl.get
    if (td.getWidth > 0)  io.o_val(d).data.get := r_fifo(d).data.get
  }
}

class GenDFifo[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD, nSeqLvl: Int, depth: Int, nInPort: Int, nOutPort: Int) extends Module {
  val io = IO(new Bundle {
    val i_flush = Input(Vec(p.nDomeSlct, Bool()))

    val b_din = Vec(nInPort, Flipped(new GenDRVIO(p, tc, td)))

    val o_pt = Output(Vec(p.nDomeSlct, UInt((log2Ceil(depth + 1)).W)))
    val o_val = Output(Vec(depth, new GenDVBus(p, tc, td)))
    
    val b_dout = Vec(nOutPort, new GenDRVIO(p, tc, td))
  })

  val r_pt = RegInit(VecInit(Seq.fill(p.nDomeSlct)(0.U((log2Ceil(depth + 1)).W))))
  val r_fifo = Reg(Vec(depth, new GenDBus(p, tc, td)))

  val w_out_pt = Wire(Vec(p.nDomeSlct, Vec(nOutPort + 1, UInt((log2Ceil(depth + 1)).W))))
  val w_out_in_pt = Wire(Vec(p.nDomeSlct, Vec(nOutPort + 1, UInt((log2Ceil(nInPort + 1)).W))))

  val w_in_pt = Wire(Vec(p.nDomeSlct, Vec(nInPort + 1, UInt((log2Ceil(depth + 1)).W))))
  val w_full_pt = Wire(Vec(p.nDomeSlct, Vec(nInPort + 1, UInt((log2Ceil(depth + 1)).W))))

  for (ds <- 0 until p.nDomeSlct) {
    // ******************************
    //              READ
    // ******************************
    w_out_pt(ds)(0) := 0.U
    w_out_in_pt(ds)(0) := nInPort.U
    if (nSeqLvl <= 0) {
      for (i <- 0 until nInPort) {
        when (io.b_din(nInPort - 1 - i).valid(ds)) {
          w_out_in_pt(ds)(0) := (nInPort - 1 - i).U
        }
      }
    }  

    // ------------------------------
    //       SUPPORT CROSS READ
    // ------------------------------
    if (nSeqLvl <= 0) {
      for (o <- 0 until nOutPort) {
        io.b_dout(o).valid(ds) := (w_out_pt(ds)(o) < r_pt(ds)) | (w_out_in_pt(ds)(o) < nInPort.U)
        if (p.useDomeTag)     io.b_dout(o).dome.get     := r_fifo(0).dome.get
        if (tc.getWidth > 0)  io.b_dout(o).ctrl.get(ds) := r_fifo(0).ctrl.get(ds)
        if (td.getWidth > 0)  io.b_dout(o).data.get(ds) := r_fifo(0).data.get(ds)
        w_out_pt(ds)(o + 1) := w_out_pt(ds)(o)
        w_out_in_pt(ds)(o + 1) := w_out_in_pt(ds)(o)

        for (d <- 0 until depth) {
          when ((w_out_pt(ds)(o) < r_pt(ds)) & (d.U === w_out_pt(ds)(o))) {
            if (p.useDomeTag)     io.b_dout(o).dome.get     := r_fifo(d).dome.get
            if (tc.getWidth > 0)  io.b_dout(o).ctrl.get(ds) := r_fifo(d).ctrl.get(ds)
            if (td.getWidth > 0)  io.b_dout(o).data.get(ds) := r_fifo(d).data.get(ds)
          }
        }

        for (i <- 0 until nInPort) {
          when((w_out_pt(ds)(o) >= r_pt(ds)) & (w_out_in_pt(ds)(o) < nInPort.U) & (i.U === w_out_in_pt(ds)(o))) {
            if (p.useDomeTag)     io.b_dout(o).dome.get     := io.b_din(i).dome.get
            if (tc.getWidth > 0)  io.b_dout(o).ctrl.get(ds) := io.b_din(i).ctrl.get(ds)
            if (td.getWidth > 0)  io.b_dout(o).data.get(ds) := io.b_din(i).data.get(ds)
          }
        }

        when(io.b_dout(o).ready(ds) & (w_out_pt(ds)(o) < r_pt(ds))) {
          w_out_pt(ds)(o + 1) := w_out_pt(ds)(o) + 1.U
        }

        when(io.b_dout(o).ready(ds) & (w_out_pt(ds)(o) >= r_pt(ds)) & (w_out_in_pt(ds)(o) < nInPort.U)) {
          w_out_in_pt(ds)(o + 1) := nInPort.U
          for (i <- 0 until nInPort) {
            when (io.b_din(nInPort - 1 - i).valid(ds) & ((nInPort - 1 - i).U > w_out_in_pt(ds)(o))) {
              w_out_in_pt(ds)(o + 1) := (nInPort - 1 - i).U
            }
          }
        }
      }    

    // ------------------------------
    //         NO CROSS READ
    // ------------------------------
    } else {
      for (o <- 0 until nOutPort) {
        if (nSeqLvl >= 5) {
          io.b_dout(o).valid(ds) := (o.U < r_pt(ds))
        } else {
          io.b_dout(o).valid(ds) := (w_out_pt(ds)(o) < r_pt(ds))
        } 
        if (p.useDomeTag)     io.b_dout(o).dome.get     := r_fifo(0).dome.get
        if (tc.getWidth > 0)  io.b_dout(o).ctrl.get(ds) := r_fifo(0).ctrl.get(ds)
        if (td.getWidth > 0)  io.b_dout(o).data.get(ds) := r_fifo(0).data.get(ds)
        w_out_pt(ds)(o + 1) := w_out_pt(ds)(o)
        w_out_in_pt(ds)(o + 1) := w_out_in_pt(ds)(o)

        for (d <- 0 until depth) {
          when (d.U === w_out_pt(ds)(o)) {
            if (p.useDomeTag)     io.b_dout(o).dome.get     := r_fifo(d).dome.get
            if (tc.getWidth > 0)  io.b_dout(o).ctrl.get(ds) := r_fifo(d).ctrl.get(ds)
            if (td.getWidth > 0)  io.b_dout(o).data.get(ds) := r_fifo(d).data.get(ds)
          }
        }

        when(io.b_dout(o).ready(ds) & (w_out_pt(ds)(o) < r_pt(ds))) {
          w_out_pt(ds)(o + 1) := w_out_pt(ds)(o) + 1.U
        }
      }      
    }

    // ******************************
    //             SHIFT
    // ******************************
    for (o <- 0 to nOutPort) {
      when (o.U === w_out_pt(ds)(nOutPort)) {
        for (s <- 0 until depth - o) {
          if (p.useDomeTag)     r_fifo(s).dome.get     := r_fifo(s + o).dome.get
          if (tc.getWidth > 0)  r_fifo(s).ctrl.get(ds) := r_fifo(s + o).ctrl.get(ds)
          if (td.getWidth > 0)  r_fifo(s).data.get(ds) := r_fifo(s + o).data.get(ds)          
        }
      }
    }  

    // ******************************
    //             WRITE
    // ******************************
    w_in_pt(ds)(0) := r_pt(ds) - w_out_pt(ds)(nOutPort)
    if (nSeqLvl <= 1) {
      w_full_pt(ds)(0) := r_pt(ds) - w_out_pt(ds)(nOutPort)
    } else if ((nSeqLvl == 2) || (nSeqLvl == 3)) {
      w_full_pt(ds)(0) := r_pt(ds)
    } else {
      when((depth.U - r_pt(ds)) >= nInPort.U) {
        w_full_pt(ds)(0) := r_pt(ds)
      }.otherwise {
        w_full_pt(ds)(0) := depth.U
      }
    }

    for (i <- 0 until nInPort) {
      w_in_pt(ds)(i + 1) := w_in_pt(ds)(i)

      if (nSeqLvl <= 0) {
        io.b_din(i).ready(ds) := (w_full_pt(ds)(i) < depth.U) | (w_out_in_pt(ds)(nOutPort) > i.U)

        w_full_pt(ds)(i + 1) := w_full_pt(ds)(i)

        when (io.b_din(i).valid(ds) & (w_full_pt(ds)(i) < depth.U) & (w_out_in_pt(ds)(nOutPort) <= i.U)) {
          for (d <- 0 until depth) {
            when (d.U === w_in_pt(ds)(i)) {
              if (p.useDomeTag)     r_fifo(d).dome.get := io.b_din(i).dome.get
              if (tc.getWidth > 0)  r_fifo(d).ctrl.get(ds) := io.b_din(i).ctrl.get(ds)
              if (td.getWidth > 0)  r_fifo(d).data.get(ds) := io.b_din(i).data.get(ds)
            }
          }
          w_in_pt(ds)(i + 1) := w_in_pt(ds)(i) + 1.U
          w_full_pt(ds)(i + 1) := w_full_pt(ds)(i) + 1.U
        }
      } else {
        io.b_din(i).ready(ds) := (w_full_pt(ds)(i) < depth.U)

        if (nSeqLvl == 3) {
          w_full_pt(ds)(i + 1) := w_full_pt(ds)(i) + 1.U
        } else if (nSeqLvl >= 4) {
          w_full_pt(ds)(i + 1) := w_full_pt(ds)(0)
        } else {
          w_full_pt(ds)(i + 1) := w_full_pt(ds)(i)
        }

        when (io.b_din(i).valid(ds) & (w_full_pt(ds)(i) < depth.U)) {
          for (d <- 0 until depth) {
            when (d.U === w_in_pt(ds)(i)) {
              if (p.useDomeTag)     r_fifo(d).dome.get := io.b_din(i).dome.get
              if (tc.getWidth > 0)  r_fifo(d).ctrl.get(ds) := io.b_din(i).ctrl.get(ds)
              if (td.getWidth > 0)  r_fifo(d).data.get(ds) := io.b_din(i).data.get(ds)
            }
          }
          w_in_pt(ds)(i + 1) := w_in_pt(ds)(i) + 1.U
          if (nSeqLvl <= 2) w_full_pt(ds)(i + 1) := w_full_pt(ds)(i) + 1.U
        }
      }
    }  

    // ******************************
    //           REGISTER
    // ******************************
    when (io.i_flush(ds)) {
      r_pt(ds) := 0.U
    }.otherwise {
      r_pt(ds) := w_in_pt(ds)(nInPort)
    } 

    // ******************************
    //        EXTERNAL ACCESS
    // ******************************
    io.o_pt(ds) := r_pt(ds)
    for (d <- 0 until depth) {
      io.o_val(d).valid(ds) := (d.U < r_pt(ds))
      if (p.useDomeTag)     io.o_val(d).dome.get := r_fifo(d).dome.get
      if (tc.getWidth > 0)  io.o_val(d).ctrl.get(ds) := r_fifo(d).ctrl.get(ds)
      if (td.getWidth > 0)  io.o_val(d).data.get(ds) := r_fifo(d).data.get(ds)
    }
  }  
}

class GenSFifo[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD, nSeqLvl: Int, depth: Int, nInPort: Int, nOutPort: Int) extends Module {
  val io = IO(new Bundle {
    val i_flush = Input(Vec(p.nDomeSlct, Bool()))

    val i_slct_in = if (p.useDomeSlct) Some(Input(Vec(nInPort, new SlctBus(p.nDome, p.nPart, 1)))) else None
    val b_sin = Vec(nInPort, Flipped(new GenSRVIO(p, tc, td)))

    val o_pt = Output(Vec(p.nDomeSlct, UInt((log2Ceil(depth + 1)).W)))
    val o_val = Output(Vec(depth, new GenDVBus(p, tc, td)))
    
    val i_slct_out = if (p.useDomeSlct) Some(Input(new SlctBus(p.nDome, p.nPart, 1))) else None
    val b_sout = Vec(nOutPort, new GenSRVIO(p, tc, td))
  })

  val m_in = Seq.fill(nInPort){Module(new GenSDemux(p, tc, td))}
  val m_fifo = Module(new GenDFifo(p, tc, td, nSeqLvl, depth, nInPort, nOutPort))
  val m_out = Seq.fill(nInPort){Module(new GenSMux(p, tc, td))}

  m_fifo.io.i_flush := io.i_flush

  // Inputs
  for (i <- 0 until nInPort) {
    if (p.useDomeSlct) m_in(i).io.i_slct.get := io.i_slct_in.get(i)
    m_in(i).io.b_sin <> io.b_sin(i)
  }

  // FIFO
  for (i <- 0 until nInPort) {
    m_fifo.io.b_din(i) <> m_in(i).io.b_dout
  }
  io.o_pt := m_fifo.io.o_pt
  io.o_val := m_fifo.io.o_val

  // Outputs
  for (o <- 0 until nOutPort) {
    if (p.useDomeSlct) m_out(o).io.i_slct.get := io.i_slct_out.get
    m_out(o).io.b_din <> m_fifo.io.b_dout(o)
    io.b_sout(o) <> m_out(o).io.b_sout
  }
}

object GenFifo extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new GenFifo(GenConfigBase, UInt(4.W), UInt(8.W), 2, 4, 2, 2), args)
}

object GenDFifo extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new GenDFifo(GenConfigBase, UInt(4.W), UInt(8.W), 2, 4, 2, 2), args)
}

object GenSFifo extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new GenSFifo(GenConfigBase, UInt(4.W), UInt(8.W), 2, 4, 2, 2), args)
}
