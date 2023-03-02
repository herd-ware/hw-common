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


package herd.common.field

import chisel3._
import chisel3.util._


trait FieldParams {
  def useField: Boolean
  def nField: Int
  def multiField: Boolean
  def useFieldTag: Boolean = useField && !multiField
  def useFieldSlct: Boolean = useField && multiField
  def nFieldTag: Int = {
    if (useField) {
      return nField
    } else {
      return 1
    }
  }
  def nFieldSlct: Int = {
    if (useFieldSlct) {
      return nField
    } else {
      return 1
    }
  }
  def nPart: Int
}

case class FieldConfig (
  useField: Boolean,
  nField: Int,
  multiField: Boolean,
  nPart: Int
) extends FieldParams
