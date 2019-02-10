package nl.alexeyu.photomate.search.shutterstock;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.swing.ImageIcon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;

import nl.alexeyu.photomate.api.PhotoApi;
import nl.alexeyu.photomate.search.api.PhotoStockApi;
import nl.alexeyu.photomate.search.api.RemotePhoto;
import nl.alexeyu.photomate.util.ConfigReader;

public class ShutterPhotoStockApi implements PhotoApi<ShutterPhotoDescription, RemotePhoto>, PhotoStockApi {

    private static final String DEFAULT_RESULTS_PER_PAGE = "10";

    private static final Logger logger = LogManager.getLogger();

    private static final String QUERY_TEMPLATE = "http://api.shutterstock.com/images/search.json?searchterm=%s&results_per_page=%s&search_group=photos";

    @Inject
    private ConfigReader configReader;

    private HttpClient client;

    private int resultsPerPage;

    @Inject
    public void init() {
        Optional<String> resultsPerPageProperty = configReader.getProperty("stock.shutter.api.resultsPerPage");
        this.resultsPerPage = Integer.valueOf(resultsPerPageProperty.orElse(DEFAULT_RESULTS_PER_PAGE));
        this.client = HttpClient.newBuilder().version(Version.HTTP_1_1).authenticator(new PasswordAuthenticator())
                .build();
    }

    @Override
    public List<RemotePhoto> search(String keywords) {
        var searchUri = String.format(QUERY_TEMPLATE, encode(keywords), resultsPerPage);
        var result = new HttpResponseSupplier<>(searchUri, new JsonResponseReader<>(ShutterSearchResult.class)).get();
        return createPhotos(result.getPhotoDescriptions().stream(),
                source -> new RemotePhoto(source.getUrl(), source.getThumbailUrl()));
    }

    private String encode(String keywords) {
        try {
            return URLEncoder.encode(keywords, Charsets.UTF_8.toString());
        } catch (IOException ex) {
            logger.error("Could not encode keywords", ex);
            return "";
        }
    }

    @Override
    public Supplier<ShutterPhotoDetails> metaDataSupplier(RemotePhoto photo) {
        return new HttpResponseSupplier<>(photo.photoUrl() + ".json",
                new JsonResponseReader<>(ShutterPhotoDetails.class));
    }

    @Override
    public Supplier<List<ImageIcon>> thumbnailsSupplier(RemotePhoto photo) {
        return new HttpResponseSupplier<>(photo.thumbnailUrl(),
                content -> Collections.singletonList(new ImageIcon(content)));
    }

    private static class JsonResponseReader<T> implements Function<String, T> {

        private final Class<T> clazz;

        public JsonResponseReader(Class<T> clazz) {
            this.clazz = clazz;
        }

        @Override
        public T apply(String content) {
            try {
                return new ObjectMapper().readValue(content, clazz);
            } catch (IOException ex) {
                logger.error("Cannot read content", ex);
                return null;
            }
        }

    }

    private class HttpResponseSupplier<T> implements Supplier<T> {

        private final URI uri;
        private final Function<String, T> responseReader;

        public HttpResponseSupplier(String url, Function<String, T> responseReader) {
            this.uri = URI.create(url);
            this.responseReader = responseReader;
        }

        @Override
        public T get() {
            try {
                var request = HttpRequest.newBuilder().GET().uri(uri).build();
                var response = client.send(request, BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    logger.error("Cannot find photos: {}", response.statusCode());
                    return null;
                }
                return responseReader.apply(response.body());
            } catch (IOException | InterruptedException ex) {
                logger.error("Cannot read url {}", ex);
                return null;
            }
        }

    }

    private static class PasswordAuthenticator extends Authenticator {

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            var name = System.getProperty("SHUTTERSTOCK_API_NAME", "");
            var apiKey = System.getProperty("SHUTTERSTOCK_API_KEY", "");
            return new PasswordAuthentication(name, apiKey.toCharArray());
        }

    }

}
