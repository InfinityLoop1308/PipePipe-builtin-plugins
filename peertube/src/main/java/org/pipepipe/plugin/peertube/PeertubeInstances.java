package org.pipepipe.plugin.peertube;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import com.grack.nanojson.JsonStringWriter;
import com.grack.nanojson.JsonWriter;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceInstance;
import org.schabi.newpipe.extractor.ServiceInstances;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import org.schabi.newpipe.extractor.plugin.PluginPreferences;
import org.schabi.newpipe.extractor.utils.JsonUtils;
import org.schabi.newpipe.extractor.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The PeerTube instances of the plugin: the current one, the saved list and its storage.
 *
 * <p>The current instance is stored in the {@value #INSTANCE_KEY} preference, which is also
 * the plugin setting of the same name, so users can still set it without the instance list
 * UI. The list is stored as JSON in {@value #LIST_KEY}.</p>
 */
final class PeertubeInstances implements ServiceInstances {

    static final String INSTANCE_KEY = "instance_url";
    static final ServiceInstance DEFAULT_INSTANCE =
            new ServiceInstance("FramaTube", "https://framatube.org");

    private static final String LIST_KEY = "instances";
    private static final String HELP_URL = "https://joinpeertube.org/instances#instances-list";

    @Nonnull
    private final PluginPreferences preferences;
    @Nonnull
    private final List<ServiceInstance> instances = new ArrayList<>();
    @Nonnull
    private volatile ServiceInstance current;

    PeertubeInstances(@Nonnull final PluginPreferences preferences) {
        this.preferences = preferences;
        synchronized (this) {
            load();
        }
    }

    private void load() {
        final String selectedUrl = normalize(preferences.getString(INSTANCE_KEY, null));
        instances.clear();
        instances.addAll(readList());

        ServiceInstance selected = selectedUrl == null ? null : findByUrl(selectedUrl);
        if (selected == null) {
            selected = selectedUrl == null
                    ? DEFAULT_INSTANCE
                    : new ServiceInstance(hostOf(selectedUrl), selectedUrl);
        }
        if (findByUrl(selected.getUrl()) == null) {
            instances.add(0, selected);
            persistList();
        }
        current = selected;
        if (!selected.getUrl().equals(selectedUrl)) {
            preferences.putString(INSTANCE_KEY, selected.getUrl());
        }
    }

    /**
     * Re-read the current instance after {@value #INSTANCE_KEY} was changed outside of this
     * class, for example from the plugin settings screen.
     */
    void reloadSelected() {
        final String url = normalize(preferences.getString(INSTANCE_KEY, null));
        if (url == null) {
            return;
        }
        synchronized (this) {
            ServiceInstance selected = findByUrl(url);
            if (selected == null) {
                selected = new ServiceInstance(hostOf(url), url);
                instances.add(selected);
                persistList();
            }
            current = selected;
        }
        if (!url.equals(preferences.getString(INSTANCE_KEY, null))) {
            preferences.putString(INSTANCE_KEY, url);
        }
    }

    @Nonnull
    @Override
    public synchronized List<ServiceInstance> getInstances() {
        return Collections.unmodifiableList(new ArrayList<>(instances));
    }

    @Nonnull
    @Override
    public ServiceInstance getCurrentInstance() {
        return current;
    }

    @Nonnull
    @Override
    public ServiceInstance addInstance(@Nonnull final String url) throws ExtractionException {
        final String normalized = normalize(url);
        if (normalized == null) {
            throw new ExtractionException("Only HTTPS URLs are supported");
        }
        synchronized (this) {
            final ServiceInstance existing = findByUrl(normalized);
            if (existing != null) {
                return existing;
            }
        }

        // the network request must not block the other instance operations
        final ServiceInstance instance = new ServiceInstance(fetchName(normalized), normalized);
        synchronized (this) {
            final ServiceInstance existing = findByUrl(normalized);
            if (existing != null) {
                return existing;
            }
            instances.add(instance);
            persistList();
        }
        return instance;
    }

    @Override
    public synchronized void selectInstance(@Nonnull final ServiceInstance instance) {
        ServiceInstance selected = findByUrl(instance.getUrl());
        if (selected == null) {
            selected = instance;
            instances.add(selected);
            persistList();
        }
        current = selected;
        preferences.putString(INSTANCE_KEY, selected.getUrl());
    }

    @Override
    public synchronized void setInstances(@Nonnull final List<ServiceInstance> newInstances) {
        instances.clear();
        for (final ServiceInstance instance : newInstances) {
            if (instance == null) {
                continue;
            }
            final String url = normalize(instance.getUrl());
            if (url == null || findByUrl(url) != null) {
                continue;
            }
            instances.add(url.equals(instance.getUrl())
                    ? instance
                    : new ServiceInstance(instance.getName(), url));
        }

        if (instances.isEmpty()) {
            instances.add(DEFAULT_INSTANCE);
            current = DEFAULT_INSTANCE;
            preferences.putString(INSTANCE_KEY, DEFAULT_INSTANCE.getUrl());
        } else if (findByUrl(current.getUrl()) == null) {
            instances.add(current);
        }
        persistList();
    }

    @Nullable
    @Override
    public String getInstancesHelpUrl() {
        return HELP_URL;
    }

    @Nullable
    private ServiceInstance findByUrl(@Nonnull final String url) {
        for (final ServiceInstance instance : instances) {
            if (instance.getUrl().equals(url)) {
                return instance;
            }
        }
        return null;
    }

    private void persistList() {
        final JsonStringWriter writer = JsonWriter.string().object().array("instances");
        for (final ServiceInstance instance : instances) {
            writer.object();
            writer.value("name", instance.getName());
            writer.value("url", instance.getUrl());
            writer.end();
        }
        preferences.putString(LIST_KEY, writer.end().end().done());
    }

    @Nonnull
    private List<ServiceInstance> readList() {
        final String saved = preferences.getString(LIST_KEY, null);
        final List<ServiceInstance> result = new ArrayList<>();
        if (saved == null) {
            return result;
        }
        try {
            final JsonArray array = JsonParser.object().from(saved).getArray("instances");
            for (int i = 0; i < array.size(); i++) {
                final JsonObject item = array.getObject(i);
                final String url = normalize(item.getString("url", null));
                final String name = item.getString("name", null);
                if (url == null || name == null || name.trim().isEmpty()) {
                    continue;
                }
                boolean duplicate = false;
                for (final ServiceInstance instance : result) {
                    if (instance.getUrl().equals(url)) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    result.add(new ServiceInstance(name, url));
                }
            }
        } catch (final JsonParserException | RuntimeException e) {
            result.clear();
        }
        return result;
    }

    @Nonnull
    private static String fetchName(@Nonnull final String url) throws ExtractionException {
        try {
            final Response response = NewPipe.getDownloader().get(url + "/api/v1/config");
            if (response == null || Utils.isBlank(response.responseBody())) {
                throw new ExtractionException("Empty response from " + url);
            }
            final JsonObject json = JsonParser.object().from(response.responseBody());
            return JsonUtils.getString(json, "instance.name");
        } catch (final ReCaptchaException | IOException | JsonParserException
                | ParsingException e) {
            throw new ExtractionException("Could not validate instance " + url, e);
        }
    }

    @Nullable
    static String normalize(@Nullable final String value) {
        if (value == null) {
            return null;
        }
        String url = value.trim();
        if (url.isEmpty()) {
            return null;
        }
        if (!url.startsWith("http")) {
            url = "https://" + url;
        }
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (!url.startsWith("https://") || url.length() <= "https://".length()) {
            return null;
        }
        return url;
    }

    @Nonnull
    private static String hostOf(@Nonnull final String url) {
        return url.substring("https://".length());
    }
}
