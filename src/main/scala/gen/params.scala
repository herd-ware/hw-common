/*
 * File: params.scala                                                          *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:25:53 pm                                       *
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
import scala.math._

import herd.common.dome._


trait GenParams extends DomeParams {
  def debug: Boolean

  def useDome: Boolean
  def nDome: Int
  def multiDome: Boolean
  def nPart: Int
}

case class GenConfig (
  debug: Boolean,

  useDome: Boolean,
  nDome: Int,
  multiDome: Boolean,
  nPart: Int
) extends GenParams