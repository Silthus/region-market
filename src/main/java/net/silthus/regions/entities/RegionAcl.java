package net.silthus.regions.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.silthus.ebean.BaseEntity;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Getter
@Setter
@Accessors(fluent = true)
@Table(name = "rcregions_acl")
public class RegionAcl extends BaseEntity {

    @ManyToOne
    private Region region;
    @ManyToOne
    private RegionPlayer player;
    private AccessLevel accessLevel;
}
