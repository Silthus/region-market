package net.silthus.regions.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.silthus.ebean.BaseEntity;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "sregions_transactions")
@Getter
@Setter
@Accessors(fluent = true)
public class RegionTransaction extends BaseEntity {

    @ManyToOne
    private Region region;
}
