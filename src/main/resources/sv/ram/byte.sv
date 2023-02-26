/*
 * File: byte.sv
 * Created Date: 2023-02-25 12:54:02 pm
 * Author: Mathieu Escouteloup
 * -----
 * Last Modified: 2023-02-25 09:28:56 pm
 * Modified By: Mathieu Escouteloup
 * -----
 * License: See LICENSE.md
 * Copyright (c) 2023 HerdWare
 * -----
 * Description: 
 */

module ByteRamSv
  #(  parameter INITFILE = "",
      parameter NBYTE = 64,
      parameter NDATABYTE = 8,
      
      localparam NADDRBIT = $clog2(NBYTE))
  (   input logic                     clock,
      input logic                     reset,

      // P1 READ-ONLY PORT
      input logic                     i_p1_en,
      input logic                     i_p1_wen,
      input logic [NDATABYTE-1:0]     i_p1_mask,
      input logic [NADDRBIT-1:0]      i_p1_addr,
      input logic [NDATABYTE*8-1:0]   i_p1_wdata,
      output logic [NDATABYTE*8-1:0]  o_p1_rdata,

      // P2 READ-WRITE PORT
      input logic                     i_p2_en,
      input logic                     i_p2_wen,
      input logic [NDATABYTE-1:0]     i_p2_mask,
      input logic [NADDRBIT-1:0]      i_p2_addr,
      input logic [NDATABYTE*8-1:0]   i_p2_wdata,
      output logic [NDATABYTE*8-1:0]  o_p2_rdata);

  logic [7:0] r_mem [NBYTE-1:0];

  // ******************************
  //           FILE INIT
  // ******************************
  `ifdef verilator
    export "DPI-C" task ext_readmemh;

    task ext_readmemh;
      input string TASK_INITFILE;
      $readmemh(TASK_INITFILE, r_mem);
    endtask
  `else
    initial begin
      $readmemh(INITFILE, r_mem);
    end
  `endif  

  // ******************************
  //             READ
  // ******************************
  for(genvar b = 0; b < NDATABYTE; b++) begin
    always_ff @(posedge clock) begin
      if (reset) begin
        o_p1_rdata[(b + 1) * 8 - 1 : b * 8] = 8'h00;
        o_p2_rdata[(b + 1) * 8 - 1 : b * 8] = 8'h00;
      end
      else begin
        // Port 1
        if (i_p1_en && i_p1_mask[b]) begin
          o_p1_rdata[(b + 1) * 8 - 1 : b * 8] = r_mem[i_p1_addr + b];
        end
        else begin
          o_p1_rdata[(b + 1) * 8 - 1 : b * 8] = 8'h0;
        end

        // Port 2
        if (i_p2_en && i_p2_mask[b]) begin
          o_p2_rdata[(b + 1) * 8 - 1 : b * 8] = r_mem[i_p2_addr + b];
        end
        else begin
          o_p2_rdata[(b + 1) * 8 - 1 : b * 8] = 8'h0;
        end
      end
    end
  end

  // ******************************
  //            WRITE
  // ******************************
  for(genvar b = 0; b < NDATABYTE; b++) begin
    always_ff @(posedge clock) begin
      if (~reset) begin 
        // Port 1
        if (i_p1_en && i_p1_wen[b] && i_p1_mask[b]) begin
          r_mem[i_p1_addr[p] + b] = i_p1_wdata[p][(b + 1) * 8 - 1 : b * 8];
        end

        // Port 2
        if (i_p2_en && i_p2_wen[b] && i_p2_mask[b]) begin
          r_mem[i_p2_addr[p] + b] = i_p2_wdata[p][(b + 1) * 8 - 1 : b * 8];
        end
      end
    end
  end
endmodule
