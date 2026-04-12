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

package org.kc7bfi.jflac.sound.spi;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URL;
import java.nio.file.Files;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;

import org.kc7bfi.jflac.Constants;
import org.kc7bfi.jflac.FLACDecoder;
import org.kc7bfi.jflac.io.BitInputStream;
import org.kc7bfi.jflac.io.BitOutputStream;
import org.kc7bfi.jflac.metadata.StreamInfo;

import static java.lang.System.getLogger;


/**
 * Provider for Flac audio file reading services. This implementation can parse
 * the format information from Flac audio file, and can produce audio input
 * streams from files of this type.
 *
 * @author Marc Gimpel, Wimba S.A. (marc@wimba.com)
 * @version $Revision: 1.8 $
 */
public class FlacAudioFileReader extends AudioFileReader {

    private static final Logger logger = getLogger(FlacAudioFileReader.class.getName());

    private FLACDecoder decoder;
    private StreamInfo streamInfo;

    /**
     * Obtains the audio file format of the File provided. The File must point
     * to valid audio file data.
     *
     * @param file the File from which file format information should be extracted.
     * @return an AudioFileFormat object describing the audio file format.
     * @throws UnsupportedAudioFileException if the File does not point to a valid audio file data
     *                                       recognized by the system.
     * @throws IOException                   if an I/O exception occurs.
     */
    @Override
    public AudioFileFormat getAudioFileFormat(File file) throws UnsupportedAudioFileException, IOException {
        try (InputStream inputStream = Files.newInputStream(file.toPath())) {
            return getAudioFileFormat(new BufferedInputStream(inputStream), (int) file.length());
        }
    }

    /**
     * Obtains an audio input stream from the URL provided. The URL must point
     * to valid audio file data.
     *
     * @param url the URL for which the AudioInputStream should be constructed.
     * @return an AudioInputStream object based on the audio file data pointed
     * to by the URL.
     * @throws UnsupportedAudioFileException if the File does not point to a valid audio file data
     *                                       recognized by the system.
     * @throws IOException                   if an I/O exception occurs.
     */
    @Override
    public AudioFileFormat getAudioFileFormat(URL url) throws UnsupportedAudioFileException, IOException {
        try (InputStream inputStream = url.openStream()) {
            return getAudioFileFormat(inputStream instanceof BufferedInputStream ? inputStream : new BufferedInputStream(inputStream));
        }
    }

    /**
     * Obtains an audio input stream from the input stream provided.
     *
     * @param stream the input stream from which the AudioInputStream should be constructed.
     * @return an AudioInputStream object based on the audio file data contained
     * in the input stream.
     * @throws UnsupportedAudioFileException if the File does not point to a valid audio file data
     *                                       recognized by the system.
     * @throws IOException                   if an I/O exception occurs.
     */
    @Override
    public AudioFileFormat getAudioFileFormat(InputStream stream) throws UnsupportedAudioFileException, IOException {
        return getAudioFileFormat(stream, AudioSystem.NOT_SPECIFIED);
    }

