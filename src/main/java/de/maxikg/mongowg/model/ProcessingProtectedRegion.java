package de.maxikg.mongowg.model;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bson.types.ObjectId;

/**
 * Allows to store a {@link ProtectedRegion} and a {@code string} as a parent's name.
 */
public class ProcessingProtectedRegion {

    private final ProtectedRegion region;
    private final String parent;
    private ObjectId databaseId;
    private String world;

    /**
     * Constructor.
     *
     * @param region The {@link ProtectedRegion}
     * @param parent The id of the parent region
     * @param databaseId The {@link ObjectId} under which the object is saved in the database
     * @param world The name of the world
     * @throws NullPointerException Thrown, if {@code region} or {@code world} is null.
     */
    public ProcessingProtectedRegion(ProtectedRegion region, String parent, ObjectId databaseId, String world) {
        this.region = Preconditions.checkNotNull(region, "region must be not null.");
        this.parent = parent;
        this.databaseId = databaseId;
        this.world = Preconditions.checkNotNull(world, "world must be not null.");
    }

    /**
     * Constructor.
     *
     * @param region The {@link ProtectedRegion}
     * @param world The name of the world
     * @throws NullPointerException Thrown, if {@code region} or {@code world} is null.
     */
    public ProcessingProtectedRegion(ProtectedRegion region, String world) {
        this(region, extractParent(region), null, world);
    }

    /**
     * Returns the {@link ProtectedRegion}.
     *
     * @return The {@link ProtectedRegion}
     */
    public ProtectedRegion getRegion() {
        return region;
    }

    /**
     * Returns the name of the parent.
     *
     * @return The name of the parent or {@code null} if no parent is present
     */
    public String getParent() {
        return parent;
    }

    /**
     * Returns the {@link ObjectId} of the database object.
     *
     * @return The {@link ObjectId} or {@code null} of there is not data about it
     */
    public ObjectId getDatabaseId() {
        return databaseId;
    }

    /**
     * Returns the name of the world.
     *
     * @return The name of the world
     */
    public String getWorld() {
        return world;
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
        ProcessingProtectedRegion that = (ProcessingProtectedRegion) o;
        return Objects.equal(getRegion(), that.getRegion()) &&
                Objects.equal(getParent(), that.getParent()) &&
                Objects.equal(getWorld(), that.getWorld());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hashCode(getRegion(), getParent(), getWorld());
    }

    private static String extractParent(ProtectedRegion region) {
        ProtectedRegion parent = region.getParent();
        return parent != null ? parent.getId() : null;
    }
}
