/*
 * File: bus.scala                                                             *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:25:09 pm                                       *
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

import herd.common.dome.{SlctBus}


// ******************************
//            FLAT BUS
// ******************************
// ------------------------------
//            SIMPLE
// ------------------------------
class FlatVBus extends Bundle {
  val valid = Bool()
}

class FlatRVBus extends FlatVBus {
  val ready = Bool()
}

// ------------------------------
//             DOME
// ------------------------------

// ------------------------------
//          DOME SELECT
// ------------------------------

// ******************************
//            FLAT IO
// ******************************
// ------------------------------
//            SIMPLE
// ------------------------------
class FlatVIO extends Bundle {
  val valid = Output(Bool())  
}

class FlatRVIO extends FlatVIO {
  val ready = Input(Bool())
}

// ------------------------------
//             DOME
// ------------------------------
class FlatDIO (useDomeTag: Boolean, nDome: Int, nDomeSlct: Int) extends Bundle {
  val dome = if (useDomeTag) Some(UInt(log2Ceil(nDome).W)) else None
}

class FlatDVIO (useDomeTag: Boolean, nDome: Int, nDomeSlct: Int) extends FlatDIO(useDomeTag, nDome, nDomeSlct) {
  val valid = Output(Vec(nDomeSlct, Bool()))
}

class FlatDRVIO (useDomeTag: Boolean, nDome: Int, nDomeSlct: Int) extends FlatDVIO(useDomeTag, nDome, nDomeSlct) {
  val ready = Input(Vec(nDomeSlct, Bool()))
}

// ------------------------------
//          DOME SELECT
// ------------------------------
class FlatSIO (useDomeTag: Boolean, nDome: Int) extends Bundle {
  val dome = if (useDomeTag) Some(Output(UInt(log2Ceil(nDome).W))) else None  
}

class FlatSVIO (useDomeTag: Boolean, nDome: Int) extends FlatVIO {
  val dome = if (useDomeTag) Some(Output(UInt(log2Ceil(nDome).W))) else None  
}

class FlatSRVIO (useDomeTag: Boolean, nDome: Int) extends FlatRVIO {
  val dome = if (useDomeTag) Some(Output(UInt(log2Ceil(nDome).W))) else None  
}

// ******************************
//          GENERIC BUS
// ******************************
// ------------------------------
//            SIMPLE
// ------------------------------
class GenBus[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends Bundle {
  val ctrl = if (tc.getWidth > 0) Some(tc) else None
  val data = if (td.getWidth > 0) Some(td) else None
}

class GenVBus[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends GenBus[TC, TD](p, tc, td) {
  val valid = Bool()
}

class GenRVBus[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends GenVBus[TC, TD](p, tc, td) {
  val ready = Bool()
}

// ------------------------------
//             DOME
// ------------------------------
class GenDBus[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends Bundle {
  val dome = if (p.useDomeTag) Some(UInt(log2Ceil(p.nDome).W)) else None
  val ctrl = if (tc.getWidth > 0) Some(Vec(p.nDomeSlct, tc)) else None
  val data = if (td.getWidth > 0) Some(Vec(p.nDomeSlct, td)) else None
}

class GenDVBus[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends GenDBus[TC, TD](p, tc, td) {
  val valid = Vec(p.nDomeSlct, Bool())
}

class GenDRVBus[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends GenDVBus[TC, TD](p, tc, td) {
  val ready = Vec(p.nDomeSlct, Bool())
}

// ------------------------------
//          DOME SELECT
// ------------------------------
class GenSBus[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends GenBus[TC, TD](p, tc, td) {
  val dome = if (p.useDome) Some(UInt(log2Ceil(p.nDome).W)) else None
}

class GenSVBus[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends GenVBus[TC, TD](p, tc, td) {
  val dome = if (p.useDome) Some(UInt(log2Ceil(p.nDome).W)) else None
}

class GenSRVBus[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends GenRVBus[TC, TD](p, tc, td) {
  val dome = if (p.useDome) Some(UInt(log2Ceil(p.nDome).W)) else None
}

// ******************************
//           GENERIC IO
// ******************************
// ------------------------------
//            SIMPLE
// ------------------------------
class GenIO[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends Bundle {
  val ctrl = if (tc.getWidth > 0) Some(Output(tc)) else None
  val data = if (td.getWidth > 0) Some(Output(td)) else None
}
class GenVIO[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends GenIO[TC, TD](p, tc, td) {
  val valid = Output(Bool())
}

class GenRVIO[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends GenVIO[TC, TD](p, tc, td) {
  val ready = Input(Bool())
}

// ------------------------------
//             DOME
// ------------------------------
class GenDIO[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends Bundle {
  val dome = if (p.useDomeTag) Some(Output(UInt(log2Ceil(p.nDome).W))) else None  
  val ctrl = if (tc.getWidth > 0) Some(Output(Vec(p.nDomeSlct, tc))) else None
  val data = if (td.getWidth > 0) Some(Output(Vec(p.nDomeSlct, td))) else None
}

class GenDVIO[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends GenDIO[TC, TD](p, tc, td) {
  val valid = Output(Vec(p.nDomeSlct, Bool()))
}

class GenDRVIO[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends GenDVIO[TC, TD](p, tc, td) {
  val ready = Input(Vec(p.nDomeSlct, Bool()))
}

// ------------------------------
//          DOME SELECT
// ------------------------------
class GenSIO[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends GenIO[TC, TD](p, tc, td) {
  val dome = if (p.useDome) Some(Output(UInt(log2Ceil(p.nDome).W))) else None
}

class GenSVIO[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends GenVIO[TC, TD](p, tc, td) {
  val dome = if (p.useDome) Some(Output(UInt(log2Ceil(p.nDome).W))) else None
}

class GenSRVIO[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends GenRVIO[TC, TD](p, tc, td) {
  val dome = if (p.useDome) Some(Output(UInt(log2Ceil(p.nDome).W))) else None
}