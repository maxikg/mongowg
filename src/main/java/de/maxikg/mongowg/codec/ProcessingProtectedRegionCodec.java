package de.maxikg.mongowg.codec;

import de.maxikg.mongowg.model.ProcessingProtectedRegion;
import de.maxikg.mongowg.utils.DataUtils;
import org.bson.BsonDocument;
import org.bson.BsonReader;
import org.bson.BsonString;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class ProcessingProtectedRegionCodec implements Codec<ProcessingProtectedRegion> {

    private final BsonDocumentCodec codec = new BsonDocumentCodec();

    @Override
    public ProcessingProtectedRegion decode(BsonReader reader, DecoderContext decoderContext) {
        // ToDo: Of course this is just temporary. But it is needed for testing things.
        Document document = Document.parse(codec.decode(reader, decoderContext).toJson());
        return new ProcessingProtectedRegion(DataUtils.toProtectedRegion(document), document.getString("parent"), document.getString("world"));
    }

    @Override
    public void encode(BsonWriter writer, ProcessingProtectedRegion value, EncoderContext encoderContext) {
        BsonDocument document = DataUtils.toBson(value.getRegion());
        document.put("world", new BsonString(value.getWorld()));
        codec.encode(writer, document, encoderContext);
    }

    @Override
    public Class<ProcessingProtectedRegion> getEncoderClass() {
        return ProcessingProtectedRegion.class;
    }
}
