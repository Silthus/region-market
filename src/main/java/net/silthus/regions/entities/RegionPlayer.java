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
@Getter
@Setter
@Accessors(fluent = true)
@Table(name = "sregions_players")
public class RegionPlayer extends BaseEntity {

    private String name;
    @OneToMany(cascade = CascadeType.DETACH)
    private List<Region> regions = new ArrayList<>();
}
