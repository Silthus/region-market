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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
@Accessors(fluent = true)
@Entity
@Table(name = "rcregions_sales")
public class Sale extends BaseEntity {

    public static final Finder<UUID, Sale> find = new Finder<>(Sale.class);

    public static List<Sale> of(@NonNull Region region) {

        return find.query().where().eq("region_id", region.id()).findList();
    }

    /**
     * Tries to find an active sale for the given region.
     *
     * @param region the region to find an active sale for
     * @return the active sale if found
     */
    public static Optional<Sale> getActiveSale(@NonNull Region region) {

        return find.query().where()
                .eq("region_id", region.id())
                .findList()
                .stream()
                .filter(Sale::active)
                .findAny();
    }

    public static List<Sale> getActiveSales() {

        return find.query().where()
                .isNull("end")
                .findList();
    }

    public static List<Sale> getExpiredSales() {

        return find.query().where()
                .isNull("end")
                .and()
                .le("expires", Instant.now())
                .findList();
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

    public void abort(boolean acknowledge) {

        end(Instant.now());
        if (acknowledge) acknowledged(Instant.now());
        Region region = region();
        region.status(Region.Status.OCCUPIED)
                .priceType(region.group().priceType())
                .save();
        save();
    }

    public boolean active() {

        return end == null;
    }

    public boolean needsAcknowledgement() {

        return !active() && acknowledged() == null;
    }

    public boolean expired() {

        return !active() && buyer() == null;
    }

    public void expire() {

        abort(false);
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
