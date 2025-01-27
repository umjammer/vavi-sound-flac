/*
 * libFLAC - Free Lossless Audio Codec library
 * Copyright (C) 2001,2002,2003  Josh Coalson
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA.
 */

package org.kc7bfi.jflac.frame;

import java.io.IOException;

import org.kc7bfi.jflac.ChannelData;
import org.kc7bfi.jflac.FrameDecodeException;
import org.kc7bfi.jflac.LPCPredictor;
import org.kc7bfi.jflac.io.BitInputStream;
import org.kc7bfi.jflac.util.BitMath;


/**
 * LPC FLAC subframe (channel).
 *
 * @author kc7bfi
 */
public class ChannelLPC extends Channel {

    private static final int SUBFRAME_LPC_QLP_COEFF_PRECISION_LEN = 4; // bits
    private static final int SUBFRAME_LPC_QLP_SHIFT_LEN = 5; // bits
    private static final int MAX_LPC_ORDER = 32;

    /** The residual coding method. */
    private final EntropyCodingMethod entropyCodingMethod;
    /** The FIR order. */
    private final int order;
    /** Quantized FIR filter coefficient precision in bits. */
    private final int qlpCoeffPrecision;
    /** The qlp coeff shift needed. */
    private final int quantizationLevel;
    /** FIR filter coefficients. */
    private final int[] qlpCoeff = new int[MAX_LPC_ORDER];
    /** Warmup samples to prime the predictor, length == order. */
    private final int[] warmup = new int[MAX_LPC_ORDER];
    /** The residual signal, length == (blocksize minus order) samples. */
    private final int[] residual;

    /**
     * The constructor.
     *
     * @param is          The InputBitStream
     * @param header      The FLAC Frame Header
     * @param channelData The decoded channel data (output)
     * @param bps         The bits-per-second
     * @param wastedBits  The bits waisted in the frame
     * @param order       The predicate order
     * @throws IOException          Thrown if error reading from the InputBitStream
     * @throws FrameDecodeException
     */
    public ChannelLPC(BitInputStream is, Header header, ChannelData channelData, int bps, int wastedBits, int order) throws IOException, FrameDecodeException {
        super(header, wastedBits);

        this.residual = channelData.getResidual();
        this.order = order;

        // read warm-up samples
        //logger.log(Level.DEBUG, "Order="+order);
        for (int u = 0; u < order; u++) {
            warmup[u] = is.readRawInt(bps);
        }
        //for (int i = 0; i < order; i++) logger.log(Level.DEBUG, "Warm "+i+" "+warmup[i]);

        // read qlp coeff precision
        int u32 = is.readRawUInt(SUBFRAME_LPC_QLP_COEFF_PRECISION_LEN);
        if (u32 == (1 << SUBFRAME_LPC_QLP_COEFF_PRECISION_LEN) - 1) {
            throw new FrameDecodeException("STREAM_DECODER_ERROR_STATUS_LOST_SYNC");
        }
        qlpCoeffPrecision = u32 + 1;
        //logger.log(Level.DEBUG, "qlpCoeffPrecision="+qlpCoeffPrecision);

        // read qlp shift
        quantizationLevel = is.readRawInt(SUBFRAME_LPC_QLP_SHIFT_LEN);
        //logger.log(Level.DEBUG, "quantizationLevel="+quantizationLevel);

        // read quantized lp coefficiencts
        for (int u = 0; u < order; u++) {
            qlpCoeff[u] = is.readRawInt(qlpCoeffPrecision);
        }

        // read entropy coding method info
        int codingType = is.readRawUInt(ENTROPY_CODING_METHOD_TYPE_LEN);
        //logger.log(Level.DEBUG, "codingType="+codingType);
        switch (codingType) {
        case ENTROPY_CODING_METHOD_PARTITIONED_RICE:
            entropyCodingMethod = new EntropyPartitionedRice();
            break;
        case RESIDUAL_CODING_METHOD_PARTITIONED_RICE2:
            entropyCodingMethod = new EntropyPartitionedRice2();
            break;
        default:
            throw new FrameDecodeException("STREAM_DECODER_UNPARSEABLE_STREAM, " + codingType);
        }
        entropyCodingMethod.order = is.readRawUInt(ENTROPY_CODING_METHOD_PARTITIONED_RICE_ORDER_LEN);
        entropyCodingMethod.contents = channelData.getPartitionedRiceContents();

        // read residual
        entropyCodingMethod.readResidual(is, order, entropyCodingMethod.order, header,
                channelData.getResidual());

//        if (logger.isLoggable(Level.DEBUG)) {
//            System.out.println();
//            for (int i = 0; i < header.blockSize; i++) {
//                System.out.print(channelData.getResidual()[i] + " ");
//                if (i % 200 == 0) System.out.println();
//            }
//            System.out.println();
//        }

        // decode the subframe
        System.arraycopy(warmup, 0, channelData.getOutput(), 0, order);
        if (bps + qlpCoeffPrecision + BitMath.ilog2(order) <= 32) {
            if (bps <= 16 && qlpCoeffPrecision <= 16)
                LPCPredictor.restoreSignal(channelData.getResidual(), header.blockSize - order, qlpCoeff, order, quantizationLevel, channelData.getOutput(), order);
            else
                LPCPredictor.restoreSignal(channelData.getResidual(), header.blockSize - order, qlpCoeff, order, quantizationLevel, channelData.getOutput(), order);
        } else {
            LPCPredictor.restoreSignalWide(channelData.getResidual(), header.blockSize - order, qlpCoeff, order, quantizationLevel, channelData.getOutput(), order);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ChannelLPC: Order=" + order + " WastedBits=" + wastedBits);
        sb.append(" qlpCoeffPrecision=").append(qlpCoeffPrecision).append(" quantizationLevel=").append(quantizationLevel);
        sb.append("\n\t\tqlpCoeff: ");
        for (int i = 0; i < order; i++) sb.append(qlpCoeff[i]).append(" ");
        sb.append("\n\t\tWarmup: ");
        for (int i = 0; i < order; i++) sb.append(warmup[i]).append(" ");
        sb.append("\n\t\tParameter: ");
        for (int i = 0; i < (1 << entropyCodingMethod.order); i++)
            sb.append(entropyCodingMethod.contents.parameters[i]).append(" ");
        return sb.toString();
    }
}
