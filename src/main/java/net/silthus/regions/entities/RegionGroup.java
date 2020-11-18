package net.silthus.regions.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.silthus.ebean.BaseEntity;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sregions_region_groups")
@Getter
@Setter
@Accessors(fluent = true)
public class RegionGroup extends BaseEntity {

    private String identifier;
    private String name;
    private String description;

    @OneToMany
    private List<Region> regions = new ArrayList<>();
}
