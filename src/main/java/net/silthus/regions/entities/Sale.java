package net.silthus.regions.entities;

import io.ebean.Finder;
import io.ebean.annotation.DbEnumValue;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.silthus.ebean.BaseEntity;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
@Accessors(fluent = true)
@Entity
@Table(name = "rcregions_sales")
public class Sale extends BaseEntity {

    public static final Finder<UUID, Sale> find = new Finder<>(Sale.class);

    /**
     * Tries to find an active sale for the given region.
     *
     * @param region the region to find an active sale for
     * @return the active sale if found
     */
    public static Optional<Sale> of(@NonNull Region region) {

        return find.query().where()
                .eq("region_id", region.id())
                .findList()
                .stream()
                .filter(Sale::active)
                .findAny();
    }

    @ManyToOne
    private Region region;
    @ManyToOne
    private RegionPlayer seller;
    private double price;
    private Type type;

    @ManyToOne
    private RegionPlayer buyer;

    private Instant start;
    private Instant expires;
    private Instant end;
    private Instant acknowledged;

    public Sale(Region region, RegionPlayer seller, Type type) {

        this.region = region;
        this.seller = seller;
        this.type = type;
        this.start = Instant.now();
    }

    public Sale(Region region, RegionPlayer seller, Type type, double price) {

        this.region = region;
        this.seller = seller;
        this.type = type;
        this.price = price;
    }

    public Sale buyer(@NonNull RegionPlayer buyer) {

        this.buyer = buyer;
        end(Instant.now());
        return this;
    }

    public void abort() {

        end(Instant.now());
        region().status(Region.Status.OCCUPIED).save();
        save();
    }

    public boolean active() {

        return end == null;
    }

    public enum Type {
        DIRECT,
        AUCTION,
        SERVER;

        @DbEnumValue
        public String getValue() {
            return name();
        }
    }
}
