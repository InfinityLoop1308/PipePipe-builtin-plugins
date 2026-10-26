package org.pipepipe.plugin.soundcloud;

import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.channel.ChannelTabExtractor;
import org.schabi.newpipe.extractor.comments.CommentsExtractor;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.kiosk.KioskList;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.pipepipe.plugin.soundcloud.extractors.SoundcloudChannelExtractor;
import org.pipepipe.plugin.soundcloud.extractors.SoundcloudChannelTabExtractor;
import org.pipepipe.plugin.soundcloud.extractors.SoundcloudChartsExtractor;
import org.pipepipe.plugin.soundcloud.extractors.SoundcloudCommentsExtractor;
import org.pipepipe.plugin.soundcloud.extractors.SoundcloudPlaylistExtractor;
import org.pipepipe.plugin.soundcloud.extractors.SoundcloudSearchExtractor;
import org.pipepipe.plugin.soundcloud.extractors.SoundcloudStreamExtractor;
import org.pipepipe.plugin.soundcloud.extractors.SoundcloudSubscriptionExtractor;
import org.pipepipe.plugin.soundcloud.extractors.SoundcloudSuggestionExtractor;
import org.pipepipe.plugin.soundcloud.linkHandler.SoundcloudChannelLinkHandlerFactory;
import org.pipepipe.plugin.soundcloud.linkHandler.SoundcloudChannelTabLinkHandlerFactory;
import org.pipepipe.plugin.soundcloud.linkHandler.SoundcloudChartsLinkHandlerFactory;
import org.pipepipe.plugin.soundcloud.linkHandler.SoundcloudCommentsLinkHandlerFactory;
import org.pipepipe.plugin.soundcloud.linkHandler.SoundcloudPlaylistLinkHandlerFactory;
import org.pipepipe.plugin.soundcloud.linkHandler.SoundcloudSearchQueryHandlerFactory;
import org.pipepipe.plugin.soundcloud.linkHandler.SoundcloudStreamLinkHandlerFactory;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor;

import java.util.List;

import static java.util.Arrays.asList;
import static org.schabi.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability.AUDIO;
import static org.schabi.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability.COMMENTS;

public class SoundcloudService extends StreamingService {

    /** The instance created by the plugin host, used by static helpers. */
    @SuppressWarnings("checkstyle:StaticVariableName")
    public static SoundcloudService SoundCloud;

    public SoundcloudService(final int id) {
        super(id, "SoundCloud", asList(AUDIO, COMMENTS));
        SoundCloud = this;
    }

    @Override
    public long getInfoCacheExpirationMillis() {
        // stream urls expire quickly
        return 5 * 60 * 1000L;
    }

    @Override
    public String getBaseUrl() {
        return "https://soundcloud.com";
    }

    @Override
    public SearchQueryHandlerFactory getSearchQHFactory() {
        return SoundcloudSearchQueryHandlerFactory.getInstance();
    }

    @Override
    public LinkHandlerFactory getStreamLHFactory() {
        return SoundcloudStreamLinkHandlerFactory.getInstance();
    }

    @Override
    public ListLinkHandlerFactory getChannelLHFactory() {
        return SoundcloudChannelLinkHandlerFactory.getInstance();
    }

    @Override
    public ListLinkHandlerFactory getChannelTabLHFactory() {
        return SoundcloudChannelTabLinkHandlerFactory.getInstance();
    }

    @Override
    public ListLinkHandlerFactory getPlaylistLHFactory() {
        return SoundcloudPlaylistLinkHandlerFactory.getInstance();
    }

    @Override
    public List<ContentCountry> getSupportedCountries() {
        // Country selector here: https://soundcloud.com/charts/top?genre=all-music
        return ContentCountry.listFrom(
                "AU", "CA", "DE", "FR", "GB", "IE", "NL", "NZ", "US"
        );
    }

    @Override
    public StreamExtractor getStreamExtractor(final LinkHandler linkHandler) {
        return new SoundcloudStreamExtractor(this, linkHandler);
    }

    @Override
    public ChannelExtractor getChannelExtractor(final ListLinkHandler linkHandler) {
        return new SoundcloudChannelExtractor(this, linkHandler);
    }

    @Override
    public ChannelTabExtractor getChannelTabExtractor(final ListLinkHandler linkHandler) {
        return new SoundcloudChannelTabExtractor(this, linkHandler);
    }

    @Override
    public PlaylistExtractor getPlaylistExtractor(final ListLinkHandler linkHandler) {
        return new SoundcloudPlaylistExtractor(this, linkHandler);
    }

    @Override
    public SearchExtractor getSearchExtractor(final SearchQueryHandler queryHandler) {
        return new SoundcloudSearchExtractor(this, queryHandler);
    }

    @Override
    public SoundcloudSuggestionExtractor getSuggestionExtractor() {
        return new SoundcloudSuggestionExtractor(this);
    }
    @Override
    public KioskList getKioskList() throws ExtractionException {
        final KioskList.KioskExtractorFactory chartsFactory = (streamingService, url, id) ->
                new SoundcloudChartsExtractor(SoundcloudService.this,
                        new SoundcloudChartsLinkHandlerFactory().fromUrl(url), id);

        final KioskList list = new KioskList(this);

        // add kiosks here e.g.:
        final SoundcloudChartsLinkHandlerFactory h = new SoundcloudChartsLinkHandlerFactory();
        try {
            list.addKioskEntry(chartsFactory, h, "Top 50");
            list.addKioskEntry(chartsFactory, h, "New & hot");
            list.setDefaultKiosk("New & hot");
        } catch (final Exception e) {
            throw new ExtractionException(e);
        }

        return list;
    }

    @Override
    public SubscriptionExtractor getSubscriptionExtractor() {
        return new SoundcloudSubscriptionExtractor(this);
    }

    @Override
    public ListLinkHandlerFactory getCommentsLHFactory() {
        return SoundcloudCommentsLinkHandlerFactory.getInstance();
    }

    @Override
    public CommentsExtractor getCommentsExtractor(final ListLinkHandler linkHandler)
            throws ExtractionException {
        return new SoundcloudCommentsExtractor(this, linkHandler);
    }
}
