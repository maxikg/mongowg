package de.maxikg.mongowg.utils;

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
import org.bson.Document;
import org.bukkit.GameMode;
import org.bukkit.entity.EntityType;
import org.junit.Assert;
import org.junit.Test;

import java.util.Set;
import java.util.UUID;

public class DataUtilsTest {

    @Test
    public void testCuboidSerializationProcess() {
        ProtectedCuboidRegion region = new ProtectedCuboidRegion("test_region_cuboid", new BlockVector(0, 0, 0), new BlockVector(42, 42, 42));
        fillRegion(region);
        String json = DataUtils.toBson(region).toJson();

        Document document = Document.parse(json);
        ProtectedRegion other = DataUtils.toProtectedRegion(document);

        Assert.assertEquals(region, other);
    }

    @Test
    public void testPolygonSerializationProcess() {
        ProtectedPolygonalRegion region = new ProtectedPolygonalRegion("test_region_polygonal", ImmutableList.of(new BlockVector2D(0, 0), new BlockVector2D(42, 0), new BlockVector2D(0, 42)), 0, 42);
        fillRegion(region);
        String json = DataUtils.toBson(region).toJson();

        Document document = Document.parse(json);
        ProtectedRegion other = DataUtils.toProtectedRegion(document);

        Assert.assertEquals(region, other);
    }

    @Test
    public void testGlobalSerializationProcess() {
        GlobalProtectedRegion region = new GlobalProtectedRegion("test_region_polygonal");
        fillRegion(region);
        String json = DataUtils.toBson(region).toJson();

        Document document = Document.parse(json);
        ProtectedRegion other = DataUtils.toProtectedRegion(document);

        Assert.assertEquals(region, other);
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
