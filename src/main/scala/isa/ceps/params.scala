/*
 * File: params.scala                                                          *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:26:39 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */

 
package herd.common.isa.ceps

import chisel3._
import chisel3.util._


trait DomeCfgParams {
  def nDataBit: Int
  def nTrapLvl: Int
  def useRange: Boolean
  def useFr: Boolean

  def nRange: Int = {
    if ((nDataBit % log2Ceil(nDataBit)) != 0) {
      return (nDataBit / log2Ceil(nDataBit)) + 1
    } else {
      return (nDataBit / log2Ceil(nDataBit))
    }
  }
}

case class DomeCfgConfig (
  nDataBit: Int,
  nTrapLvl: Int,
  useRange: Boolean,
  useFr: Boolean
) extends DomeCfgParams
