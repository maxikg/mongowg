package de.maxikg.mongowg.codec;

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistry;

public class MongoWGCodecProvider implements CodecProvider {

    private final ProcessingProtectedRegionCodec processingProtectedRegionCodec = new ProcessingProtectedRegionCodec();

    @Override
    @SuppressWarnings("unchecked")
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {
        if (processingProtectedRegionCodec.getEncoderClass().equals(clazz))
            return (Codec<T>) processingProtectedRegionCodec;
        else
            return null;
    }
}
