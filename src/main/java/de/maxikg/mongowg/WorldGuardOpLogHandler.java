package de.maxikg.mongowg;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.maxikg.mongowg.model.ProcessingProtectedRegion;
import de.maxikg.mongowg.oplog.OpLogHandler;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.types.ObjectId;
import org.bukkit.World;

import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Acts as a bridge between {@link RegionStorageAdapter} and {@link de.maxikg.mongowg.oplog.OpLogParser}.
 */
public class WorldGuardOpLogHandler implements OpLogHandler, RegionStorageListener {

    private static final Logger LOGGER = Logger.getLogger(WorldGuardOpLogHandler.class.getName());

    private final Set<RegionStorageAdapter.RegionPath> ignoreChanges = Collections.synchronizedSet(Sets.<RegionStorageAdapter.RegionPath>newHashSet());
    private final Codec<ProcessingProtectedRegion> processingProtectedRegionCodec;
    private final RegionStorageAdapter storageAdapter;
    private final WorldGuardPlugin worldGuard;

    /**
     * Constructor.
     *
     * @param processingProtectedRegionCodec The {@link Codec} which should be used to decode {@link ProcessingProtectedRegion}s
     * @param storageAdapter The {@link RegionStorageAdapter} to which the oplog changes should be applied
     * @param worldGuard The {@link WorldGuardPlugin} instance
     */
    public WorldGuardOpLogHandler(Codec<ProcessingProtectedRegion> processingProtectedRegionCodec, RegionStorageAdapter storageAdapter, WorldGuardPlugin worldGuard) {
        this.processingProtectedRegionCodec = Preconditions.checkNotNull(processingProtectedRegionCodec, "processingProtectedRegionCodec must be not null.");
        this.storageAdapter = Preconditions.checkNotNull(storageAdapter, "storageAdapter must be not null.");
        this.worldGuard = Preconditions.checkNotNull(worldGuard, "worldGuard must be not null.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeDatabaseUpdate(String world, ProtectedRegion region) {
        ignoreChanges.add(RegionStorageAdapter.RegionPath.create(world, region.getId()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterDatabaseUpdate(String world, ProcessingProtectedRegion result) {
        ignoreChanges.remove(RegionStorageAdapter.RegionPath.create(world, result.getRegion().getId()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeDatabaseDelete(String world, ProtectedRegion region) {
        ignoreChanges.add(RegionStorageAdapter.RegionPath.create(world, region.getId()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterDatabaseDelete(String world, ProcessingProtectedRegion result) {
        ignoreChanges.remove(RegionStorageAdapter.RegionPath.create(world, result.getRegion().getId()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(BsonDocument createdDocument) {
        RegionStorageAdapter.RegionPath path = RegionStorageAdapter.RegionPath.create(createdDocument.getString("world").getValue() , createdDocument.getString("name").getValue());
        if (checkIsIgnored(path))
            return;
        RegionManager regionManager = getRegionManager(path);
        if (regionManager != null)
            regionManager.addRegion(read(createdDocument).getRegion());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onUpdate(ObjectId updatedObject) {
        ProcessingProtectedRegion region = storageAdapter.load(updatedObject);
        ProtectedRegion protectedRegion = region.getRegion();
        if (checkIsIgnored(RegionStorageAdapter.RegionPath.create(region.getWorld(), protectedRegion.getId())))
            return;
        RegionManager regionManager = getRegionManager(region.getWorld());
        if (regionManager != null) {
            regionManager.removeRegion(protectedRegion.getId());
            regionManager.addRegion(protectedRegion);
            String parent = region.getParent();
            if (parent != null) {
                try {
                    protectedRegion.setParent(regionManager.getRegion(parent));
                } catch (ProtectedRegion.CircularInheritanceException ignore) {
                    LOGGER.warning("Circular inheritance for region " + protectedRegion.getId() + " of world " + region.getWorld() + ".");
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDelete(ObjectId deletedObject) {
        RegionStorageAdapter.RegionPath path = storageAdapter.resolvePath(deletedObject);
        RegionManager regionManager = getRegionManager(path);
        if (regionManager != null)
            regionManager.removeRegion(path.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onException(Throwable throwable) {
        LOGGER.log(Level.SEVERE, "An error occurred while reading oplog.", throwable);
    }

    private ProcessingProtectedRegion read(BsonDocument document) {
        return processingProtectedRegionCodec.decode(new BsonDocumentReader(document), DecoderContext.builder().build());
    }

    private RegionManager getRegionManager(RegionStorageAdapter.RegionPath path) {
        return getRegionManager(path.getWorld());
    }

    private RegionManager getRegionManager(String worldName) {
        World world = worldGuard.getServer().getWorld(worldName);
        if (world != null)
            return worldGuard.getRegionManager(world);
        return null;
    }

    private boolean checkIsIgnored(RegionStorageAdapter.RegionPath path) {
        boolean contains = ignoreChanges.contains(path);
        if (contains)
            ignoreChanges.remove(path);
        return contains;
    }
}
