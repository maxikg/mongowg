package de.maxikg.mongowg.codec;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.Location;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.managers.storage.RegionDatabaseUtils;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionType;
import de.maxikg.mongowg.model.ProcessingProtectedRegion;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.Document;
import org.bson.codecs.BsonTypeClassMap;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodec;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProcessingProtectedRegionCodec implements Codec<ProcessingProtectedRegion> {

    private static final Function<Object, UUID> OBJECT_TO_UUID = new Function<Object, UUID>() {
        @Nullable
        @Override
        public UUID apply(@Nullable Object input) {
            if (!(input instanceof String))
                throw new UnsupportedOperationException("Input must be string.");

            return UUID.fromString((String) input);
        }
    };
    private static final Function<Object, String> OBJECT_TO_STRING = new Function<Object, String>() {
        @Nullable
        @Override
        public String apply(@Nullable Object input) {
            if (!(input instanceof String))
                throw new UnsupportedOperationException("Input must be string.");

            return (String) input;
        }
    };

    private final DocumentCodec documentCodec;

    public ProcessingProtectedRegionCodec(CodecRegistry registry) {
        documentCodec = new DocumentCodec(registry, new BsonTypeClassMap());
    }

    @Override
    public ProcessingProtectedRegion decode(BsonReader reader, DecoderContext decoderContext) {
        Document document = documentCodec.decode(reader, decoderContext);
        RegionType regionType = RegionType.valueOf(document.getString("type"));
        ProtectedRegion region;
        switch (regionType) {
            case CUBOID:
                region = new ProtectedCuboidRegion(
                        document.getString("name"),
                        toBlockVector(document.get("min", Document.class)),
                        toBlockVector(document.get("min", Document.class))
                );
                break;
            case GLOBAL:
                region = new GlobalProtectedRegion(
                        document.getString("name")
                );
                break;
            case POLYGON:
                region = new ProtectedPolygonalRegion(
                        document.getString("name"),
                        toBlockVector2D((List<?>) document.get("points", List.class)),
                        document.getInteger("min_y"),
                        document.getInteger("max_y")
                );
                break;
            default:
                throw new UnsupportedOperationException("Unknown region type: " + regionType);
        }
        region.setPriority(document.getInteger("priority", 0));
        apply(region.getOwners(), document.get("owners", Document.class));
        apply(region.getMembers(), document.get("members", Document.class));
        RegionDatabaseUtils.trySetFlagMap(region, document.get("flags", Document.class));

        return new ProcessingProtectedRegion(region, document.getString("world"));
    }

    @Override
    public void encode(BsonWriter writer, ProcessingProtectedRegion value, EncoderContext encoderContext) {
        ProtectedRegion region = value.getRegion();
        Document document = new Document()
                .append("name", region.getId())
                .append("type", region.getType().name())
                .append("parent", value.getParent())
                .append("priority", region.getPriority())
                .append("owners", toDocument(region.getOwners()))
                .append("members", toDocument(region.getMembers()))
                .append("flags", toMapValues(region.getFlags()));

        if (region instanceof ProtectedCuboidRegion) {
            document.append("min", toDocument(region.getMinimumPoint()))
                    .append("max", toDocument(region.getMaximumPoint()));
        } else if (region instanceof ProtectedPolygonalRegion) {
            document.append("points", region.getPoints())
                    .append("min_y", region.getMinimumPoint().getBlockY())
                    .append("max_y", region.getMaximumPoint().getBlockY());
        }

        documentCodec.encode(writer, document, encoderContext);
    }

    @Override
    public Class<ProcessingProtectedRegion> getEncoderClass() {
        return ProcessingProtectedRegion.class;
    }

    private static Document toMapValues(Map<Flag<?>, Object> value) {
        Document document = new Document();
        for (Map.Entry<Flag<?>, Object> entry : value.entrySet())
            document.put(entry.getKey().getName(), entry.getValue());
        return document;
    }

    private static void apply(DefaultDomain domain, Document data) {
        for (UUID uuid : Iterables.transform((List<?>) data.get("players", List.class), OBJECT_TO_UUID))
            domain.addPlayer(uuid);

        for (String group : Iterables.transform((List<?>) data.get("groups", List.class), OBJECT_TO_STRING))
            domain.addGroup(group);
    }

    private static BlockVector toBlockVector(Document document) {
        return new BlockVector(document.getInteger("x"), document.getInteger("y"), document.getInteger("z"));
    }

    private static List<BlockVector2D> toBlockVector2D(Iterable<?> documents) {
        ImmutableList.Builder<BlockVector2D> builder = ImmutableList.builder();
        for (Document document : Iterables.filter(documents, Document.class)) {
            builder.add(new BlockVector2D(
                    document.getInteger("x"),
                    document.getInteger("z")
            ));
        }
        return builder.build();
    }

    private static Document toDocument(DefaultDomain domain) {
        return new Document()
                .append("groups", domain.getGroupDomain().getGroups())
                .append("players", domain.getPlayerDomain().getUniqueIds());
    }

    private static Document toDocument(BlockVector vector) {
        return new Document()
                .append("x", vector.getBlockX())
                .append("y", vector.getBlockY())
                .append("z", vector.getBlockZ());
    }

    private static Object genericToDocument(Object value) {
        if (value instanceof Location) {
            Location location = (Location) value;
            return new Document()
                    .append("direction", location.getDirection())
                    .append("position", location.getPosition())
                    .append("pitch", location.getPitch())
                    .append("yaw", location.getYaw());
        }

        return value;
    }
}
