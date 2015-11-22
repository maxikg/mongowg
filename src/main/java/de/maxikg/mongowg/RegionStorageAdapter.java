package de.maxikg.mongowg;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.mongodb.Block;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
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

/**
 * The adapter to save or retrieve regions from database.
 */
public class RegionStorageAdapter {

    /**
     * The name of the collections which contains regions.
     */
    public static final String COLLECTION_NAME = "regions";

    private static final Interner<RegionPath> REGION_PATH_CACHE = Interners.newWeakInterner();

    private final Map<ObjectId, RegionPath> idToRegion = Maps.newConcurrentMap();
    private final MongoDatabase database;
    private RegionStorageListener listener;

    /**
     * Constructor.
     *
     * @param database The {@link MongoDatabase} to use
     */
    public RegionStorageAdapter(MongoDatabase database) {
        this.database = Preconditions.checkNotNull(database, "database must be not null.");
    }

    /**
     * Returns the current configured {@link RegionStorageListener}.
     *
     * @return The {@link RegionStorageListener} or {@code null} if nothing is set
     */
    public RegionStorageListener getListener() {
        return listener;
    }

    /**
     * Set's a new {@link RegionStorageListener}.
     *
     * @param listener The new {@link RegionStorageListener} or {@code null} if no one is wanted
     */
    public void setListener(RegionStorageListener listener) {
        this.listener = listener;
    }

    /**
     * Resolves a given {@link ObjectId} to it's {@link RegionPath}.
     *
     * @param id The {@link ObjectId}
     * @return The {@link RegionPath} or {@code null} if the {@code id} is not known
     */
    public RegionPath resolvePath(ObjectId id) {
        return idToRegion.get(id);
    }

    /**
     * Load's the region with the given {@link ObjectId} from database.
     *
     * @param id The {@link ObjectId}
     * @return The {@link ProcessingProtectedRegion}
     */
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
                if (region != null)
                    idToRegion.put(region.getDatabaseId(), new RegionPath(region.getWorld(), region.getRegion().getId()));
            }
        });
        Throwable realLastError = lastError.get();
        if (realLastError != null)
            Throwables.propagate(realLastError);
        ConcurrentUtils.safeAwait(waiter);
        return result.get();
    }

    /**
     * Load all regions for a specified world.
     *
     * @param world The name of the world
     * @return A immutable {@link Set} of all {@link ProtectedRegion}s
     * @throws StorageException Thrown if something goes wrong during database query
     */
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
                        if (region != null)
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

    /**
     * Saves a set of {@link ProtectedRegion} for the specified world to database.
     *
     * @param world The name of the world
     * @param set The {@link Set} of regions
     * @throws StorageException Thrown if something goes wrong during database query
     */
    public void saveAll(final String world, Set<ProtectedRegion> set) throws StorageException {
        MongoCollection<ProcessingProtectedRegion> collection = getCollection();
        final AtomicReference<Throwable> lastError = new AtomicReference<>();
        final CountDownLatch waiter = new CountDownLatch(set.size());
        for (final ProtectedRegion region : set) {
            if (listener != null)
                listener.beforeDatabaseUpdate(world, region);
            collection.findOneAndUpdate(
                    Filters.and(Filters.eq("name", region.getId()), Filters.eq("world", world)),
                    new Document("$set", new ProcessingProtectedRegion(region, world)),
                    new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER),
                    OperationResultCallback.create(lastError, waiter, new UpdateCallback(world))
            );
        }
        ConcurrentUtils.safeAwait(waiter);
        Throwable realLastError = lastError.get();
        if (realLastError != null)
            throw new StorageException("An error occurred while saving or updating in MongoDB.", realLastError);
    }

    /**
     * Saves the given {@link RegionDifference} for the specified world to database.
     *
     * @param world The name oft the world
     * @param regionDifference The {@link RegionDifference} which should be saved
     * @throws StorageException Thrown if something goes wrong during database query
     */
    public void saveChanges(final String world, RegionDifference regionDifference) throws StorageException {
        MongoCollection<ProcessingProtectedRegion> collection = getCollection();
        Set<ProtectedRegion> changed = regionDifference.getChanged();
        Set<ProtectedRegion> removed = regionDifference.getRemoved();
        final AtomicReference<Throwable> lastError = new AtomicReference<>();
        final CountDownLatch waiter = new CountDownLatch(changed.size() + removed.size());
        for (ProtectedRegion region : regionDifference.getChanged()) {
            if (listener != null)
                listener.beforeDatabaseUpdate(world, region);
            collection.findOneAndUpdate(
                    Filters.and(Filters.eq("name", region.getId()), Filters.eq("world", world)),
                    new Document("$set", new ProcessingProtectedRegion(region, world)),
                    new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER),
                    OperationResultCallback.create(lastError, waiter, new UpdateCallback(world))
            );
        }

        for (ProtectedRegion region : regionDifference.getRemoved()) {
            if (listener != null)
                listener.beforeDatabaseDelete(world, region);
            collection.findOneAndDelete(
                    Filters.and(Filters.eq("name", region.getId()), Filters.eq("world", world)),
                    OperationResultCallback.create(lastError, waiter, new DeleteCallback(world))
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

    private class UpdateCallback implements SingleResultCallback<ProcessingProtectedRegion> {

        private final String world;

        public UpdateCallback(String world) {
            this.world = Preconditions.checkNotNull(world, "world must be not null.");
        }

        @Override
        public void onResult(ProcessingProtectedRegion result, Throwable throwable) {
            idToRegion.put(result.getDatabaseId(), RegionPath.create(result.getWorld(), result.getRegion().getId()));

            if (listener != null)
                listener.afterDatabaseUpdate(world, result);
        }
    }

    private class DeleteCallback implements SingleResultCallback<ProcessingProtectedRegion> {

        private final String world;

        public DeleteCallback(String world) {
            this.world = Preconditions.checkNotNull(world, "world must be not null.");
        }

        @Override
        public void onResult(ProcessingProtectedRegion result, Throwable throwable) {
            if (listener != null && result != null)
                listener.afterDatabaseDelete(world, result);

            idToRegion.remove(result.getDatabaseId());
        }
    }

    /**
     * A simple class which holds an world name and a region id.
     */
    public static class RegionPath {

        private final String world;
        private final String id;

        private RegionPath(String world, String id) {
            this.world = world;
            this.id = id;
        }

        /**
         * Returns the world name.
         *
         * @return The world name
         */
        public String getWorld() {
            return world;
        }

        /**
         * Returns the region id.
         *
         * @return The region id
         */
        public String getId() {
            return id;
        }

        /**
         * {@inheritDoc}
         */
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

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return Objects.hashCode(world, id);
        }

        /**
         * Create a new instance of {@link RegionPath}.
         *
         * @param world The name of the world
         * @param id The name of the region
         */
        public static RegionPath create(String world, String id) {
            return REGION_PATH_CACHE.intern(new RegionPath(world, id));
        }
    }
}
