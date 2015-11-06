package de.maxikg.mongowg.utils;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldedit.Location;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.managers.storage.RegionDatabaseUtils;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionType;
import org.bson.BsonArray;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonNull;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.Document;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Utilities, primary for bson and document conversions.
 */
public class DataUtils {

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

    private DataUtils() {
    }

    /**
     * Parses a {@code ProtectedRegion} from {@link Document}.
     *
     * @param document The document from which the region should parsed
     * @return The parsed region
     */
    public static ProtectedRegion toProtectedRegion(Document document) {
        ProtectedRegion protectedRegion;
        RegionType regionType = RegionType.valueOf(document.getString("type"));
        switch (regionType) {
            case CUBOID:
                protectedRegion = new ProtectedCuboidRegion(
                        document.getString("name"),
                        documentToBlockVector(document.get("min", Document.class)),
                        documentToBlockVector(document.get("max", Document.class))
                );
                break;
            case GLOBAL:
                protectedRegion = new GlobalProtectedRegion(
                        document.getString("name")
                );
                break;
            case POLYGON:
                protectedRegion = new ProtectedPolygonalRegion(
                        document.getString("name"),
                        documentToBlockVector2D((List<?>) document.get("points", List.class)),
                        document.getInteger("min_y"),
                        document.getInteger("max_y")
                );
                break;
            default:
                throw new UnsupportedOperationException("Unknown region type: " + regionType);
        }
        protectedRegion.setPriority(document.getInteger("priority", 0));
        apply(protectedRegion.getOwners(), document.get("owners", Document.class));
        apply(protectedRegion.getMembers(), document.get("members", Document.class));
        RegionDatabaseUtils.trySetFlagMap(protectedRegion, document.get("flags", Document.class));

        return protectedRegion;
    }

    /**
     * Maps a {@code ProtectedRegion} as an {@link BsonDocument}.
     *
     * @param region The region which should be mapped
     * @return The mapped region
     */
    public static BsonDocument toBson(ProtectedRegion region) {
        ProtectedRegion parent = region.getParent();
        BsonDocument document = new BsonDocument()
                .append("name", new BsonString(region.getId()))
                .append("type", new BsonString(region.getType().name()))
                .append("parent", parent != null ? new BsonString(parent.getId()) : BsonNull.VALUE)
                .append("priority", new BsonInt32(region.getPriority()))
                .append("owners", toBson(region.getOwners()))
                .append("members", toBson(region.getMembers()))
                .append("flags", flagsToBson(region.getFlags()));

        if (region instanceof ProtectedCuboidRegion) {
            document.append("min", toBson(region.getMinimumPoint()))
                    .append("max", toBson(region.getMaximumPoint()));
        } else if (region instanceof ProtectedPolygonalRegion) {
            document.append("points", blockVector2DsToBson(region.getPoints()))
                    .append("min_y", new BsonInt32(region.getMinimumPoint().getBlockY()))
                    .append("max_y", new BsonInt32(region.getMaximumPoint().getBlockY()));
        }

        return document;
    }

    private static void apply(DefaultDomain domain, Document data) {
        for (UUID uuid : Iterables.transform((List<?>) data.get("players", List.class), OBJECT_TO_UUID))
            domain.addPlayer(uuid);

        for (String group : Iterables.transform((List<?>) data.get("groups", List.class), OBJECT_TO_STRING))
            domain.addGroup(group);
    }

    private static BsonDocument toBson(DefaultDomain domain) {
        return new BsonDocument()
                .append("groups", new BsonArray(stringsToBson(domain.getGroupDomain().getGroups())))
                .append("players", new BsonArray(uuidsToBson(domain.getPlayerDomain().getUniqueIds())));
    }

    private static BsonDocument toBson(Location location) {
        return new BsonDocument()
                .append("direction", toBson(location.getDirection()))
                .append("position", toBson(location.getPosition()))
                .append("pitch", new BsonDouble(location.getPitch()))
                .append("yaw", new BsonDouble(location.getYaw()));
    }

