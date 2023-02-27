/*
 * File: params.scala                                                          *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:22:26 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.dome

import chisel3._
import chisel3.util._


trait DomeParams {
  def useDome: Boolean
  def nDome: Int
  def multiDome: Boolean
  def useDomeTag: Boolean = useDome && !multiDome
  def useDomeSlct: Boolean = useDome && multiDome
  def nDomeTag: Int = {
    if (useDome) {
      return nDome
    } else {
      return 1
    }
  }
  def nDomeSlct: Int = {
    if (useDomeSlct) {
      return nDome
    } else {
      return 1
    }
  }
  def nPart: Int
}

case class DomeConfig (
  useDome: Boolean,
  nDome: Int,
  multiDome: Boolean,
  nPart: Int
) extends DomeParams
