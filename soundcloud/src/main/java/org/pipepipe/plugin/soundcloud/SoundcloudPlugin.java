package org.pipepipe.plugin.soundcloud;

import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.plugin.PipePipePlugin;
import org.schabi.newpipe.extractor.plugin.PluginContext;

import javax.annotation.Nonnull;

public final class SoundcloudPlugin implements PipePipePlugin {
    @Nonnull
    @Override
    public StreamingService createService(final int serviceId,
                                          @Nonnull final PluginContext context) {
        return new SoundcloudService(serviceId);
    }
}
