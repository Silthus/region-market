package net.silthus.regions.entities;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.ebean.Finder;
import io.ebean.annotation.DbEnumValue;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.silthus.ebean.BaseEntity;
import org.bukkit.Bukkit;
import org.bukkit.World;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Entity
@Getter
@Setter
@Accessors(fluent = true)
@Table(name = "sregions_regions")
public class Region extends BaseEntity {

    public static Region of(ProtectedRegion region) {

        return of(region.getId());
    }

    public static Region of(String worldGuardRegion) {

        return find.query().where().eq("world_guard_region", worldGuardRegion)
                .findOneOrEmpty()
                .orElseGet(() -> {
                    Region region = new Region(worldGuardRegion);
                    region.save();
                    return region;
                });
    }

    public static final Finder<UUID, Region> find = new Finder<>(Region.class);

    Region(String worldGuardRegion) {

        this.worldGuardRegion = worldGuardRegion;
        this.volume = volume();
        this.size = size();
    }

    private String worldGuardRegion;
    private UUID world;
    private String worldName;
    private RegionType regionType = RegionType.SELL;
    private PriceType priceType = PriceType.STATIC;
    private double price;
    private long volume;
    private long size;

    @ManyToOne
    private RegionGroup group;

    @ManyToOne
    private RegionPlayer owner;

    @OneToMany(cascade = CascadeType.REMOVE)
    private List<RegionAcl> acl = new ArrayList<>();

    @OneToMany(cascade = CascadeType.REMOVE)
    private List<RegionTransaction> transactions = new ArrayList<>();

    public Optional<ProtectedRegion> protectedRegion() {

        World world = Bukkit.getWorld(world());
        if (world == null) {
            return Optional.empty();
        }

        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(new BukkitWorld(world));
        if (regionManager == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(regionManager.getRegion(worldGuardRegion()));
    }

    /**
     * Gets the m3 volume of the region.
     * <p>This is the total amount of blocks inside the region.
     *
     * @return the m³ volume of the region
     */
    public long volume() {

        return protectedRegion().map(ProtectedRegion::volume).orElse(0);
    }

    /**
     * Gets the m2 size of the region.
     * <p>This is the amount of blocks in one plane of the region.
     *
     * @return the m² size of the region
     */
    public long size() {

        return protectedRegion().map(region -> region.volume() / (region.getMaximumPoint().getBlockY() - region.getMinimumPoint().getBlockY())).orElse(0);
    }

    public enum RegionType {
        SELL,
        RENT,
        CONTRACT,
        HOTEL;

        @DbEnumValue
        public String getValue() {

            return name();
        }
    }

    public enum PriceType {
        FREE,
        STATIC,
        DYNAMIC;

        @DbEnumValue
        public String getValue() {

            return name();
        }
    }
}
