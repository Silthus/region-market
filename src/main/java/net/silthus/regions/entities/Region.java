package net.silthus.regions.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.silthus.ebean.BaseEntity;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Accessors(fluent = true)
@Table(name = "sregions_regions")
public class Region extends BaseEntity {

    @ManyToOne
    private RegionGroup group;
    private String worldGuardRegion;
    @ManyToOne
    private RegionPlayer owner;

    @OneToMany(cascade = CascadeType.REMOVE)
    private List<RegionAcl> acl = new ArrayList<>();

    @OneToMany(cascade = CascadeType.REMOVE)
    private List<RegionTransaction> transactions = new ArrayList<>();
}
