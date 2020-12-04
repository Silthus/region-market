package net.silthus.regions.entities;

import io.ebean.Finder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.silthus.ebean.BaseEntity;

import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Getter
@Setter
@Accessors(fluent = true)
@MappedSuperclass
public abstract class Sale extends BaseEntity {

    public static final Finder<UUID, Sale> find = new Finder<>(Sale.class);

    public static Optional<Sale> of(@NonNull Region region) {

        find.query().where()
                .eq("region_id", region.id())

    }

    @ManyToOne
    private Region region;
    private RegionPlayer seller;
    private RegionPlayer buyer;

    private Instant start;
    private Instant expires;
    private Instant end;

    public boolean active() {

        return end == null;
    }
}
