package de.maxikg.mongowg.codec;

import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.Location;
import com.sk89q.worldedit.ServerInterface;
import com.sk89q.worldedit.Vector;
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class LocationCodec implements Codec<Location> {

    private final ServerInterface serverInterface;

    public LocationCodec(ServerInterface serverInterface) {
        this.serverInterface = serverInterface;
    }

    @Override
    public Location decode(BsonReader reader, DecoderContext decoderContext) {
        LocalWorld world = null;
        Vector position = Vector.ZERO;
        float pitch = 0.0f;
        float yaw = 0.0f;

        reader.readStartDocument();
        while (reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String name = reader.readName();
            if ("position".equals(name))
                position = VectorCodec.INSTANCE.decode(reader, decoderContext);
            else if ("yaw".equals(name))
                yaw = (float) reader.readDouble();
            else if ("pitch".equals(name))
                pitch = (float) reader.readDouble();
            else if ("world".equals(name))
                world = match(reader.readString());
        }
        reader.readEndDocument();

        if (world == null)
            throw new UnsupportedOperationException("No world specified.");

        return new Location(world, position, yaw, pitch);
    }

    @Override
    public void encode(BsonWriter writer, Location value, EncoderContext encoderContext) {
        writer.writeStartDocument();
        writer.writeName("direction");
        VectorCodec.INSTANCE.encode(writer, value.getDirection(), encoderContext);
        writer.writeName("position");
        VectorCodec.INSTANCE.encode(writer, value.getPosition(), encoderContext);
        writer.writeName("pitch");
        writer.writeDouble(value.getPitch());
        writer.writeName("yaw");
        writer.writeDouble(value.getYaw());
        writer.writeEndDocument();
    }

    @Override
    public Class<Location> getEncoderClass() {
        return Location.class;
    }

    private LocalWorld match(String name) {
        for (LocalWorld world : serverInterface.getWorlds()) {
            if (name.equals(world.getName()))
                return world;
        }

        throw new IllegalArgumentException("No world named '" + name + "' is recognized by WorldEdit.");
    }
}
