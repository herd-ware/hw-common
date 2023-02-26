/*
 * File: instr.scala                                                           *
 * Created Date: 2023-02-25 12:54:02 pm                                        *
 * Author: Mathieu Escouteloup                                                 *
 * -----                                                                       *
 * Last Modified: 2023-02-25 09:26:03 pm                                       *
 * Modified By: Mathieu Escouteloup                                            *
 * -----                                                                       *
 * License: See LICENSE.md                                                     *
 * Copyright (c) 2023 HerdWare                                                *
 * -----                                                                       *
 * Description:                                                                *
 */


package herd.common.isa.base

import chisel3._
import chisel3.util._


object INSTR {
  // ******************************
  //            RV32I
  // ******************************
  def LUI       = BitPat("b?????????????????????????0110111")
  def AUIPC     = BitPat("b?????????????????????????0010111")
  def JAL       = BitPat("b?????????????????????????1101111")
  def JALR      = BitPat("b?????????????????000?????1100111")
  def BEQ       = BitPat("b?????????????????000?????1100011")
  def BNE       = BitPat("b?????????????????001?????1100011")
  def BLT       = BitPat("b?????????????????100?????1100011")
  def BGE       = BitPat("b?????????????????101?????1100011")
  def BLTU      = BitPat("b?????????????????110?????1100011")
  def BGEU      = BitPat("b?????????????????111?????1100011")
  def LB        = BitPat("b?????????????????000?????0000011")
  def LH        = BitPat("b?????????????????001?????0000011")
  def LW        = BitPat("b?????????????????010?????0000011")
  def LBU       = BitPat("b?????????????????100?????0000011")
  def LHU       = BitPat("b?????????????????101?????0000011")
  def SB        = BitPat("b?????????????????000?????0100011")
  def SH        = BitPat("b?????????????????001?????0100011")
  def SW        = BitPat("b?????????????????010?????0100011")
  def ADDI      = BitPat("b?????????????????000?????0010011")
  def SLLI      = BitPat("b000000???????????001?????0010011")
  def SLTI      = BitPat("b?????????????????010?????0010011")
  def SLTIU     = BitPat("b?????????????????011?????0010011")
  def XORI      = BitPat("b?????????????????100?????0010011")
  def SRLI      = BitPat("b000000???????????101?????0010011")
  def SRAI      = BitPat("b010000???????????101?????0010011")
  def ORI       = BitPat("b?????????????????110?????0010011")
  def ANDI      = BitPat("b?????????????????111?????0010011")
  def ADD       = BitPat("b0000000??????????000?????0110011")
  def SUB       = BitPat("b0100000??????????000?????0110011")
  def SLL       = BitPat("b0000000??????????001?????0110011")
  def SLT       = BitPat("b0000000??????????010?????0110011")
  def SLTU      = BitPat("b0000000??????????011?????0110011")
  def XOR       = BitPat("b0000000??????????100?????0110011")
  def SRL       = BitPat("b0000000??????????101?????0110011")
  def SRA       = BitPat("b0100000??????????101?????0110011")
  def OR        = BitPat("b0000000??????????110?????0110011")
  def AND       = BitPat("b0000000??????????111?????0110011")

  def FENCETSO  = BitPat("b10000011001100000000000000001111")
  def PAUSE     = BitPat("b00000001000000000000000000001111")
  def FENCE     = BitPat("b?????????????????000?????0001111")
  def ECALL     = BitPat("b00000000000000000000000001110011")
  def EBREAK    = BitPat("b00000000000100000000000001110011")

  // ******************************
  //             RV64I
  // ******************************
  def LWU       = BitPat("b?????????????????110?????0000011")
  def LD        = BitPat("b?????????????????011?????0000011")
  def SD        = BitPat("b?????????????????011?????0100011")
  def ADDIW     = BitPat("b?????????????????000?????0011011")
  def SLLIW     = BitPat("b0000000??????????001?????0011011")
  def SRLIW     = BitPat("b0000000??????????101?????0011011")
  def SRAIW     = BitPat("b0100000??????????101?????0011011")
  def ADDW      = BitPat("b0000000??????????000?????0111011")
  def SUBW      = BitPat("b0100000??????????000?????0111011")
  def SLLW      = BitPat("b0000000??????????001?????0111011")
  def SRLW      = BitPat("b0000000??????????101?????0111011")
  def SRAW      = BitPat("b0100000??????????101?????0111011")

