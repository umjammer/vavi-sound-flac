/*
 * Created on Jun 28, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */

package org.kc7bfi.jflac;

import java.util.HashSet;
import java.util.Set;

import org.kc7bfi.jflac.metadata.StreamInfo;
import org.kc7bfi.jflac.util.ByteData;


/**
 * Class to handle PCM processors.
 *
 * @author kc7bfi
 */
class PCMProcessors implements PCMProcessor {

    private final Set<PCMProcessor> pcmProcessors = new HashSet<>();

    /**
     * Add a PCM processor.
     *
     * @param processor The processor listener to add
     */
    public void addPCMProcessor(PCMProcessor processor) {
        synchronized (pcmProcessors) {
            pcmProcessors.add(processor);
        }
    }

    /**
     * Remove a PCM processor.
     *
     * @param processor The processor listener to remove
     */
    public void removePCMProcessor(PCMProcessor processor) {
        synchronized (pcmProcessors) {
            pcmProcessors.remove(processor);
        }
    }

    /**
     * Process the StreamInfo block.
     *
     * @param info the StreamInfo block
     * @see org.kc7bfi.jflac.PCMProcessor#processStreamInfo(org.kc7bfi.jflac.metadata.StreamInfo)
     */
    @Override
    public void processStreamInfo(StreamInfo info) {
        synchronized (pcmProcessors) {
            for (PCMProcessor processor : pcmProcessors) {
                processor.processStreamInfo(info);
            }
        }
    }

    /**
     * Process the decoded PCM bytes.
     *
     * @param pcm The decoded PCM data
     * @see org.kc7bfi.jflac.PCMProcessor#processPCM(org.kc7bfi.jflac.util.ByteData)
     */
    @Override
    public void processPCM(ByteData pcm) {
        synchronized (pcmProcessors) {
            for (PCMProcessor processor : pcmProcessors) {
                processor.processPCM(pcm);
            }
        }
    }

    public boolean isCanceled() {
        return pcmProcessors.isEmpty();
    }
}
