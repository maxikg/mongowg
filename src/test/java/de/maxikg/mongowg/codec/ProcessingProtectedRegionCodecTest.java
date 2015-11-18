package de.maxikg.mongowg.codec;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.BlockVector2D;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.maxikg.mongowg.model.ProcessingProtectedRegion;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.DocumentCodecProvider;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.ValueCodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.json.JsonReader;
import org.bson.json.JsonWriter;
import org.bukkit.GameMode;
import org.bukkit.entity.EntityType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Set;
import java.util.UUID;

public class ProcessingProtectedRegionCodecTest {

    private ProcessingProtectedRegionCodec codec;

    @Before
    public void prepare() {
        CodecRegistry codecRegistry = CodecRegistries.fromRegistries(
                CodecRegistries.fromProviders(new ValueCodecProvider(), new DocumentCodecProvider()),
                CodecRegistries.fromCodecs(new BlockVector2DCodec(), new BlockVectorCodec(), new DefaultDomainCodec())
        );
        codec = new ProcessingProtectedRegionCodec(codecRegistry);
    }

    @Test
    public void testCodecForCuboid() throws IOException {
        ProtectedRegion region = new ProtectedCuboidRegion("cuboid", new BlockVector(4, 4, 4), new BlockVector(42, 42, 42));
        fillRegion(region);
        ProcessingProtectedRegion container = new ProcessingProtectedRegion(region, "world");

        ProcessingProtectedRegion other;
        try (StringWriter sw = new StringWriter()) {
            codec.encode(new JsonWriter(sw), container, EncoderContext.builder().build());
            other = codec.decode(new JsonReader(sw.toString()), DecoderContext.builder().build());
        }

        Assert.assertEquals(container, other);
    }

    @Test
    public void testCodecForGlobal() throws IOException {
        ProtectedRegion region = new GlobalProtectedRegion(GlobalProtectedRegion.GLOBAL_REGION);
        fillRegion(region);
        ProcessingProtectedRegion container = new ProcessingProtectedRegion(region, "world");

        ProcessingProtectedRegion other;
        try (StringWriter sw = new StringWriter()) {
            codec.encode(new JsonWriter(sw), container, EncoderContext.builder().build());
            other = codec.decode(new JsonReader(sw.toString()), DecoderContext.builder().build());
        }

        Assert.assertEquals(container, other);
    }

    @Test
    public void testCodecForPolygonal() throws IOException {
        ProtectedRegion region = new ProtectedPolygonalRegion("polygon", ImmutableList.of(new BlockVector2D(0, 0), new BlockVector2D(0, 4), new BlockVector2D(4, 4), new BlockVector2D(4, 0)), 0, 64);
        fillRegion(region);
        ProcessingProtectedRegion container = new ProcessingProtectedRegion(region, "world");

        ProcessingProtectedRegion other;
        try (StringWriter sw = new StringWriter()) {
            codec.encode(new JsonWriter(sw), container, EncoderContext.builder().build());
            other = codec.decode(new JsonReader(sw.toString()), DecoderContext.builder().build());
        }

        Assert.assertEquals(container, other);
    }

    private static void fillRegion(ProtectedRegion region) {
        region.setPriority(42);
        region.getOwners().addPlayer(UUID.randomUUID());
        region.getOwners().addGroup("owner_test_group");
        region.getMembers().addPlayer(UUID.randomUUID());
        region.getMembers().addGroup("member_test_group");
        region.setFlag(DefaultFlag.ENTRY_DENY_MESSAGE, "Test entry deny message");
        region.setFlag(DefaultFlag.BUILD, StateFlag.State.ALLOW);
        region.setFlag(DefaultFlag.BLOCK_PLACE, StateFlag.State.ALLOW);
        region.setFlag(DefaultFlag.NOTIFY_ENTER, true);
        region.setFlag(DefaultFlag.GAME_MODE, GameMode.CREATIVE);
        region.setFlag(DefaultFlag.ALLOWED_CMDS, (Set<String>) ImmutableSet.of("/test", "/mongowg"));
        region.setFlag(DefaultFlag.DENY_SPAWN, (Set<EntityType>) ImmutableSet.of(EntityType.HORSE));
        region.setFlag(DefaultFlag.HEAL_AMOUNT, 21);
        region.setFlag(DefaultFlag.MAX_HEAL, 42.0);
    }
}
