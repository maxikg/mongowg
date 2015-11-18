package de.maxikg.mongowg.codec;

import com.sk89q.worldedit.Vector;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class VectorCodec implements Codec<Vector> {

    public static final VectorCodec INSTANCE = new VectorCodec();

    @Override
    public Vector decode(BsonReader reader, DecoderContext decoderContext) {
        double x = 0;
        double y = 0;
        double z = 0;

        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String name = reader.readName();
            if ("x".equals(name))
                x = reader.readDouble();
            else if ("y".equals(name))
                y = reader.readDouble();
            else if ("z".equals(name))
                z = reader.readDouble();
        }
        reader.readEndDocument();

        return new Vector(x, y, z);
    }

    @Override
    public void encode(BsonWriter writer, Vector value, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName("x");
        writer.writeDouble(value.getX());
        writer.writeName("y");
        writer.writeDouble(value.getY());
        writer.writeName("z");
        writer.writeDouble(value.getZ());
        writer.writeEndDocument();
    }

    @Override
    public Class<Vector> getEncoderClass() {
        return Vector.class;
    }
}
