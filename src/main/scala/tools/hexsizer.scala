/*
 * File: hexsizer.scala                                                        *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:26:51 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.tools

import scala.io.Source
import scala.math._
import chisel3.util.log2Ceil


// Get the size of the hex file in bytes, as a ceiling power of 2
object HexSizer {
  def apply(filepath: String, additionnal_size: Int): Int = {
    var tot_size = 0
    for (line <- Source.fromFile(filepath).getLines) {
        val full_line = line.replace(" ", "")
        val line_size = full_line.length / 2
        tot_size += line_size
    }
    tot_size += additionnal_size /*.text + .(ro)data */
    return pow(2, log2Ceil(tot_size)).toInt
  }
}
