package net.silthus.regions.entities;

import io.ebean.Finder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Optional;
import java.util.UUID;

@Entity
@Getter
@Setter
@Accessors(fluent = true)
@Table(name = "rcregions_direct_sales")
public class DirectSale extends Sale {

    public static final Finder<UUID, DirectSale> find = new Finder<>(DirectSale.class);

    public static Optional<DirectSale>

    private double price;
}
