package de.maxikg.mongowg.model;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;

/**
 * Allows to store a {@link ProtectedRegion} and a {@code string} as a parent's name.
 */
public class ProcessingProtectedRegion {

    private final ProtectedRegion region;
    private final String parent;
    private String world;

    public ProcessingProtectedRegion(ProtectedRegion region, String parent, String world) {
        this.region = Preconditions.checkNotNull(region, "region must be not null.");
        this.parent = parent;
        this.world = Preconditions.checkNotNull(world, "world must be not null.");
    }

    public ProcessingProtectedRegion(ProtectedRegion region, String world) {
        this(region, extractParent(region), world);
    }

    public ProtectedRegion getRegion() {
        return region;
    }

    public String getParent() {
        return parent;
    }

    public String getWorld() {
        return world;
    }

    private static String extractParent(ProtectedRegion region) {
        ProtectedRegion parent = region.getParent();
        return parent != null ? parent.getId() : null;
    }

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

    @Override
    public int hashCode() {
        return Objects.hashCode(getRegion(), getParent(), getWorld());
    }
}
