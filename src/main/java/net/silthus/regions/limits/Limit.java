package net.silthus.regions.limits;

import de.exlll.configlib.annotation.Comment;
import de.exlll.configlib.annotation.ConfigurationElement;
import io.ebean.annotation.DbEnumValue;
import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionGroup;
import net.silthus.regions.entities.RegionPlayer;

import java.util.EnumSet;
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

    public LimitCheckResult check(@NonNull Region region, @NonNull RegionPlayer player) {

        EnumSet<LimitCheckResult.Type> result = EnumSet.noneOf(LimitCheckResult.Type.class);

        boolean totalLimit = hasReachedTotalLimit(player);
        if (totalLimit) result.add(LimitCheckResult.Type.TOTAL_LIMIT_REACHED);

        boolean groupLimit = hasReachedGroupLimit(player);
        if (groupLimit) result.add(LimitCheckResult.Type.TOTAL_GROUP_LIMIT_REACHED);

        boolean inGroupLimit = hasReachedRegionsInGroupLimit(player, region.group());
        if (inGroupLimit) result.add(LimitCheckResult.Type.REGIONS_IN_GROUP_LIMIT_REACHED);

        if (!totalLimit && !groupLimit && !inGroupLimit) {
            return new LimitCheckResult();
        } else {
            return new LimitCheckResult(result);
        }
    }

    boolean hasReachedTotalLimit(@NonNull RegionPlayer player) {

        return total() > -1 && player.regions().size() >= total();
    }

    boolean hasReachedRegionsInGroupLimit(@NonNull RegionPlayer player, @NonNull RegionGroup group) {

        int groupLimit = groupRegions().getOrDefault(group.identifier(), -1);
        return groupLimit > -1 && player.regions().stream()
                .filter(region -> Objects.nonNull(region.group()))
                .filter(region -> region.group().equals(group))
                .count() >= groupLimit;
    }

    boolean hasReachedGroupLimit(@NonNull RegionPlayer player) {

        return groups() > -1 && player.regionGroups().size() >= groups();
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

    public void set(LimitCheckResult.Type limitType, int limitValue) {

        switch (limitType) {
            case TOTAL_GROUP_LIMIT_REACHED:
                groups(limitValue);
                break;
            case TOTAL_LIMIT_REACHED:
                total(limitValue);
                break;
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
