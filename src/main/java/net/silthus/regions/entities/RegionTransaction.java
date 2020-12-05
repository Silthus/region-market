package net.silthus.regions.entities;

import io.ebean.annotation.DbEnumValue;
import io.ebean.annotation.DbJson;
import io.ebean.text.json.EJson;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.silthus.ebean.BaseEntity;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "rcregions_transactions")
@Getter
@Setter
@Accessors(fluent = true)
public class RegionTransaction extends BaseEntity {

    static {
        try {
            EJson.write(new Object());
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public static RegionTransaction of(Region region, RegionPlayer player, Action action) {

        return new RegionTransaction(region, player).action(action);
    }

    public static RegionTransaction of(Region region, Action action) {

        return new RegionTransaction(region).action(action);
    }

    @ManyToOne
    private Region region;
    @ManyToOne
    private RegionPlayer player;
    private Action action;
    @DbJson
    private Map<String, Object> data = new HashMap<>();

    RegionTransaction(Region region) {

        this.region = region;
    }

    RegionTransaction(Region region, RegionPlayer player) {
        this.region = region;
        this.player = player;
    }

    public RegionTransaction data(String key, Object value) {

        data.put(key, value);
        return this;
    }

    public enum Action {

        SELL_TO_SERVER,
        SELL_TO_PLAYER,
        BUY,
        CHANGE_OWNER,
        SAVE_SCHEMATIC;

        @DbEnumValue
        public String getValue() {
            return name();
        }
    }
}
