package net.silthus.regions.entities;

import io.ebean.annotation.WhenCreated;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.silthus.ebean.BaseEntity;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;

@Entity
@Getter
@Setter
@Accessors(fluent = true)
@Table(name = "rcregions_owned_regions")
public class OwnedRegion extends BaseEntity {

    @ManyToOne
    private Region region;
    @ManyToOne
    private RegionPlayer player;
    @WhenCreated
    private Instant start;
    private Instant end;

    public OwnedRegion(Region region, RegionPlayer player) {

        this.region = region;
        this.player = player;
    }

    public String name() {

        return player().name();
    }

    public boolean active() {
        return end() != null;
    }
}
