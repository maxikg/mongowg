package de.maxikg.mongowg;

import com.google.common.base.Preconditions;
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
import org.bukkit.event.Listener;

import java.util.logging.Level;
import java.util.logging.Logger;

public class WorldGuardOpLogHandler implements OpLogHandler, Listener {

    private static final Logger LOGGER = Logger.getLogger(WorldGuardOpLogHandler.class.getName());

    private final Codec<ProcessingProtectedRegion> processingProtectedRegionCodec;
    private final RegionStorageAdapter storageAdapter;
    private final WorldGuardPlugin worldGuard;

    public WorldGuardOpLogHandler(Codec<ProcessingProtectedRegion> processingProtectedRegionCodec, RegionStorageAdapter storageAdapter, WorldGuardPlugin worldGuard) {
        this.processingProtectedRegionCodec = Preconditions.checkNotNull(processingProtectedRegionCodec, "processingProtectedRegionCodec must be not null.");
        this.storageAdapter = Preconditions.checkNotNull(storageAdapter, "storageAdapter must be not null.");
        this.worldGuard = Preconditions.checkNotNull(worldGuard, "worldGuard must be not null.");
    }

    @Override
    public void onCreate(BsonDocument createdDocument) {
        RegionStorageAdapter.RegionPath path = storageAdapter.resolvePath(createdDocument.getObjectId("_id").getValue());
        RegionManager regionManager = getRegionManager(path);
        if (regionManager != null)
            regionManager.addRegion(read(createdDocument).getRegion());
    }

    @Override
    public void onUpdate(ObjectId updatedObject) {
        ProcessingProtectedRegion region = storageAdapter.load(updatedObject);
        RegionManager regionManager = getRegionManager(region.getWorld());
        if (regionManager != null) {
            ProtectedRegion protectedRegion = region.getRegion();
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

    @Override
    public void onDelete(ObjectId deletedObject) {
        RegionStorageAdapter.RegionPath path = storageAdapter.resolvePath(deletedObject);
        RegionManager regionManager = getRegionManager(path);
        if (regionManager != null)
            regionManager.removeRegion(path.getId());
    }

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
}
