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

import herd.common.field.{SlctBus}


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
//            FIELD
// ------------------------------

// ------------------------------
//         FIELD SELECT
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
//            FIELD
// ------------------------------
class FlatDIO (useFieldTag: Boolean, nField: Int, nFieldSlct: Int) extends Bundle {
  val field = if (useFieldTag) Some(UInt(log2Ceil(nField).W)) else None
}

class FlatDVIO (useFieldTag: Boolean, nField: Int, nFieldSlct: Int) extends FlatDIO(useFieldTag, nField, nFieldSlct) {
  val valid = Output(Vec(nFieldSlct, Bool()))
}

class FlatDRVIO (useFieldTag: Boolean, nField: Int, nFieldSlct: Int) extends FlatDVIO(useFieldTag, nField, nFieldSlct) {
  val ready = Input(Vec(nFieldSlct, Bool()))
}

// ------------------------------
//         FIELD SELECT
// ------------------------------
class FlatSIO (useFieldTag: Boolean, nField: Int) extends Bundle {
  val field = if (useFieldTag) Some(Output(UInt(log2Ceil(nField).W))) else None  
}

class FlatSVIO (useFieldTag: Boolean, nField: Int) extends FlatVIO {
  val field = if (useFieldTag) Some(Output(UInt(log2Ceil(nField).W))) else None  
}

class FlatSRVIO (useFieldTag: Boolean, nField: Int) extends FlatRVIO {
  val field = if (useFieldTag) Some(Output(UInt(log2Ceil(nField).W))) else None  
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
//            FIELD
// ------------------------------
class GenDBus[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends Bundle {
  val field = if (p.useFieldTag) Some(UInt(log2Ceil(p.nField).W)) else None
  val ctrl = if (tc.getWidth > 0) Some(Vec(p.nFieldSlct, tc)) else None
  val data = if (td.getWidth > 0) Some(Vec(p.nFieldSlct, td)) else None
}

class GenDVBus[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends GenDBus[TC, TD](p, tc, td) {
  val valid = Vec(p.nFieldSlct, Bool())
}

class GenDRVBus[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends GenDVBus[TC, TD](p, tc, td) {
  val ready = Vec(p.nFieldSlct, Bool())
}

// ------------------------------
//         FIELD SELECT
// ------------------------------
class GenSBus[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends GenBus[TC, TD](p, tc, td) {
  val field = if (p.useField) Some(UInt(log2Ceil(p.nField).W)) else None
}

class GenSVBus[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends GenVBus[TC, TD](p, tc, td) {
  val field = if (p.useField) Some(UInt(log2Ceil(p.nField).W)) else None
}

class GenSRVBus[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends GenRVBus[TC, TD](p, tc, td) {
  val field = if (p.useField) Some(UInt(log2Ceil(p.nField).W)) else None
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
//            FIELD
// ------------------------------
class GenDIO[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends Bundle {
  val field = if (p.useFieldTag) Some(Output(UInt(log2Ceil(p.nField).W))) else None  
  val ctrl = if (tc.getWidth > 0) Some(Output(Vec(p.nFieldSlct, tc))) else None
  val data = if (td.getWidth > 0) Some(Output(Vec(p.nFieldSlct, td))) else None
}

class GenDVIO[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends GenDIO[TC, TD](p, tc, td) {
  val valid = Output(Vec(p.nFieldSlct, Bool()))
}

class GenDRVIO[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends GenDVIO[TC, TD](p, tc, td) {
  val ready = Input(Vec(p.nFieldSlct, Bool()))
}

// ------------------------------
//         FIELD SELECT
// ------------------------------
class GenSIO[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends GenIO[TC, TD](p, tc, td) {
  val field = if (p.useField) Some(Output(UInt(log2Ceil(p.nField).W))) else None
}

class GenSVIO[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends GenVIO[TC, TD](p, tc, td) {
  val field = if (p.useField) Some(Output(UInt(log2Ceil(p.nField).W))) else None
}

class GenSRVIO[TC <: Data, TD <: Data](p: GenParams, tc: TC, td: TD) extends GenRVIO[TC, TD](p, tc, td) {
  val field = if (p.useField) Some(Output(UInt(log2Ceil(p.nField).W))) else None
}