    /**
     * Return the AudioFileFormat from the given InputStream. Implementation.
     *
     * @param bitStream input to decode
     * @param mediaLength unused
     * @return an AudioInputStream object based on the audio file data contained
     * in the input stream.
     * @throws UnsupportedAudioFileException if the File does not point to a valid audio file data
     *                                       recognized by the system.
     * @throws IOException                   if an I/O exception occurs.
     */
    protected AudioFileFormat getAudioFileFormat(InputStream bitStream, int mediaLength) throws UnsupportedAudioFileException, IOException {
logger.log(Level.DEBUG, "enter available: " + bitStream.available());
        if (!bitStream.markSupported()) {
            throw new IllegalArgumentException("must be mark supported");
        }
        AudioFormat format;
        try {
            bitStream.mark(1000);
            decoder = new FLACDecoder(bitStream);
            streamInfo = decoder.readStreamInfo();
            if (streamInfo == null) {
logger.log(Level.DEBUG, "FLAC file reader: no stream info found");
                throw new UnsupportedAudioFileException("No StreamInfo found");
            }

            format = new FlacAudioFormat(streamInfo);
        } catch (IOException ioe) {
            if ("Could not find Stream Sync".equals(ioe.getMessage())) {
logger.log(Level.DEBUG, "FLAC file reader: not a FLAC stream");
logger.log(Level.TRACE, ioe.getMessage(), ioe);
                throw (UnsupportedAudioFileException) new UnsupportedAudioFileException(ioe.getMessage()).initCause(ioe);
            } else {
                throw ioe;
            }
        } catch (Exception e) {
logger.log(Level.DEBUG, e.toString());
logger.log(Level.TRACE, e.toString(), e);
            throw (UnsupportedAudioFileException) new UnsupportedAudioFileException(e.getMessage()).initCause(e);
        } finally {
            try {
                bitStream.reset();
            } catch (IOException e) {
                logger.log(Level.INFO, e.getMessage());
            }
            logger.log(Level.DEBUG, "finally available: " + bitStream.available());
        }
logger.log(Level.DEBUG, "FLAC file reader: got stream with format " + format);
        return new AudioFileFormat(FlacFileFormatType.FLAC, format, AudioSystem.NOT_SPECIFIED);
    }

    @Override
    public AudioInputStream getAudioInputStream(File file) throws UnsupportedAudioFileException, IOException {
        InputStream inputStream = Files.newInputStream(file.toPath());
        return getAudioInputStream(new BufferedInputStream(inputStream), (int) file.length());
    }

    @Override
    public AudioInputStream getAudioInputStream(URL url) throws UnsupportedAudioFileException, IOException {
        InputStream inputStream = url.openStream();
        return getAudioInputStream(inputStream instanceof BufferedInputStream ? inputStream : new BufferedInputStream(inputStream));
    }

    @Override
    public AudioInputStream getAudioInputStream(InputStream stream) throws UnsupportedAudioFileException, IOException {
        return getAudioInputStream(stream, AudioSystem.NOT_SPECIFIED);
    }

    /**
     * Obtains an audio input stream from the input stream provided. The stream
     * must point to valid audio file data.
     *
     * @param inputStream the input stream from which the AudioInputStream should be
     *                    constructed.
     * @param mediaLength unused
     * @return an AudioInputStream object based on the audio file data contained
     * in the input stream.
     * @throws UnsupportedAudioFileException if the File does not point to a valid audio file data
     *                                       recognized by the system.
     * @throws IOException                   if an I/O exception occurs.
     */
    protected AudioInputStream getAudioInputStream(InputStream inputStream, int mediaLength) throws UnsupportedAudioFileException, IOException {
        AudioFileFormat audioFileFormat = getAudioFileFormat(inputStream, mediaLength);

        // push back the StreamInfo
        ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
        BitOutputStream bitOutStream = new BitOutputStream(byteOutStream);
        bitOutStream.writeByteBlock(Constants.STREAM_SYNC_STRING, Constants.STREAM_SYNC_STRING.length);
        // TODO what if StreamInfo not last?
        streamInfo.write(bitOutStream, false);

        // flush bit input stream
        BitInputStream bis = decoder.getBitInputStream();
        int bytesLeft = bis.getInputBytesUnconsumed();
        byte[] b = new byte[bytesLeft];
        bis.readByteBlockAlignedNoCRC(b, bytesLeft);
        byteOutStream.write(b);

        ByteArrayInputStream byteInStream = new ByteArrayInputStream(byteOutStream.toByteArray());

        SequenceInputStream sequenceInputStream = new SequenceInputStream(byteInStream, inputStream);
        return new AudioInputStream(sequenceInputStream, audioFileFormat.getFormat(), audioFileFormat.getFrameLength());
    }
}
