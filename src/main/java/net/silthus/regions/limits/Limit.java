package net.silthus.regions.limits;

import de.exlll.configlib.annotation.Comment;
import de.exlll.configlib.annotation.ConfigurationElement;
import io.ebean.annotation.DbEnumValue;
import lombok.Data;
import lombok.Value;
import lombok.experimental.Accessors;
import me.wiefferink.interactivemessenger.processing.Message;
import net.silthus.regions.entities.RegionGroup;
import net.silthus.regions.entities.RegionPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Data
@Accessors(fluent = true)
@ConfigurationElement
public class Limit {

    @Comment({
            "The total number of regions a player is allowed to be owner of.",
            "Use -1 for unlimited regions."
    })
    private int total = -1;
    @Comment("The number of allowed regions inside the defined groups.")
    private Map<String, Integer> groupRegions = new HashMap<>();
    @Comment("The number of groups a player is allowed to own regions in.")
    private int groups = -1;
    @Comment("The priority of this limit. Limits with higher priorities overwrite lower priorities.")
    private int priority = 100;
    @Comment({
            "Defines the mode how this limit should be combined with other limits when this limit has a higher priority.",
            "   - ABSOLUTE (default): all values in this limit will be set as the absolute limit",
            "   - ADDITIVE: values in this limit will be added to the lower priority limit",
            "   - SUBTRACTIVE: values in this limit will be subtracted from the lower priority limit",
            "   - PRIORITY: all values that are not -1 will be set as absolute values"
    })
    private Mode mode = Mode.ABSOLUTE;

    public Limit() {
    }

    Limit(Limit limit) {
        this.total = limit.total();
        this.groupRegions = Map.copyOf(limit.groupRegions());
        this.groups = limit.groups();
        this.priority = limit.priority();
    }

    public Result hasReachedTotalLimit(RegionPlayer player) {

        if (total() > -1 && player.regions().size() >= total()) {
            return new Result(true,
                    Message.fromKey("limits-reached-total")
                        .replacements(player, this)
                        .getSingle(),
                    Type.TOTAL
            );
        }

        return new Result(false, null, Type.TOTAL);
    }

    public Result hasReachedRegionsInGroupLimit(RegionPlayer player, RegionGroup group) {

        if (group == null) return new Result(false, null, Type.REGIONS);

        int groupLimit = groupRegions().getOrDefault(group.identifier(), -1);
        if (groupLimit > -1 && player.regions().stream()
                .filter(region -> Objects.nonNull(region.group()))
                .filter(region -> region.group().equals(group))
                .count() >= groupLimit) {
            return new Result(true,
                    Message.fromKey("limits-reached-regions")
                        .replacements(player, group, this)
                        .getSingle(),
                    Type.REGIONS
            );
        }

        return new Result(false, null, Type.REGIONS);
    }

    public Result hasReachedGroupLimit(RegionPlayer player) {

        if (groups() > -1 && player.regionGroups().size() >= groups()) {
            return new Result(true,
                    Message.fromKey("limits-reached-group")
                        .replacements(player, this)
                        .getSingle(),
                    Type.GROUPS
            );
        }

        return new Result(false, null, Type.GROUPS);
    }

    public Limit setGroupLimit(String group, int limit) {
        groupRegions().put(group, limit);
        return this;
    }

    public Limit combine(Limit other) {

        if (other.priority() > this.priority()) {
            return new Limit(other);
        } else {
            return new Limit(this);
        }
    }

    public void set(Type limitType, int limitValue) {

        switch (limitType) {
            case GROUPS:
                groups(limitValue);
                break;
            case TOTAL:
                total(limitValue);
                break;
        }
    }

    @Value
    public static class Result {

        boolean reachedLimit;
        String error;
        Type type;
    }

    public enum Type {

        TOTAL,
        REGIONS,
        GROUPS,
        ALL,
        NONE;

        @DbEnumValue
        public String getValue() {
            return name();
        }
    }

    /**
     * The mode how limits should be combined.
     */
    public enum Mode {

        ABSOLUTE,
        ADDITIVE,
        SUBTRACTIVE,
        PRIORITY;

        @DbEnumValue
        public String getValue() {
            return name();
        }
    }
}
