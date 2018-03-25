package nl.alexeyu.photomate.api;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.ImageIcon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.alexeyu.photomate.model.PhotoMetaData;

public interface PhotoApi<S, P extends AbstractPhoto> {
    
	Logger logger = LogManager.getLogger();

	default List<P> createPhotos(Stream<S> sources, PhotoFactory<S, P> photoFactory) {
        var photos = sources
                .map(photoFactory::createPhoto)
                .collect(Collectors.toList());
        photos.forEach(this::initPhoto);
        return photos;
    }
    
    default void initPhoto(P photo) {
    	Runnable photoInitiator = () -> {
    		thumbnailsSupplier(photo).get().forEach(photo::addThumbnail);
    		photo.setMetaData(metaDataSupplier(photo).get());
    		// scaling of many big photos might be challenging if not enough memory provided
    		// ensure we clean up some
    		System.gc();
    	};
    	CompletableFuture.runAsync(photoInitiator);
    }

    Supplier<? extends PhotoMetaData> metaDataSupplier(P photo);
    
    Supplier<List<ImageIcon>> thumbnailsSupplier(P photo);
    
}
