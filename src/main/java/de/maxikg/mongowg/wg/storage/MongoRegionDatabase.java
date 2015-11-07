package de.maxikg.mongowg.wg.storage;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import com.mongodb.Block;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.sk89q.worldguard.protection.managers.RegionDifference;
import com.sk89q.worldguard.protection.managers.storage.RegionDatabase;
import com.sk89q.worldguard.protection.managers.storage.RegionDatabaseUtils;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.maxikg.mongowg.utils.DataUtils;
import de.maxikg.mongowg.utils.ConcurrentUtils;
import de.maxikg.mongowg.utils.OperationResultCallback;
import org.bson.Document;

import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The {@code RegionDatabase} implementation for MongoWG.
 */
public class MongoRegionDatabase implements RegionDatabase {

    public static final String COLLECTION_NAME = "regions";

    private final MongoDatabase database;
    private final String world;

    /**
     * Constructor.
     *
     * @param database The {@link MongoDatabase} which should used
     * @param world The name of the world
     */
    public MongoRegionDatabase(MongoDatabase database, String world) {
        this.database = Preconditions.checkNotNull(database, "database must be not null.");
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
        final CountDownLatch waiter = new CountDownLatch(1);
        final AtomicReference<Throwable> lastError = new AtomicReference<>();
        final ConcurrentMap<String, ProtectedRegion> regions = new MapMaker().makeMap();
        final ConcurrentMap<ProtectedRegion, String> parents = new MapMaker().makeMap();
        getCollection().find(Filters.eq("world", world)).forEach(
                new Block<Document>() {
                    @Override
                    public void apply(Document document) {
                        ProtectedRegion region = DataUtils.toProtectedRegion(document);
                        regions.putIfAbsent(region.getId(), region);
                        String parent = document.getString("parent");
                        if (parent != null)
                            parents.putIfAbsent(region, parent);
                    }
                },
                OperationResultCallback.<Void>create(lastError, waiter)
        );
        ConcurrentUtils.safeAwait(waiter);
        Throwable realLastError = lastError.get();
        if (realLastError != null)
            throw new StorageException("An error occurred while saving or updating in MongoDB.", realLastError);
        RegionDatabaseUtils.relinkParents(regions, parents);
        return ImmutableSet.copyOf(regions.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveAll(Set<ProtectedRegion> set) throws StorageException {
        MongoCollection<Document> collection = getCollection();
        final AtomicReference<Throwable> lastError = new AtomicReference<>();
        final CountDownLatch waiter = new CountDownLatch(set.size());
        for (ProtectedRegion region : set) {
            collection.updateOne(
                    Filters.and(Filters.eq("name", region.getId()), Filters.eq("world", world)),
                    new Document("$set", DataUtils.toBson(region)),
                    new UpdateOptions().upsert(true),
                    OperationResultCallback.<UpdateResult>create(lastError, waiter)
            );
        }
        ConcurrentUtils.safeAwait(waiter);
        Throwable realLastError = lastError.get();
        if (realLastError != null)
            throw new StorageException("An error occurred while saving or updating in MongoDB.", realLastError);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void saveChanges(RegionDifference regionDifference) throws StorageException {
        MongoCollection<Document> collection = getCollection();
        Set<ProtectedRegion> changed = regionDifference.getChanged();
        Set<ProtectedRegion> removed = regionDifference.getRemoved();
        final AtomicReference<Throwable> lastError = new AtomicReference<>();
        final CountDownLatch waiter = new CountDownLatch(changed.size() + removed.size());
        for (ProtectedRegion region : regionDifference.getChanged()) {
            collection.updateOne(
                    Filters.and(Filters.eq("name", region.getId()), Filters.eq("world", world)),
                    new Document("$set", DataUtils.toBson(region)),
                    new UpdateOptions().upsert(true),
                    OperationResultCallback.<UpdateResult>create(lastError, waiter)
            );
        }

        for (ProtectedRegion region : regionDifference.getRemoved()) {
            collection.deleteOne(
                    Filters.and(Filters.eq("name", region.getId()), Filters.eq("world", world)),
                    OperationResultCallback.<DeleteResult>create(lastError, waiter)
            );
        }

        ConcurrentUtils.safeAwait(waiter);
        Throwable realLastError = lastError.get();
        if (realLastError != null)
            throw new StorageException("An error occurred while saving or updating in MongoDB.", realLastError);
    }

    private MongoCollection<Document> getCollection() {
        return database.getCollection(COLLECTION_NAME);
    }
}
