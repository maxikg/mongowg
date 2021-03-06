package de.maxikg.mongowg.wg.storage;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.mongodb.async.client.MongoDatabase;
import com.sk89q.worldguard.protection.managers.storage.RegionDatabase;
import com.sk89q.worldguard.protection.managers.storage.RegionDriver;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import de.maxikg.mongowg.RegionStorageAdapter;
import org.bukkit.Server;
import org.bukkit.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * The {@code RegionDriver} implementation for MongoWG.
 */
public class MongoRegionDriver implements RegionDriver {

    private static final Function<World, String> WORLD_NAME = new Function<World, String>() {
        @Nullable
        @Override
        public String apply(@Nullable World input) {
            return Preconditions.checkNotNull(input, "input must be not null.").getName();
        }
    };

    private final Server server;
    private final RegionStorageAdapter storageAdapter;

    /**
     * Constructor.
     *
     * @param server The {@link Server} on which this {@code RegionDriver} acts
     * @param storageAdapter The {@link RegionStorageAdapter} which should be used
     */
    public MongoRegionDriver(Server server, RegionStorageAdapter storageAdapter) {
        this.server = Preconditions.checkNotNull(server, "server must be not null.");
        this.storageAdapter = Preconditions.checkNotNull(storageAdapter, "storageAdapter must be not null.");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RegionDatabase get(String name) {
        return new MongoRegionDatabase(storageAdapter, name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<RegionDatabase> getAll() throws StorageException {
        ImmutableList.Builder<RegionDatabase> builder = ImmutableList.builder();
        for (String world : Iterables.transform(server.getWorlds(), WORLD_NAME))
            builder.add(get(world));
        return builder.build();
    }
}