  // ******************************
  //             RV32M
  // ******************************
  def MUL       = BitPat("b0000001??????????000?????0110011")
  def MULH      = BitPat("b0000001??????????001?????0110011")
  def MULHSU    = BitPat("b0000001??????????010?????0110011")
  def MULHU     = BitPat("b0000001??????????011?????0110011")
  def DIV       = BitPat("b0000001??????????100?????0110011")
  def DIVU      = BitPat("b0000001??????????101?????0110011")
  def REM       = BitPat("b0000001??????????110?????0110011")
  def REMU      = BitPat("b0000001??????????111?????0110011")
  
  // ******************************
  //             RV64M
  // ******************************
  def MULW      = BitPat("b0000001??????????001?????0111011")
  def DIVW      = BitPat("b0000001??????????100?????0111011")
  def DIVUW     = BitPat("b0000001??????????101?????0111011")
  def REMW      = BitPat("b0000001??????????110?????0111011")
  def REMUW     = BitPat("b0000001??????????111?????0111011")

  // ******************************
  //           ZIFENCEI
  // ******************************
  def FENCEI    = BitPat("b?????????????????001?????0001111")

  // ******************************
  //            ZICSR
  // ******************************
  def CSRRW0    = BitPat("b?????????????????001000001110011")
  def CSRRW     = BitPat("b?????????????????001?????1110011")
  def CSRRS0    = BitPat("b????????????00000010?????1110011")
  def CSRRS     = BitPat("b?????????????????010?????1110011")
  def CSRRC0    = BitPat("b????????????00000011?????1110011")
  def CSRRC     = BitPat("b?????????????????011?????1110011")
  def CSRRWI0   = BitPat("b?????????????????101000001110011")
  def CSRRWI    = BitPat("b?????????????????101?????1110011")
  def CSRRSI0   = BitPat("b????????????00000110?????1110011")
  def CSRRSI    = BitPat("b?????????????????110?????1110011")
  def CSRRCI0   = BitPat("b????????????00000111?????1110011")
  def CSRRCI    = BitPat("b?????????????????111?????1110011")

  // ******************************
  //             RV32A
  // ******************************
  def LRW       = BitPat("b00010??00000?????010?????0101111")
  def SCW       = BitPat("b00011????????????010?????0101111")
  def AMOSWAPW  = BitPat("b00001????????????010?????0101111")
  def AMOADDW   = BitPat("b00000????????????010?????0101111")
  def AMOXORW   = BitPat("b00100????????????010?????0101111")
  def AMOANDW   = BitPat("b01100????????????010?????0101111")
  def AMOORW    = BitPat("b01000????????????010?????0101111")
  def AMOMINW   = BitPat("b10000????????????010?????0101111")
  def AMOMAXW   = BitPat("b10100????????????010?????0101111")
  def AMOMINUW  = BitPat("b11000????????????010?????0101111")
  def AMOMAXUW  = BitPat("b11100????????????010?????0101111")

  // ******************************
  //             RV64A
  // ******************************
  def LRD       = BitPat("b00010??00000?????011?????0101111")
  def SCD       = BitPat("b00011????????????011?????0101111")
  def AMOSWAPD  = BitPat("b00001????????????011?????0101111")
  def AMOADDD   = BitPat("b00000????????????011?????0101111")
  def AMOXORD   = BitPat("b00100????????????011?????0101111")
  def AMOANDD   = BitPat("b01100????????????011?????0101111")
  def AMOORD    = BitPat("b01000????????????011?????0101111")
  def AMOMIND   = BitPat("b10000????????????011?????0101111")
  def AMOMAXD   = BitPat("b10100????????????011?????0101111")
  def AMOMINUD  = BitPat("b11000????????????011?????0101111")
  def AMOMAXUD  = BitPat("b11100????????????011?????0101111")
  
  // ******************************
  //             RV32B
  // ******************************
  // ------------------------------
  //              ZBA
  // ------------------------------
  def SH1ADD    = BitPat("b0010000??????????010?????0110011")
  def SH2ADD    = BitPat("b0010000??????????100?????0110011")
  def SH3ADD    = BitPat("b0010000??????????110?????0110011")

