package de.maxikg.mongowg.codec;

import com.sk89q.worldguard.domains.DefaultDomain;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.util.UUID;

public class DefaultDomainCodec implements Codec<DefaultDomain> {

    public static final DefaultDomainCodec INSTANCE = new DefaultDomainCodec();

    @Override
    public DefaultDomain decode(BsonReader reader, DecoderContext decoderContext) {
        DefaultDomain domain = new DefaultDomain();

        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String name = reader.readName();
            if ("players".equals(name)) {
                reader.readStartArray();
                while (reader.readBsonType() != BsonType.END_OF_DOCUMENT)
                    domain.addPlayer(UUID.fromString(reader.readString()));
                reader.readEndArray();
            } else if ("groups".equals(name)) {
                reader.readStartArray();
                while (reader.readBsonType() != BsonType.END_OF_DOCUMENT)
                    domain.addGroup(reader.readString());
                reader.readEndArray();
            } else {
                reader.skipValue();
            }
        }
        reader.readEndDocument();

        return domain;
    }

    @Override
    public void encode(BsonWriter writer, DefaultDomain value, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName("players");
        writer.writeStartArray();
        for (UUID uuid : value.getUniqueIds())
            writer.writeString(uuid.toString());
        writer.writeEndArray();
        writer.writeName("groups");
        writer.writeStartArray();
        for (String group : value.getGroups())
            writer.writeString(group);
        writer.writeEndArray();
        writer.writeEndDocument();
    }

    @Override
    public Class<DefaultDomain> getEncoderClass() {
        return DefaultDomain.class;
    }
}
