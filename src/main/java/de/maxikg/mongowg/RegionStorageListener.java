package de.maxikg.mongowg;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.maxikg.mongowg.model.ProcessingProtectedRegion;

public interface RegionStorageListener {

    void beforeDatabaseUpdate(String world, ProtectedRegion region);

    void afterDatabaseUpdate(String world, ProcessingProtectedRegion resultedRegion);

    void beforeDatabaseDelete(String world, ProtectedRegion region);

    void afterDatabaseDelete(String world, ProcessingProtectedRegion deletedRegion);
}
