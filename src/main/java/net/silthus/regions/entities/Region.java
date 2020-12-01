package net.silthus.regions.entities;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.ebean.ExpressionList;
import io.ebean.Finder;
import io.ebean.annotation.DbEnumValue;
import io.ebean.annotation.Transactional;
import lombok.Getter;
import lombok.NonNull;
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
import net.silthus.regions.limits.Limit;
import net.silthus.regions.util.MathUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

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

    @ManyToOne
    private RegionGroup group;

    @OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.EAGER)
    private List<OwnedRegion> owners = new ArrayList<>();

    @OneToMany(cascade = CascadeType.REMOVE, fetch = FetchType.EAGER)
    private List<RegionAcl> acl = new ArrayList<>();

    @OneToMany(cascade = CascadeType.REMOVE)
    private List<RegionTransaction> transactions = new ArrayList<>();

    @OneToMany(cascade = CascadeType.REMOVE)
    private List<RegionSign> signs = new ArrayList<>();

    public Region(String name) {

        this(null, name);
    }

    public Region(World world, String name) {

        this.world = world != null ? world.getUID() : null;
        this.worldName = world != null ? world.getName() : null;
        this.name = name;
        this.volume = volume();
        this.size = size();
        this.group = RegionGroup.getDefault();
    }

    public Optional<ProtectedRegion> protectedRegion() {

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

    public Optional<RegionPlayer> owner() {

        return Optional.ofNullable(owner);
    }

    public Optional<OwnedRegion> activeOwnedRegion() {

        return owners().stream()
                .filter(OwnedRegion::active)
                .findFirst();
    }

    public Region owner(RegionPlayer player) {

        Optional<OwnedRegion> previousOwner = activeOwnedRegion();

        previousOwner.ifPresent(ownedRegion -> {
            ownedRegion.end(Instant.now());
            ownedRegion.save();
        });

        OwnedRegion newOwner = new OwnedRegion(this, player);
        newOwner.save();
        owners.add(newOwner);

        owner = player;

        new RegionTransaction(this, player)
                .action(RegionTransaction.Action.CHANGE_OWNER)
                .data("previous_owner.id", previousOwner.map(ownedRegion -> ownedRegion.player().id()).orElse(null))
                .data("previous_owner.name", previousOwner.map(ownedRegion -> ownedRegion.player().name()).orElse(null))
                .data("new_owner.id", player.id())
                .data("new_owner.name", player.name())
                .save();
        save();

        protectedRegion().ifPresent(region -> {
            DefaultDomain defaultDomain = new DefaultDomain();
            defaultDomain.addPlayer(player.id());
            region.setOwners(defaultDomain);
        });

        return this;
    }

    /**
     * Gets the m3 volume of the region.
     * <p>This is the total amount of blocks inside the region.
     *
     * @return the m³ volume of the region
     */
    public long volume() {

        return protectedRegion().map(region -> {
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
    public long size() {

        return protectedRegion().map(region -> region.volume()
                / (region.getMaximumPoint().getBlockY() - region.getMinimumPoint().getBlockY()))
                .orElse(0);
    }

    public Cost.Result canBuy(@NonNull RegionPlayer player) {

        Optional<RegionPlayer> owner = owner();
        if (status() == Status.OCCUPIED) {
            if (owner.isEmpty()) {
                return new Cost.Result(false, "Das Grundstück " + name() + " gehört bereits jemandem steht aber fehlerhaft in der Datenbank. " +
                        "Bitte kontaktiere einen Admin mit der Grundstücks ID: " + id(), Cost.ResultStatus.OTHER);
            } else if (owner.get().equals(player)) {
                return new Cost.Result(false, "Du besitzt das Grundstück " + name() + " bereits.", Cost.ResultStatus.OWNED_BY_SELF);
            } else {
                return new Cost.Result(false, "Das Grundstück " + name() + " gehört bereits " + owner().map(RegionPlayer::name).orElse("jemandem."), Cost.ResultStatus.OWNED_BY_OTHER);
            }
        }

        Limit.Result limits = checkLimits(player);
        if (limits.reachedLimit()) {
            return new Cost.Result(false, limits.error(), Cost.ResultStatus.LIMITS_REACHED);
        }

        RegionsPlugin plugin = RegionsPlugin.instance();

        if (group() == null) {
            Economy economy = plugin.getEconomy();
            double balance = economy.getBalance(player.getOfflinePlayer());
            return new Cost.Result(economy.has(player.getOfflinePlayer(), price),
                    "Du hast nicht genügend Geld (" + economy.format(balance) + ")! Du benötigst mindestens: " + economy.format(price), price,
                    Cost.ResultStatus.NOT_ENOUGH_MONEY);
        } else {
            return group()
                    .loadCosts(plugin.getRegionManager())
                    .costs().stream()
                    .map(cost -> cost.check(this, player))
                    .reduce(Cost.Result::combine)
                    .orElse(new Cost.Result(true, null, Cost.ResultStatus.SUCCESS));
        }
    }

    private Limit.Result checkLimits(@Nullable RegionPlayer player) {

        return RegionsPlugin.instance()
                .getLimitsConfig()
                .getPlayerLimit(player)
                .map(playerLimit -> playerLimit.test(this))
                .orElse(new Limit.Result(false, null, Limit.Type.NONE));
    }

    @Transactional
    public Cost.Result buy(@NonNull RegionsPlugin plugin, @NonNull RegionPlayer player) {

        Cost.Result canBuy = canBuy(player);
        if (canBuy.failure()) {
            return canBuy;
        }

        double price = canBuy.price();

        plugin.getEconomy().withdrawPlayer(player.getOfflinePlayer(), price);

        owner(player);
        status(Status.OCCUPIED);
        new RegionTransaction(this, player)
                .action(RegionTransaction.Action.BUY)
                .data("price", price)
                .save();
        save();
        updateSigns();

        return new Cost.Result(true, null, price, Cost.ResultStatus.SUCCESS);
    }

    public List<Cost> costs() {

        if (group() == null) {
            return new ArrayList<>();
        } else {
            return group().costs();
        }
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
                    .loadCosts(plugin.getRegionManager())
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
                return volume();
            case regionSize:
                return size();
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
        ABADONED;

        @DbEnumValue
        public String getValue() {

            return name();
        }
    }
}
