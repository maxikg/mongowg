package de.maxikg.mongowg;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.mongodb.Block;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.sk89q.worldguard.protection.managers.RegionDifference;
import com.sk89q.worldguard.protection.managers.storage.RegionDatabaseUtils;
import com.sk89q.worldguard.protection.managers.storage.StorageException;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.maxikg.mongowg.model.ProcessingProtectedRegion;
import de.maxikg.mongowg.utils.ConcurrentUtils;
import de.maxikg.mongowg.utils.OperationResultCallback;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class RegionStorageAdapter {

    public static final String COLLECTION_NAME = "regions";

    private final Map<ObjectId, RegionPath> idToRegion = Maps.newConcurrentMap();
    private final MongoDatabase database;

    public RegionStorageAdapter(MongoDatabase database) {
        this.database = Preconditions.checkNotNull(database, "database must be not null.");
    }

    public RegionPath resolvePath(ObjectId id) {
        return idToRegion.get(id);
    }

    public ProcessingProtectedRegion load(ObjectId id) {
        final CountDownLatch waiter = new CountDownLatch(1);
        final AtomicReference<ProcessingProtectedRegion> result = new AtomicReference<>();
        final AtomicReference<Throwable> lastError = new AtomicReference<>();
        getCollection().find(Filters.eq("_id", id)).first(new SingleResultCallback<ProcessingProtectedRegion>() {
            @Override
            public void onResult(ProcessingProtectedRegion region, Throwable throwable) {
                result.set(region);
                lastError.set(throwable);
                waiter.countDown();
            }
        });
        Throwable realLastError = lastError.get();
        if (realLastError != null)
            Throwables.propagate(realLastError);
        ConcurrentUtils.safeAwait(waiter);
        return result.get();
    }

    public Set<ProtectedRegion> loadAll(final String world) throws StorageException {
        final CountDownLatch waiter = new CountDownLatch(1);
        final AtomicReference<Throwable> lastError = new AtomicReference<>();
        final ConcurrentMap<String, ProtectedRegion> regions = new MapMaker().makeMap();
        final ConcurrentMap<ProtectedRegion, String> parents = new MapMaker().makeMap();
        getCollection().find(Filters.eq("world", world)).forEach(
                new Block<ProcessingProtectedRegion>() {
                    @Override
                    public void apply(ProcessingProtectedRegion region) {
                        if (!world.equals(region.getWorld()))
                            return;
                        ProtectedRegion protectedRegion = region.getRegion();
                        idToRegion.put(region.getDatabaseId(), new RegionPath(world, protectedRegion.getId()));
                        regions.putIfAbsent(protectedRegion.getId(), protectedRegion);
                        String parent = region.getParent();
                        if (parent != null)
                            parents.putIfAbsent(protectedRegion, parent);
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

    public void saveAll(final String world, Set<ProtectedRegion> set) throws StorageException {
        MongoCollection<ProcessingProtectedRegion> collection = getCollection();
        final AtomicReference<Throwable> lastError = new AtomicReference<>();
        final CountDownLatch waiter = new CountDownLatch(set.size());
        for (ProtectedRegion region : set) {
            collection.updateOne(
                    Filters.and(Filters.eq("name", region.getId()), Filters.eq("world", world)),
                    new Document("$set", new ProcessingProtectedRegion(region, world)),
                    new UpdateOptions().upsert(true),
                    OperationResultCallback.<UpdateResult>create(lastError, waiter)
            );
        }
        ConcurrentUtils.safeAwait(waiter);
        Throwable realLastError = lastError.get();
        if (realLastError != null)
            throw new StorageException("An error occurred while saving or updating in MongoDB.", realLastError);
    }

    public void saveChanges(final String world, RegionDifference regionDifference) throws StorageException {
        MongoCollection<ProcessingProtectedRegion> collection = getCollection();
        Set<ProtectedRegion> changed = regionDifference.getChanged();
        Set<ProtectedRegion> removed = regionDifference.getRemoved();
        final AtomicReference<Throwable> lastError = new AtomicReference<>();
        final CountDownLatch waiter = new CountDownLatch(changed.size() + removed.size());
        for (ProtectedRegion region : regionDifference.getChanged()) {
            collection.updateOne(
                    Filters.and(Filters.eq("name", region.getId()), Filters.eq("world", world)),
                    new Document("$set", new ProcessingProtectedRegion(region, world)),
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

    private MongoCollection<ProcessingProtectedRegion> getCollection() {
        return database.getCollection(COLLECTION_NAME, ProcessingProtectedRegion.class);
    }

    public static class RegionPath {

        private final String world;
        private final String id;

        private RegionPath(String world, String id) {
            this.world = world;
            this.id = id;
        }

        public String getWorld() {
            return world;
        }

        public String getId() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            RegionPath that = (RegionPath) o;
            return Objects.equal(world, that.world) &&
                    Objects.equal(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(world, id);
        }
    }
}
