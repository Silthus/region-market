package net.silthus.regions.entities;

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
import net.silthus.regions.limits.Limit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
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

    public static RegionGroup getOrCreate(String identifier) {

        RegionGroup regionGroup = find.byId(identifier);

        if (regionGroup == null) {
            regionGroup = new RegionGroup(identifier);
            regionGroup.save();
        }

        return regionGroup.loadCosts(RegionsPlugin.getPlugin(RegionsPlugin.class).getRegionManager());
    }

    @Id
    private String identifier;
    private String name;
    private String description;

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

    public RegionGroup load(RegionManager regionManager, ConfigurationSection config) {

        name(config.getString("name", identifier()));
        description(config.getString("description", ""));

        ConfigurationSection costsSection = config.getConfigurationSection("costs");
        if (costsSection != null) {
            costsConfig = new HashMap<>();
            costsSection.getKeys(true).forEach(s -> costsConfig.put(s, costsSection.get(s)));
            save();

            loadCosts(regionManager, costsSection);
        }

        return this;
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
