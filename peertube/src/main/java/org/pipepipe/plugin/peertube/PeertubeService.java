package org.pipepipe.plugin.peertube;

import org.schabi.newpipe.extractor.search.filter.FilterItem;

import static org.schabi.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability.COMMENTS;
import static org.schabi.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability.VIDEO;
import static java.util.Arrays.asList;

import org.schabi.newpipe.extractor.ServiceInstance;
import org.schabi.newpipe.extractor.ServiceInstances;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.channel.ChannelTabExtractor;
import org.schabi.newpipe.extractor.comments.CommentsExtractor;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.kiosk.KioskList;
import org.schabi.newpipe.extractor.linkhandler.ChannelTabs;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.pipepipe.plugin.peertube.extractors.PeertubeAccountExtractor;
import org.pipepipe.plugin.peertube.extractors.PeertubeAccountTabExtractor;
import org.pipepipe.plugin.peertube.extractors.PeertubeChannelExtractor;
import org.pipepipe.plugin.peertube.extractors.PeertubeChannelSearchExtractor;
import org.pipepipe.plugin.peertube.extractors.PeertubeChannelTabExtractor;
import org.pipepipe.plugin.peertube.extractors.PeertubeCommentsExtractor;
import org.pipepipe.plugin.peertube.extractors.PeertubePlaylistExtractor;
import org.pipepipe.plugin.peertube.extractors.PeertubeSearchExtractor;
import org.pipepipe.plugin.peertube.extractors.PeertubeStreamExtractor;
import org.pipepipe.plugin.peertube.extractors.PeertubeSuggestionExtractor;
import org.pipepipe.plugin.peertube.extractors.PeertubeTrendingExtractor;
import org.pipepipe.plugin.peertube.linkHandler.PeertubeChannelLinkHandlerFactory;
import org.pipepipe.plugin.peertube.linkHandler.PeertubeChannelTabLinkHandlerFactory;
import org.pipepipe.plugin.peertube.linkHandler.PeertubeCommentsLinkHandlerFactory;
import org.pipepipe.plugin.peertube.linkHandler.PeertubePlaylistLinkHandlerFactory;
import org.pipepipe.plugin.peertube.linkHandler.PeertubeSearchQueryHandlerFactory;
import org.pipepipe.plugin.peertube.linkHandler.PeertubeStreamLinkHandlerFactory;
import org.pipepipe.plugin.peertube.linkHandler.PeertubeTrendingLinkHandlerFactory;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor;
import org.schabi.newpipe.extractor.suggestion.SuggestionExtractor;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PeertubeService extends StreamingService implements ServiceInstances {

    /** The instance created by the plugin host, used by static helpers. */
    @SuppressWarnings("checkstyle:StaticVariableName")
    public static PeertubeService PeerTube;

    @Nonnull
    private final PeertubeInstances instances;

    public PeertubeService(final int id, @Nonnull final PeertubeInstances instances) {
        super(id, "PeerTube", asList(VIDEO, COMMENTS));
        this.instances = instances;
        PeerTube = this;
    }

    @Override
    public LinkHandlerFactory getStreamLHFactory() {
        return PeertubeStreamLinkHandlerFactory.getInstance();
    }

    @Override
    public ListLinkHandlerFactory getChannelLHFactory() {
        return PeertubeChannelLinkHandlerFactory.getInstance();
    }

    @Override
    public ListLinkHandlerFactory getChannelTabLHFactory() {
        return PeertubeChannelTabLinkHandlerFactory.getInstance();
    }

    @Override
    public ListLinkHandlerFactory getPlaylistLHFactory() {
        return PeertubePlaylistLinkHandlerFactory.getInstance();
    }

    @Override
    public SearchQueryHandlerFactory getSearchQHFactory() {
        return PeertubeSearchQueryHandlerFactory.getInstance();
    }

    @Override
    public ListLinkHandlerFactory getCommentsLHFactory() {
        return PeertubeCommentsLinkHandlerFactory.getInstance();
    }

    @Override
    public SearchExtractor getSearchExtractor(final SearchQueryHandler queryHandler) {
        final List<FilterItem> selectedSortFilter = queryHandler.getSortFilter();

        final Optional<FilterItem> sepiaFilter = PeertubeHelpers.getSepiaFilter(selectedSortFilter);
        return new PeertubeSearchExtractor(this, queryHandler, sepiaFilter.isPresent());
    }

    @Override
    public SuggestionExtractor getSuggestionExtractor() {
        return new PeertubeSuggestionExtractor(this);
    }

    @Override
    public SubscriptionExtractor getSubscriptionExtractor() {
        return null;
    }

    @Override
    public ChannelExtractor getChannelExtractor(final ListLinkHandler linkHandler)
            throws ExtractionException {

        if (linkHandler.getUrl().contains("/video-channels/")) {
            return new PeertubeChannelExtractor(this, linkHandler);
        } else {
            return new PeertubeAccountExtractor(this, linkHandler);
        }
    }

    @Override
    public ChannelTabExtractor getChannelTabExtractor(final ListLinkHandler linkHandler)
            throws ExtractionException {
        final String tab = linkHandler.getContentFilters().get(0).getName();
        switch (tab) {
            case ChannelTabs.CHANNELS:
                return new PeertubeAccountTabExtractor(this, linkHandler);
            case ChannelTabs.VIDEOS:
            case ChannelTabs.PLAYLISTS:
                return new PeertubeChannelTabExtractor(this, linkHandler);
            case ChannelTabs.SEARCH:
                return new PeertubeChannelSearchExtractor(this, linkHandler);
        }
        throw new ParsingException("tab " + tab + " not supported");
    }

    @Override
    public PlaylistExtractor getPlaylistExtractor(final ListLinkHandler linkHandler)
            throws ExtractionException {
        return new PeertubePlaylistExtractor(this, linkHandler);
    }

    @Override
    public StreamExtractor getStreamExtractor(final LinkHandler linkHandler)
            throws ExtractionException {
        return new PeertubeStreamExtractor(this, linkHandler);
    }

    @Override
    public CommentsExtractor getCommentsExtractor(final ListLinkHandler linkHandler)
            throws ExtractionException {
        return new PeertubeCommentsExtractor(this, linkHandler);
    }

    @Override
    public String getBaseUrl() {
        return instances.getCurrentInstance().getUrl();
    }

    @Nonnull
    @Override
    public List<ServiceInstance> getInstances() {
        return instances.getInstances();
    }

    @Nonnull
    @Override
    public ServiceInstance getCurrentInstance() {
        return instances.getCurrentInstance();
    }

    @Nonnull
    @Override
    public ServiceInstance addInstance(@Nonnull final String url) throws ExtractionException {
        return instances.addInstance(url);
    }

    @Override
    public void selectInstance(@Nonnull final ServiceInstance instance) {
        instances.selectInstance(instance);
    }

    @Override
    public void setInstances(@Nonnull final List<ServiceInstance> newInstances) {
        instances.setInstances(newInstances);
    }

    @Nullable
    @Override
    public String getInstancesHelpUrl() {
        return instances.getInstancesHelpUrl();
    }

    @Override
    public KioskList getKioskList() throws ExtractionException {
        final KioskList.KioskExtractorFactory kioskFactory = (streamingService, url, id) ->
                new PeertubeTrendingExtractor(
                        PeertubeService.this,
                        new PeertubeTrendingLinkHandlerFactory().fromId(id),
                        id
                );

        final KioskList list = new KioskList(this);

        // add kiosks here e.g.:
        final PeertubeTrendingLinkHandlerFactory h = new PeertubeTrendingLinkHandlerFactory();
        try {
            list.addKioskEntry(kioskFactory, h, PeertubeTrendingLinkHandlerFactory.KIOSK_TRENDING);
            list.addKioskEntry(kioskFactory, h,
                    PeertubeTrendingLinkHandlerFactory.KIOSK_MOST_LIKED);
            list.addKioskEntry(kioskFactory, h, PeertubeTrendingLinkHandlerFactory.KIOSK_RECENT);
            list.addKioskEntry(kioskFactory, h, PeertubeTrendingLinkHandlerFactory.KIOSK_LOCAL);
            list.setDefaultKiosk(PeertubeTrendingLinkHandlerFactory.KIOSK_TRENDING);
        } catch (final Exception e) {
            throw new ExtractionException(e);
        }

        return list;
    }


}
