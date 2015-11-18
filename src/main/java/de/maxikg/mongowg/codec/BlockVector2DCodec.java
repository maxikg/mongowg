package de.maxikg.mongowg.codec;

import com.sk89q.worldedit.BlockVector2D;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class BlockVector2DCodec implements Codec<BlockVector2D> {

    public static final BlockVector2DCodec INSTANCE = new BlockVector2DCodec();

    @Override
    public BlockVector2D decode(BsonReader reader, DecoderContext decoderContext) {
        int x = 0;
        int z = 0;

        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String name = reader.readName();
            if ("x".equals(name))
                x = reader.readInt32();
            else if ("z".equals(name))
                z = reader.readInt32();
            else
                reader.skipValue();
        }
        reader.readEndDocument();

        return new BlockVector2D(x, z);
    }

    @Override
    public void encode(BsonWriter writer, BlockVector2D value, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName("x");
        writer.writeInt32(value.getBlockX());
        writer.writeName("z");
        writer.writeInt32(value.getBlockZ());
        writer.writeEndDocument();
    }

    @Override
    public Class<BlockVector2D> getEncoderClass() {
        return BlockVector2D.class;
    }
}
