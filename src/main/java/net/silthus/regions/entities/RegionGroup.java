package net.silthus.regions.entities;

import io.ebean.Finder;
import io.ebean.Model;
import io.ebean.annotation.WhenCreated;
import io.ebean.annotation.WhenModified;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.silthus.regions.Cost;
import net.silthus.regions.RegionManager;
import org.bukkit.configuration.ConfigurationSection;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "sregions_region_groups")
@Getter
@Setter
@Accessors(fluent = true)
public class RegionGroup extends Model {

    public static final Finder<String, RegionGroup> find = new Finder<>(RegionGroup.class);

    public static RegionGroup of(String identifier) {

        RegionGroup regionGroup = find.byId(identifier);

        if (regionGroup == null) {
            regionGroup = new RegionGroup(identifier);
            regionGroup.save();
        }

        return regionGroup;
    }

    @Id
    private String identifier;
    private String name;
    private String description;

    RegionGroup(String identifier) {

        this.identifier = identifier;
    }

    @Version
    Long version;

    @WhenCreated
    Instant whenCreated;

    @WhenModified
    Instant whenModified;

    @OneToMany
    private List<Region> regions = new ArrayList<>();
    @Transient
    private List<Cost> costs = new ArrayList<>();

    public void load(RegionManager regionManager, ConfigurationSection config) {

        name(config.getString("name", identifier()));
        description(config.getString("description", ""));

        ConfigurationSection costsSection = config.getConfigurationSection("costs");
        if (costsSection != null) {
            for (String costsKey : costsSection.getKeys(false)) {
                regionManager.getCost(costsKey, costsSection.getConfigurationSection(costsKey))
                        .ifPresent(costs::add);
            }
        }
    }

    public List<Region> playerRegions(RegionPlayer player) {

        return regions().stream()
                .filter(region -> region.owner().equals(player))
                .collect(Collectors.toList());
    }
}
