package net.silthus.regions.entities;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.ebean.ExpressionList;
import io.ebean.Finder;
import io.ebean.annotation.DbEnumValue;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.wiefferink.interactivemessenger.processing.ReplacementProvider;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.milkbowl.vault.economy.Economy;
import net.silthus.ebean.BaseEntity;
import net.silthus.regions.Cost;
import net.silthus.regions.MessageTags;
import net.silthus.regions.Messages;
import net.silthus.regions.RegionsPlugin;
import net.silthus.regions.costs.MoneyCost;
import net.silthus.regions.costs.PriceDetails;
import net.silthus.regions.util.MathUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static net.silthus.regions.MessageTags.*;

@Entity
@Getter
@Setter
@Accessors(fluent = true)
@Table(name = "rcregions_regions")
public class Region extends BaseEntity implements ReplacementProvider {

    public static boolean exists(World world, ProtectedRegion protectedRegion) {

        return of(world, protectedRegion).isPresent();
    }

    public static Collection<Region> at(Location location) {
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(new BukkitWorld(location.getWorld()));

        if (regionManager == null) {
            return new ArrayList<>();
        }

        return regionManager.getApplicableRegions(BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ()))
                .getRegions().stream()
                .filter(protectedRegion -> !RegionsPlugin.instance().getPluginConfig().getIgnoredRegions().contains(protectedRegion.getId()))
                .map(Region::of)
                .map(region -> region.orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static Optional<Region> atSign(Location location) {

        return RegionSign.of(location).map(RegionSign::region);
    }

    public static Optional<Region> of(ProtectedRegion region) {
        return of(region.getId());
    }

    public static Optional<Region> of(World world, ProtectedRegion region) {

        return of(world, region.getId());
    }

    public static Optional<Region> of(String worldGuardRegion) {
        return of(null, worldGuardRegion);
    }

    public static Optional<Region> of(World world, String worldGuardRegion) {

        ExpressionList<Region> query = find.query().where();
        if (world != null) {
            query = query.eq("world", world.getUID())
                    .and();
        }
        return query.eq("name", worldGuardRegion)
                .findOneOrEmpty();
    }

    public static Region getOrCreate(World world, ProtectedRegion region) {

        return of(world, region).orElseGet(() -> {
            Region rg = new Region(world, region.getId());
            rg.save();
            return rg;
        });
    }

    public static final Finder<UUID, Region> find = new Finder<>(Region.class);

    private String name;

    private UUID world;
    private String worldName;
    private RegionType regionType = RegionType.SELL;
    private PriceType priceType = PriceType.STATIC;
    private Status status = Status.FREE;
    private double price;
    private double priceMultiplier = 1.0;
    private long volume;
    private long size;

    @ManyToOne
    private RegionPlayer owner;

    @ManyToOne(fetch = FetchType.EAGER)
    private RegionGroup group;

    @OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.EAGER)
    private List<OwnedRegion> owners = new ArrayList<>();

