package net.silthus.regions.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.silthus.ebean.BaseEntity;
import org.bukkit.World;
import org.bukkit.block.Block;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "sregions_region_signs")
@Getter
@Setter
@Accessors(fluent = true)
public class RegionSign extends BaseEntity {

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
}
