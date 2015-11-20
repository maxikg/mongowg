package de.maxikg.mongowg.wg.storage;

import com.google.common.base.Preconditions;
import com.sk89q.worldguard.protection.managers.RegionDifference;
import com.sk89q.worldguard.protection.managers.storage.RegionDatabase;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.maxikg.mongowg.RegionStorageAdapter;

import java.util.Set;

/**
 * The {@code RegionDatabase} implementation for MongoWG.
 */
public class MongoRegionDatabase implements RegionDatabase {

    private final RegionStorageAdapter storageAdapter;
    private final String world;

    /**
     * Constructor.
     *
     * @param storageAdapter The {@link RegionStorageAdapter} which should used
     * @param world The name of the world
     */
    public MongoRegionDatabase(RegionStorageAdapter storageAdapter, String world) {
        this.storageAdapter = Preconditions.checkNotNull(storageAdapter, "storageAdapter must be not null.");
        this.world = Preconditions.checkNotNull(world, "world must be not null.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<ProtectedRegion> loadAll() throws StorageException {
        return storageAdapter.loadAll(world);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveAll(Set<ProtectedRegion> set) throws StorageException {
        storageAdapter.saveAll(world, set);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveChanges(RegionDifference regionDifference) throws StorageException {
        storageAdapter.saveChanges(world, regionDifference);
    }
}
