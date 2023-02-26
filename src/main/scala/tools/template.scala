/*
 * File: template.scala                                                        *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:26:49 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.tools

import chisel3._
import chisel3.util._
import scala.math._


class TemplateRVBus extends Bundle {
  val ready = Input(Bool())
  val valid = Output(Bool())
}

class TemplateBus extends TemplateRVBus {
  val in = Input(UInt(8.W))
  val out = Output(UInt(8.W))
}

class Template extends Module {

  val io = IO(new Bundle {

  })

}

object Template extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Template(), args)
}