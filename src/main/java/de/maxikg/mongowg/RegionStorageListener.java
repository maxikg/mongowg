package de.maxikg.mongowg;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.maxikg.mongowg.model.ProcessingProtectedRegion;

/**
 * Listen on storage events.
 */
public interface RegionStorageListener {

    /**
     * Called before a region will be updated (or created) in database.
     *
     * @param world The name of the world
     * @param region The {@link ProtectedRegion} which should be updated
     */
    void beforeDatabaseUpdate(String world, ProtectedRegion region);

    /**
     * Called after a region is saved to database.
     *
     * @param world The name of the world
     * @param resultedRegion The {@link ProcessingProtectedRegion} which is represents the region in the database
     */
    void afterDatabaseUpdate(String world, ProcessingProtectedRegion resultedRegion);

    /**
     * Called before a region is deleted.
     *
     * @param world The name of the world
     * @param region The {@link ProtectedRegion} which should be deleted
     */
    void beforeDatabaseDelete(String world, ProtectedRegion region);

    /**
     * Called after region is deleted.
     *
     * @param world The name of the world
     * @param deletedRegion The last known state of the region in database
     */
    void afterDatabaseDelete(String world, ProcessingProtectedRegion deletedRegion);
}
