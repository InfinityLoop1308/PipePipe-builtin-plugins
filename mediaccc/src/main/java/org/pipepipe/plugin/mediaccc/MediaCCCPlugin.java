package org.pipepipe.plugin.mediaccc;

import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.plugin.PipePipePlugin;
import org.schabi.newpipe.extractor.plugin.PluginContext;

import javax.annotation.Nonnull;

public final class MediaCCCPlugin implements PipePipePlugin {
    @Nonnull
    @Override
    public StreamingService createService(final int serviceId,
                                          @Nonnull final PluginContext context) {
        return new MediaCCCService(serviceId);
    }
}