    @OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.EAGER)
    private List<RegionAcl> acl = new ArrayList<>();

    @OneToMany(cascade = CascadeType.REMOVE)
    private List<RegionTransaction> transactions = new ArrayList<>();

    @OneToMany(cascade = CascadeType.REMOVE)
    private List<RegionSign> signs = new ArrayList<>();

    @OneToMany(cascade = CascadeType.REMOVE)
    private List<Sale> sales = new ArrayList<>();

    public Region(String name) {

        this(null, name);
    }

    public Region(World world, String name) {

        this.world = world != null ? world.getUID() : null;
        this.worldName = world != null ? world.getName() : null;
        this.name = name;
        this.volume = calcVolume();
        this.size = calcSize();
        this.group = RegionGroup.getDefault();
    }

    public RegionGroup group() {

        if (this.group != null) {
            return this.group;
        }

        Optional<ProtectedRegion> region = worldGuardRegion();
        if (region.isPresent() && RegionsPlugin.instance().getPluginConfig().isAutoMapParent()) {
            RegionGroup.ofWorldGuardRegion(region.get().getParent()).ifPresent(g -> this.group = g);
        } else {
            this.group = RegionGroup.getDefault();
        }
        save();

        return this.group;
    }

    public Optional<Sale> activeSale() {

        return sales().stream().filter(Sale::active).findAny();
    }

    public Region priceType(PriceType priceType) {

        this.priceType = priceType;
        switch (priceType) {
            case FREE:
            case DYNAMIC:
                price(0);
                break;
            case STATIC:
                priceMultiplier(1.0);
                break;
        }
        save();

        return this;
    }

    public Optional<ProtectedRegion> worldGuardRegion() {

        World world = Bukkit.getWorld(world());
        if (world == null) {
            return Optional.empty();
        }

        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(new BukkitWorld(world));
        if (regionManager == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(regionManager.getRegion(name()));
    }

    public Region group(RegionGroup group) {

        this.group = group;
        if (group != null && group.priceType() != null) {
            priceType(group.priceType());
        }

        return this;
    }

    public Optional<RegionPlayer> owner() {

        return Optional.ofNullable(owner);
    }

    public Optional<OwnedRegion> activeOwnedRegion() {

        return owners().stream()
                .filter(OwnedRegion::active)
                .findFirst();
    }

    public Region owner(@Nullable RegionPlayer player) {

        Optional<OwnedRegion> previousOwner = activeOwnedRegion();

        previousOwner.ifPresent(ownedRegion -> {
            ownedRegion.end(Instant.now());
            ownedRegion.save();
        });

        OwnedRegion newOwner = new OwnedRegion(this, player);
        newOwner.save();
        owners.add(newOwner);

        owner = player;

        RegionTransaction.of(this, player, RegionTransaction.Action.CHANGE_OWNER)
                .data("previous_owner.id", previousOwner.map(ownedRegion -> ownedRegion.player().id()).orElse(null))
                .data("previous_owner.name", previousOwner.map(ownedRegion -> ownedRegion.player().name()).orElse(null))
                .data("new_owner.id", Optional.ofNullable(player).map(BaseEntity::id).orElse(null))
                .data("new_owner.name", Optional.ofNullable(player).map(RegionPlayer::name).orElse(null))
                .save();
        save();

        if (player != null) {
            worldGuardRegion().ifPresent(region -> {
                DefaultDomain defaultDomain = new DefaultDomain();
                defaultDomain.addPlayer(player.id());
                region.setOwners(defaultDomain);
                region.setMembers(new DefaultDomain());
            });
        }

        return this;
    }

    public Region volume(long volume) {
        this.volume = volume;
        this.size = calcSize();
        return this;
    }

    public Region size(long size) {
        this.size = size;
        worldGuardRegion().ifPresent(region -> {
            this.volume = size * (region.getMaximumPoint().getBlockY() - region.getMinimumPoint().getBlockY());
        });
        return this;
    }

    /**
     * Gets the m3 volume of the region.
     * <p>This is the total amount of blocks inside the region.
     *
     * @return the m³ volume of the region
     */
    private long calcVolume() {

        return worldGuardRegion().map(region -> {
            if (region instanceof ProtectedPolygonalRegion) {
                return (int) MathUtil.calculatePolygonalArea(region.getPoints());
            } else {
                return region.volume();
            }
        }).orElse(0);
    }

    /**
     * Gets the m2 size of the region.
     * <p>This is the amount of blocks in one plane of the region.
     *
     * @return the m² size of the region
     */
    private long calcSize() {

        return worldGuardRegion().map(region -> volume()
                / (region.getMaximumPoint().getBlockY() - region.getMinimumPoint().getBlockY()))
                .orElse(size);
    }

    public List<Cost> costs() {

        if (group() == null) {
            return new ArrayList<>();
        } else {
            return group().costs();
        }
    }

    public PriceDetails priceDetails(@Nullable RegionPlayer player) {

        return costs().stream()
                .filter(cost -> cost instanceof MoneyCost)
                .map(cost -> ((MoneyCost) cost).calculate(this, player))
                .findFirst()
                .orElse(new PriceDetails());
    }

    public BaseComponent[] displayCosts() {
        return displayCosts(null);
    }

    public BaseComponent[] displayCosts(@Nullable RegionPlayer player) {

        RegionsPlugin plugin = RegionsPlugin.instance();
        if (group() == null) {
            Economy economy = plugin.getEconomy();
            return new ComponentBuilder().append(economy.format(price)).color(ChatColor.AQUA).create();
        } else {
            return group()
                    .costs().stream()
                    .map(cost -> cost.display(this, player))
                    .reduce((baseComponents, baseComponents2) -> new ComponentBuilder()
                            .append(baseComponents).append("\n")
                            .append(baseComponents2).create())
                    .orElse(new BaseComponent[0]);
        }
    }

    public void updateSigns() {

        for (RegionSign sign : signs()) {
            World world = Bukkit.getWorld(sign.worldId());
            if (world == null) continue;
            Block blockAt = world.getBlockAt(sign.x(), sign.y(), sign.z());
            if (!(blockAt.getState() instanceof Sign)) {
                sign.delete();
            } else {
                Sign block = (Sign) blockAt.getState();
                String[] lines = Messages.formatRegionSign(this);
                for (int i = 0; i < lines.length; i++) {
                    block.setLine(i, lines[i]);
                }
                block.update(true);
            }
        }
    }

    @Override
    public void save() {

        super.save();
        updateSigns();
    }

    @Override
    public boolean delete() {

        for (RegionSign sign : signs()) {
            sign.block().ifPresent(block -> block.setType(Material.AIR));
            sign.delete();
        }
        return super.delete();
    }

    @Override
    public Object provideReplacement(String variable) {

        switch (variable) {
            case regionName:
                return name();
            case regionVolume:
                return calcVolume();
            case regionSize:
                return calcSize();
            case playerName:
            case MessageTags.owner:
                return owner().map(RegionPlayer::name).orElse(null);
            case ownerId:
            case playerId:
                return owner().map(BaseEntity::id).orElse(null);
            case MessageTags.price:
                return RegionsPlugin.instance().getEconomy().format(price());
            case priceraw:
                return price();
            case MessageTags.priceMultiplier:
                return priceMultiplier();
            case MessageTags.worldName:
                return worldName();
            case regionStatus:
                return status().getValue().toLowerCase();
            case MessageTags.regionType:
                return regionType().getValue().toLowerCase();
            case MessageTags.priceType:
                return priceType().getValue().toLowerCase();
            case groupName:
                return group() != null ? group().name() : null;
            case groupId:
                return group() != null ? group().identifier() : null;
            case groupDescription:
                return group() != null ? group().description() : null;
        }

        return null;
    }

    public double basePrice() {

        List<Cost> costs = costs();

        return costs.stream()
                .filter(cost -> cost instanceof MoneyCost)
                .map(cost -> (MoneyCost) cost)
                .map(moneyCost -> moneyCost.calculate(this))
                .map(PriceDetails::regionBasePrice)
                .reduce(Double::sum)
                .orElse(price() * priceMultiplier());
    }

    public boolean isOwner(Player player) {

        return owner != null && owner.id().equals(player.getUniqueId());
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

    public enum Status {
        FREE,
        OCCUPIED,
        ABADONED,
        FOR_SALE;

        @DbEnumValue
        public String getValue() {

            return name();
        }
    }
}
