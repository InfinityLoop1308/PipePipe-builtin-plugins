package org.pipepipe.plugin.peertube;

import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.plugin.PipePipePlugin;
import org.schabi.newpipe.extractor.plugin.PluginContext;
import org.schabi.newpipe.extractor.plugin.PluginPreferences;

import javax.annotation.Nonnull;

/**
 * The instances are managed by {@link PeertubeInstances}. The current instance is also kept
 * in the {@code instance_url} plugin setting, so it can be changed without the instance list UI.
 */
public final class PeertubePlugin implements PipePipePlugin {

    @Nonnull
    @Override
    public StreamingService createService(final int serviceId,
                                          @Nonnull final PluginContext context) {
        final PluginPreferences preferences = context.getPreferences();
        final PeertubeInstances instances = new PeertubeInstances(preferences);
        preferences.addChangeListener(key -> {
            if (PeertubeInstances.INSTANCE_KEY.equals(key)) {
                instances.reloadSelected();
            }
        });
        return new PeertubeService(serviceId, instances);
    }
}
