package net.silthus.regions.entities;

import io.ebean.Finder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.silthus.ebean.BaseEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Optional;
import java.util.UUID;

@Entity
@Table(name = "rcregions_region_signs")
@Getter
@Setter
@Accessors(fluent = true)
public class RegionSign extends BaseEntity {

    public static Optional<RegionSign> of(Location location) {

        if (location.getWorld() == null) {
            return Optional.empty();
        }

        return find.query().where()
                .eq("x", location.getBlockX())
                .eq("y", location.getBlockY())
                .eq("z", location.getBlockZ())
                .eq("world_id", location.getWorld().getUID())
                .findOneOrEmpty();
    }

    public static final Finder<UUID, RegionSign> find = new Finder<>(RegionSign.class);

    @ManyToOne
    private Region region;
    private int x;
    private int y;
    private int z;
    private UUID worldId;
    private String world;

    public RegionSign() {
    }

    public RegionSign(Region region, Block block) {
        this.region = region;
        this.x = block.getX();
        this.y = block.getY();
        this.z = block.getZ();
        world(block.getWorld());
    }

    public RegionSign world(String world) {
        this.world = world;
        return this;
    }

    public RegionSign world(World world) {
        worldId(world.getUID());
        world(world.getName());
        return this;
    }

    public Optional<Block> block() {
        return Optional.ofNullable(Bukkit.getWorld(worldId))
                .map(world -> world.getBlockAt(x, y, z));
    }

    public Optional<Sign> sign() {
        return block()
                .filter(block -> block.getState() instanceof Sign)
                .map(block -> (Sign)block.getState());
    }
}
