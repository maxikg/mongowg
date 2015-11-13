package de.maxikg.mongowg.codec;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.maxikg.mongowg.utils.DataUtils;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class ProtectedRegionCodec implements Codec<ProtectedRegion> {

    private final BsonDocumentCodec codec = new BsonDocumentCodec();

    @Override
    public ProtectedRegion decode(BsonReader reader, DecoderContext decoderContext) {
        // ToDo: Of course this is just temporary. But it is needed for testing things.
        return DataUtils.toProtectedRegion(Document.parse(codec.decode(reader, decoderContext).toJson()));
    }

    @Override
    public void encode(BsonWriter writer, ProtectedRegion value, EncoderContext encoderContext) {
        codec.encode(writer, DataUtils.toBson(value), encoderContext);
    }

    @Override
    public Class<ProtectedRegion> getEncoderClass() {
        return ProtectedRegion.class;
    }
}
