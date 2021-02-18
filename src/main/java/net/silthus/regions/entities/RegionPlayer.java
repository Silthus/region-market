package net.silthus.regions.entities;

import io.ebean.Finder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.silthus.ebean.BaseEntity;
import net.silthus.regions.Constants;
import net.silthus.regions.RegionsPlugin;
import net.silthus.regions.limits.LimitCheckResult;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionAttachmentInfo;

import javax.persistence.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@Accessors(fluent = true)
@Table(name = "rcregions_players")
public class RegionPlayer extends BaseEntity {

    public static Optional<RegionPlayer> of(OfflinePlayer player) {
        return Optional.ofNullable(find.byId(player.getUniqueId()));
    }

    public static Optional<RegionPlayer> of(String name) {
        return find.query().where().eq("name", name).findOneOrEmpty();
    }

    public static RegionPlayer getOrCreate(OfflinePlayer player) {

        RegionPlayer regionPlayer = find.byId(player.getUniqueId());
        if (regionPlayer == null) {
            regionPlayer = new RegionPlayer(player);
            regionPlayer.save();
        }
        return regionPlayer;
    }

    public static final Finder<UUID, RegionPlayer> find = new Finder<>(RegionPlayer.class);

    private String name;
    @Transient
    private double priceMultiplier;
    private Instant lastOnline;

    @OneToMany
    private List<Region> regions = new ArrayList<>();

    @OneToMany(cascade = CascadeType.REMOVE)
    private List<OwnedRegion> ownedRegions = new ArrayList<>();

    @OneToMany(cascade = CascadeType.REMOVE, mappedBy = "seller")
    private List<Sale> sales = new ArrayList<>();

    RegionPlayer(OfflinePlayer player) {

        id(player.getUniqueId());
        name(player.getName());
    }

    public Optional<Player> getBukkitPlayer() {
        return Optional.ofNullable(Bukkit.getPlayer(id()));
    }

    public OfflinePlayer getOfflinePlayer() {

        return Bukkit.getOfflinePlayer(id());
    }

    public List<Sale> activeSales() {

        return sales().stream().filter(Sale::active).collect(Collectors.toList());
    }

    public double priceMultiplier() {

        if (priceMultiplier == 0) {
            priceMultiplier = getBukkitPlayer()
                    .map(Permissible::getEffectivePermissions).stream()
                    .flatMap(Collection::stream)
                    .map(PermissionAttachmentInfo::getPermission)
                    .filter(permission -> permission.startsWith(Constants.PRICE_MODIFIER_PREFIX))
                    .map(s -> s.replace(Constants.PRICE_MODIFIER_PREFIX, ""))
                    .map(Double::parseDouble)
                    .reduce((aDouble, aDouble2) -> aDouble * aDouble2)
                    .orElse(1.0);
        }

        return priceMultiplier;
    }

    public Collection<RegionGroup> regionGroups() {

        return regions().stream()
                .map(Region::group)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    public List<Region> regions(RegionGroup group) {

        if (group == null) return new ArrayList<>();

        return regions().stream()
                .filter(region -> group.equals(region.group()))
                .collect(Collectors.toList());
    }

    public LimitCheckResult checkLimits(@NonNull Region region) {

        return RegionsPlugin.instance()
                .getLimitsConfig()
                .getPlayerLimit(this)
                .map(playerLimit -> playerLimit.test(region))
                .orElse(new LimitCheckResult());
    }

    public boolean isOwner(Region region) {

        return regions().contains(region);
    }
}