  // ------------------------------
  //              ZBB
  // ------------------------------
  def ANDN      = BitPat("b0100000??????????111?????0110011")
  def CLZ       = BitPat("b011000000000?????001?????0010011")
  def CPOP      = BitPat("b011000000010?????001?????0010011")
  def CTZ       = BitPat("b011000000001?????001?????0010011")
  def MAX       = BitPat("b0000101??????????110?????0110011")
  def MAXU      = BitPat("b0000101??????????111?????0110011")
  def MIN       = BitPat("b0000101??????????100?????0110011")
  def MINU      = BitPat("b0000101??????????101?????0110011")
  def ORCB      = BitPat("b001010000111?????101?????0010011")
  def ORN       = BitPat("b0100000??????????110?????0110011")
  def REV832    = BitPat("b011010011000?????101?????0010011")
  def ROL       = BitPat("b0110000??????????001?????0110011")
  def ROR       = BitPat("b0110000??????????101?????0110011")
  def RORI      = BitPat("b011000???????????101?????0010011")
  def SEXTB     = BitPat("b011000000100?????001?????0010011")
  def SEXTH     = BitPat("b011000000101?????001?????0010011")
  def XNOR      = BitPat("b0100000??????????100?????0110011")
  def ZEXTH32   = BitPat("b000010000000?????100?????0110011")

  // ------------------------------
  //              ZBC
  // ------------------------------
  def CLMUL     = BitPat("b0000101??????????001?????0110011")
  def CLMULH    = BitPat("b0000101??????????011?????0110011")
  def CLMULR    = BitPat("b0000101??????????010?????0110011")

  // ------------------------------
  //              ZBS
  // ------------------------------
  def BCLR      = BitPat("b0100100??????????001?????0110011")
  def BCLRI     = BitPat("b010010???????????001?????0010011")
  def BEXT      = BitPat("b0100100??????????101?????0110011")
  def BEXTI     = BitPat("b010010???????????101?????0010011")
  def BINV      = BitPat("b0110100??????????001?????0110011")
  def BINVI     = BitPat("b011010???????????001?????0010011")
  def BSET      = BitPat("b0010100??????????001?????0110011")
  def BSETI     = BitPat("b001010???????????001?????0010011")

  // ******************************
  //             RV64B
  // ******************************
  // ------------------------------
  //              ZBA
  // ------------------------------
  def ADDUW     = BitPat("b0000100??????????000?????0111011")
  def SH1ADDUW  = BitPat("b0010000??????????010?????0111011")
  def SH2ADDUW  = BitPat("b0010000??????????100?????0111011")
  def SH3ADDUW  = BitPat("b0010000??????????110?????0111011")
  def SLLIUW    = BitPat("b000010???????????001?????0011011")

  // ------------------------------
  //              ZBB
  // ------------------------------
  def CLZW      = BitPat("b011000000000?????001?????0011011")
  def CPOPW     = BitPat("b011000000010?????001?????0011011")
  def CTZW      = BitPat("b011000000001?????001?????0011011")
  def REV864    = BitPat("b011010111000?????101?????0010011")
  def ROLW      = BitPat("b0110000??????????001?????0111011")
  def RORIW     = BitPat("b0110000??????????101?????0011011")
  def RORW      = BitPat("b0110000??????????101?????0111011")
  def ZEXTH64   = BitPat("b000010000000?????100?????0111011")  

  // ******************************
  //             ZICBO
  // ******************************
  // ------------------------------
  //             ZICBOM
  // ------------------------------
  def CBOCLEAN  = BitPat("b000000000001?????010000000001111")
  def CBOINVAL  = BitPat("b000000000000?????010000000001111")
  def CBOFLUSH  = BitPat("b000000000010?????010000000001111")

  // ------------------------------
  //             ZICBOZ
  // ------------------------------
  def CBOZERO   = BitPat("b000000000100?????010000000001111")
  
  // ------------------------------
  //             ZICBOP
  // ------------------------------
  def PREFETCHI = BitPat("b???????00000?????110000000010011")
  def PREFETCHR = BitPat("b???????00001?????110000000010011")
  def PREFETCHW = BitPat("b???????00011?????110000000010011")
}