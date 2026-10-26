package org.pipepipe.plugin.mediaccc.linkHandler;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;
import org.pipepipe.plugin.mediaccc.extractors.MediaCCCParsingHelper;
import org.schabi.newpipe.extractor.utils.Parser;

public class MediaCCCStreamLinkHandlerFactory extends LinkHandlerFactory {
    public static final String VIDEO_API_ENDPOINT = "https://api.media.ccc.de/public/events/";
    private static final String VIDEO_PATH = "https://media.ccc.de/v/";
    private static final String RECORDING_ID_PATTERN
            = "(?:(?:(?:api\\.)?media\\.ccc\\.de/public/events/)"
            + "|(?:media\\.ccc\\.de/v/))([^/?&#]*)";
    private static final String LIVE_STREAM_PATH = "https://streaming.media.ccc.de/";
    private static final String LIVE_STREAM_ID_PATTERN
            = "streaming\\.media\\.ccc\\.de\\/(\\w+\\/\\w+)";

    @Override
    public String getId(final String url) throws ParsingException {
        String streamId = null;
        try {
            streamId = Parser.matchGroup1(LIVE_STREAM_ID_PATTERN, url);
        } catch (final Parser.RegexException ignored) {
        }

        if (streamId == null) {
            return Parser.matchGroup1(RECORDING_ID_PATTERN, url);
        }
        return streamId;
    }

    @Override
    public String getUrl(final String id) throws ParsingException {
        if (MediaCCCParsingHelper.isLiveStreamId(id)) {
            return LIVE_STREAM_PATH + id;
        }
        return VIDEO_PATH + id;
    }

    @Override
    public boolean onAcceptUrl(final String url) {
        try {
            return getId(url) != null;
        } catch (final ParsingException e) {
            return false;
        }
    }
}
