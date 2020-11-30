package net.silthus.regions.entities;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.managers.RegionManager;
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
import net.silthus.ebean.BaseEntity;
import net.silthus.regions.Cost;
import net.silthus.regions.MessageTags;
import net.silthus.regions.RegionsPlugin;
import net.silthus.regions.limits.Limit;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

    public static Optional<Region> of(Location location) {

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
    private RegionGroup group;

    @OneToOne(cascade = CascadeType.REMOVE)
    private OwnedRegion owner;

    @OneToMany(cascade = CascadeType.REMOVE)
    private List<OwnedRegion> previousOwners = new ArrayList<>();

    @OneToMany(cascade = CascadeType.REMOVE)
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

    public Region owner(RegionPlayer player) {

        OwnedRegion previousOwner = owner();
        if (previousOwner != null) {
            previousOwner.end(Instant.now());
            previousOwner.save();

            previousOwners.add(previousOwner);
        }

        this.owner = new OwnedRegion(this, player);
        this.owner.save();

        new RegionTransaction(this, player)
                .action(RegionTransaction.Action.CHANGE_OWNER)
                .data("previous_owner.id", previousOwner != null ? previousOwner.player().id() : null)
                .data("previous_owner.name", previousOwner != null ? previousOwner.name() : null)
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

    public Optional<RegionPlayer> player() {

        return Optional.ofNullable(owner()).map(OwnedRegion::player);
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

        return protectedRegion().map(region -> region.volume()
                / (region.getMaximumPoint().getBlockY() - region.getMinimumPoint().getBlockY()))
                .orElse(0);
    }

    public Cost.Result canBuy(RegionPlayer player) {

        if (status() == Status.OCCUPIED) {
            if (owner() == null) {
                return new Cost.Result(false, "Das Grundstück " + name() + " gehört bereits jemandem steht aber fehlerhaft in der Datenbank. " +
                        "Bitte kontaktiere einen Admin mit der Grundstücks ID: " + id(), Cost.ResultStatus.OTHER);
            } else if (owner() != null && owner().player().equals(player)) {
                return new Cost.Result(false, "Du besitzt das Grundstück " + name() + " bereits.", Cost.ResultStatus.OWNED_BY_SELF);
            } else {
                return new Cost.Result(false, "Das Grundstück " + name() + " gehört bereits " + owner().name(), Cost.ResultStatus.OWNED_BY_OTHER);
            }
        }

        Limit.Result limits = checkLimits(player);
        if (limits.reachedLimit()) {
            return new Cost.Result(false, limits.error(), Cost.ResultStatus.LIMITS_REACHED);
        }

        RegionsPlugin plugin = RegionsPlugin.instance();

        if (group() == null) {
            return new Cost.Result(plugin.getEconomy().has(player.getOfflinePlayer(), price),
                    "Du hast nicht genügend Geld! Du benötigst mindestens: " + plugin.getEconomy().format(price), price,
                    Cost.ResultStatus.NOT_ENOUGH_MONEY);
        } else {
            return group()
                    .loadCosts(plugin.getRegionManager())
                    .costs().stream()
                    .map(cost -> cost.check(this, player))
                    .reduce((result, result2) -> new Cost.Result(
                            result.success() && result2.success(),
                            result.error() + "\n" + result2.error(),
                            result.price() + result2.price(),
                            Cost.ResultStatus.COSTS_NOT_MET
                    )).orElse(new Cost.Result(true, null, Cost.ResultStatus.SUCCESS));
        }
    }

    private Limit.Result checkLimits(RegionPlayer player) {

        return RegionsPlugin.instance()
                .getLimitsConfig()
                .getPlayerLimit(player)
                .map(playerLimit -> playerLimit.test(this))
                .orElse(new Limit.Result(false, null, Limit.Type.NONE));
    }

    @Transactional
    public Cost.Result buy(@NonNull RegionsPlugin plugin, RegionPlayer player) {

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

    public String[] displayCosts() {
        return displayCosts(null);
    }

    public String[] displayCosts(@Nullable RegionPlayer player) {

        RegionsPlugin plugin = RegionsPlugin.instance();
        if (group() == null) {
            return new String[] {plugin.getEconomy().format(price)};
        } else {
            return group()
                    .loadCosts(plugin.getRegionManager())
                    .costs().stream()
                    .map(cost -> cost.display(this, player))
                    .toArray(String[]::new);
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
                RegionsPlugin plugin = RegionsPlugin.instance();
                Sign block = (Sign) blockAt.getState();
                if (status() != Status.OCCUPIED) {
                    block.setLine(0, ChatColor.GREEN + "[Grundstück]");
                    block.setLine(1, ChatColor.WHITE + name());
                    block.setLine(2, ChatColor.GREEN + "- Verfügbar -");
                    block.setLine(3, ChatColor.GREEN + "Preis: " + ChatColor.YELLOW + plugin.getEconomy().format(price()));
                } else {
                    block.setLine(0, ChatColor.RED + "[Grundstück]");
                    block.setLine(1, ChatColor.WHITE + name());
                    block.setLine(2, ChatColor.RED + "- Besitzer -");
                    block.setLine(3, ChatColor.YELLOW + owner().name());
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
                return owner() != null ? owner().name() : null;
            case ownerId:
            case playerId:
                return owner() != null ? owner().player().id() : null;
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