    private static BsonDocument toBson(Vector vector) {
        return new BsonDocument()
                .append("x", new BsonDouble(vector.getX()))
                .append("y", new BsonDouble(vector.getY()))
                .append("z", new BsonDouble(vector.getZ()));
    }

    private static BsonDocument toBson(BlockVector vector) {
        return new BsonDocument()
                .append("x", new BsonInt32(vector.getBlockX()))
                .append("y", new BsonInt32(vector.getBlockY()))
                .append("z", new BsonInt32(vector.getBlockZ()));
    }

    private static BlockVector documentToBlockVector(Document document) {
        return new BlockVector(document.getInteger("x"), document.getInteger("y"), document.getInteger("z"));
    }

    private static List<BsonString> stringsToBson(Iterable<String> strings) {
        ImmutableList.Builder<BsonString> builder = ImmutableList.builder();
        for (String str : strings)
            builder.add(new BsonString(str));
        return builder.build();
    }

    private static List<BsonString> uuidsToBson(Iterable<UUID> uuids) {
        ImmutableList.Builder<BsonString> builder = ImmutableList.builder();
        for (UUID uuid : uuids)
            builder.add(new BsonString(uuid.toString()));
        return builder.build();
    }

    private static BsonDocument flagsToBson(Map<Flag<?>, Object> flags) {
        BsonDocument document = new BsonDocument();
        for (Map.Entry<Flag<?>, Object> flag : flags.entrySet())
            document.append(flag.getKey().getName(), convertToBson(flag.getValue()));
        return document;
    }

    private static BsonValue convertToBson(Object value) {
        if (value == null)
            return BsonNull.VALUE;
        else if (value instanceof String)
            return new BsonString((String) value);
        else if (value instanceof Boolean)
            return new BsonBoolean((boolean) value);
        else if (value instanceof Byte)
            return new BsonInt32((byte) value);
        else if (value instanceof Short)
            return new BsonInt32((short) value);
        else if (value instanceof Integer)
            return new BsonInt32((int) value);
        else if (value instanceof Long)
            return new BsonInt64((long) value);
        else if (value instanceof Float)
            return new BsonDouble((float) value);
        else if (value instanceof Double)
            return new BsonDouble((double) value);
        else if (value instanceof Location)
            return toBson((Location) value);
        else if (value instanceof Vector)
            return toBson((Vector) value);
        else if (value instanceof Iterable)
            return iterableToBson((Iterable<?>) value);
        else if (value instanceof Enum)
            return new BsonString(((Enum<?>) value).name());
        else
            throw new UnsupportedOperationException("Invalid flag type: " + value.getClass());
    }

    private static BsonValue iterableToBson(Iterable<?> collection) {
        ImmutableList.Builder<BsonValue> builder = ImmutableList.builder();
        for (Object value : collection)
            builder.add(convertToBson(value));
        return new BsonArray(builder.build());
    }

    private static BsonArray blockVector2DsToBson(Iterable<BlockVector2D> blockVector2Ds) {
        ImmutableList.Builder<BsonValue> builder = ImmutableList.builder();
        for (BlockVector2D blockVector2D : blockVector2Ds)
            builder.add(new BsonDocument().append("x", new BsonInt32(blockVector2D.getBlockX())).append("z", new BsonInt32(blockVector2D.getBlockZ())));
        return new BsonArray(builder.build());
    }

    private static List<BlockVector2D> documentToBlockVector2D(Iterable<?> documents) {
        ImmutableList.Builder<BlockVector2D> builder = ImmutableList.builder();
        for (Document document : Iterables.filter(documents, Document.class)) {
            builder.add(new BlockVector2D(
                    document.getInteger("x"),
                    document.getInteger("z")
            ));
        }
        return builder.build();
    }
}
