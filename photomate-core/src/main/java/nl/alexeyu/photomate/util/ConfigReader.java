package nl.alexeyu.photomate.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Singleton;

import nl.alexeyu.photomate.model.PhotoStock;

@Singleton
public class ConfigReader {
	
	private static final String CONFIG_LOCATION_SYS_PROP = "configfile";
	
	private static final String DEFAULT_CONFIG_FILE = "/photomate.properties";
	
	private static final Pattern PHOTO_STOCK_NAME = Pattern.compile("stock\\.([\\w]+)\\.name");
	
	private final Properties properties = new Properties();
	
	private final List<PhotoStock> photoStocks;
	
	public ConfigReader() {
		try (InputStream is = getStream()) {
			properties.load(is);
		} catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
		photoStocks = readPhotoStocks();
	}

	private InputStream getStream() throws IOException {
		String location = System.getProperty(CONFIG_LOCATION_SYS_PROP);
		if (location == null) {
			return getClass().getResourceAsStream(DEFAULT_CONFIG_FILE);
		} 
		return Files.newInputStream(Paths.get(location));
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

	// TODO: builder
	private PhotoStock readPhotoStock(String key) {
		String prefix = "stock." + key + ".";
		String name = getProperty(prefix + "name", "");
		String icon = getProperty(prefix + "icon", "");
		String ftpUrl = getProperty(prefix + "ftp.url", "");
		String ftpUsername = getProperty(prefix + "ftp.username", "");
		String ftpPassword = getProperty(prefix + "ftp.password", "");
		return new PhotoStock(name, icon, ftpUrl, ftpUsername, ftpPassword);
	}
	
	public List<PhotoStock> getPhotoStocks() {
		return photoStocks;
	}
	
	public String getProperty(String property, String defaultValue) {
		return properties.getProperty(property, defaultValue);
	}
}