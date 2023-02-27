/*
 * File: consts.scala                                                          *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:22:25 pm                                       *
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


object SLCT {
  def ZERO: SlctBus = {
    val w_zero = Wire(new SlctBus(1, 1, 1))

    w_zero.dome := 0.U
    w_zero.next := 0.U
    w_zero.step := 0.U

    return w_zero
  }
}