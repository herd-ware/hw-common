/*
 * File: configs.scala                                                         *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:22:22 pm                                       *
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


object FieldConfigBase extends FieldConfig (
  useField = true,
  nField = 2,
  multiField = true,
  nPart = 2
)