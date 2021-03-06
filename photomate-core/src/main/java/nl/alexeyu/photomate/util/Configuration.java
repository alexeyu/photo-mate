package nl.alexeyu.photomate.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import nl.alexeyu.photomate.model.PhotoStockAccess;
import nl.alexeyu.photomate.model.PhotoStock;

@Singleton
public class Configuration {

    private static final String CONFIG_LOCATION_SYS_PROP = "configfile";

    private static final String DEFAULT_CONFIG_FILE = "/photomate.properties";

    private static final Pattern PHOTO_STOCK_NAME = Pattern.compile("stock\\.([\\w]+)\\.name");

    private static final String DEFAULT_FOLDER_PROPERTY = "defaultFolder";

    private static final String UPLOAD_FOLDER_PROPERTY = "uploadFolder";

    private static final String ARCHIVE_FOLDER_PROPERTY = "archiveFolder";

    private final Properties properties;

    private final List<PhotoStock> photoStocks;

    public Configuration(Properties props) {
        this.properties = new Properties(props);
        photoStocks = readPhotoStocks();
    }

    public static Configuration createDefault() {
        var location = System.getProperty(CONFIG_LOCATION_SYS_PROP);
        if (location == null) {
            try (var is = Configuration.class.getResourceAsStream(DEFAULT_CONFIG_FILE)) {
                return from(is);
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
        try (var is = Files.newInputStream(Paths.get(location))) {
            return from(is);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static Configuration from(InputStream is) throws IOException {
        var props = new Properties();
        props.load(is);
        return new Configuration(props);
    }

    private List<PhotoStock> readPhotoStocks() {
        return properties.stringPropertyNames().stream()
                .filter(prop -> isNotCommentedOut(prop))
                .map(prop -> PHOTO_STOCK_NAME.matcher(prop))
                .filter(matcher -> matcher.matches())
                .map(matcher -> readPhotoStock(matcher.group(1)))
                .collect(Collectors.toList());
    }

    private boolean isNotCommentedOut(String prop) {
        return !prop.startsWith("#");
    }

    private PhotoStock readPhotoStock(String key) {
        var prefix = "stock." + key + ".";
        var name = getProperty(prefix + "name").orElse("");
        var icon = getProperty(prefix + "icon").orElse("");
        var ftpUrl = getProperty(prefix + "ftp.url").orElse("");
        var ftpUsername = getProperty(prefix + "ftp.username").orElse("");
        var ftpPassword = getProperty(prefix + "ftp.password").orElse("");
        return new PhotoStock(name, icon, new PhotoStockAccess(ftpUrl, ftpUsername, ftpPassword));
    }

    public List<PhotoStock> getPhotoStocks() {
        return photoStocks;
    }

    public Optional<String> getProperty(String property) {
        return Optional.ofNullable(properties.getProperty(property));
    }

    public Path getDefaultFolder() {
        return getFolder(DEFAULT_FOLDER_PROPERTY);
    }

    public Path getUploadFolder() {
        return getFolder(UPLOAD_FOLDER_PROPERTY);
    }

    public Path getArchiveFolder() {
        return getFolder(ARCHIVE_FOLDER_PROPERTY);
    }

    public Path getFolder(String property) {
        try {
            return Optional.ofNullable(properties.getProperty(DEFAULT_FOLDER_PROPERTY))
                    .map(Path::of)
                    .orElseThrow(() -> new IllegalArgumentException());
        } catch (IllegalArgumentException ex) { // Can be also InvalidPathException
            throw new NoSuchElementException("Cannot find path specified for " + property);
        }
    }

}
