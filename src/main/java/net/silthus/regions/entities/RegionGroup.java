package net.silthus.regions.entities;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.exlll.configlib.annotation.ConfigurationElement;
import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.annotation.DbJson;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.silthus.regions.Cost;
import net.silthus.regions.RegionManager;
import net.silthus.regions.RegionsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import javax.annotation.Nullable;
import javax.persistence.*;
import java.lang.ref.WeakReference;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "rcregions_region_groups")
@Getter
@Setter
@Accessors(fluent = true)
@ConfigurationElement
public class RegionGroup extends Model {

    public static RegionGroup getDefault() {
        return getOrCreate("default");
    }

    public static final Finder<String, RegionGroup> find = new Finder<>(RegionGroup.class);

    public static Optional<RegionGroup> of(String identifier) {

        return Optional.ofNullable(find.byId(identifier));
    }

    public static Optional<RegionGroup> ofWorldGuardRegion(@Nullable ProtectedRegion region) {

        if (region == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(find.query()
                .where().eq("world_guard_region", region.getId())
                .findOne()
        );
    }

    public static RegionGroup getOrCreate(String identifier) {

        RegionGroup regionGroup = find.byId(identifier);

        if (regionGroup == null) {
            regionGroup = new RegionGroup(identifier);
            regionGroup.save();
        }

        return regionGroup.loadCosts(RegionsPlugin.instance().getRegionManager());
    }

    @Id
    private String identifier;
    private String name;
    private String description;
    private String world;
    private String worldGuardRegion;

    RegionGroup(String identifier) {

        this.identifier = identifier;
    }

    public RegionGroup() {
    }

    @Version
    Long version;

    @WhenCreated
    Instant whenCreated;

    @WhenModified
    Instant whenModified;

    @OneToMany
    private List<Region> regions = new ArrayList<>();
    @DbJson
    private Map<String, Object> costsConfig = new HashMap<>();
    @Transient
    private List<Cost> costs = new ArrayList<>();
    @Transient
    private WeakReference<ProtectedRegion> wgRegion;

    public RegionGroup load(RegionManager regionManager, ConfigurationSection config) {

        name(config.getString("name", identifier()));
        description(config.getString("description", ""));
        world(config.getString("world", "world"));
        worldGuardRegion(config.getString("worldguard-region"));

        ConfigurationSection costsSection = config.getConfigurationSection("costs");
        if (costsSection != null) {
            costsConfig = new HashMap<>();
            costsSection.getKeys(true).forEach(s -> costsConfig.put(s, costsSection.get(s)));
            save();

            loadCosts(regionManager, costsSection);
        }

        return this;
    }

    public Optional<ProtectedRegion> worldGuardRegion() {

        if (wgRegion != null && wgRegion.get() != null) {
            return Optional.ofNullable(wgRegion.get());
        }

        World world = Bukkit.getWorld(this.world);
        if (world == null) {
            return Optional.empty();
        }

        com.sk89q.worldguard.protection.managers.RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(new BukkitWorld(world));
        if (regionManager == null) {
            return Optional.empty();
        }

        ProtectedRegion region = regionManager.getRegion(worldGuardRegion);
        if (region != null) {
            wgRegion = new WeakReference<>(region);
        }

        return Optional.ofNullable(region);
    }

    /**
     * Loads the costs objects based on the configuration data stored in the database.
     *
     * @param regionManager the regionmanager used to loaded the costs
     */
    RegionGroup loadCosts(RegionManager regionManager) {

        if (costsConfig == null || costsConfig.isEmpty()) {
            return this;
        }

        if (!costs().isEmpty()) {
            return this;
        }

        MemoryConfiguration costsSection = new MemoryConfiguration();
        for (Map.Entry<String, Object> entry : costsConfig().entrySet()) {
            costsSection.set(entry.getKey(), entry.getValue());
        }
        loadCosts(regionManager, costsSection);

        return this;
    }

    RegionGroup loadCosts(RegionManager regionManager, ConfigurationSection costsSection) {

        if (costsSection == null) return this;

        costs.clear();

        for (String costsKey : costsSection.getKeys(false)) {
            regionManager.getCost(costsKey, costsSection.getConfigurationSection(costsKey))
                    .ifPresent(costs::add);
        }

        return this;
    }

    public List<Region> playerRegions(RegionPlayer player) {

        return regions().stream()
                .filter(region -> region.owner().equals(player))
                .collect(Collectors.toList());
    }
}
