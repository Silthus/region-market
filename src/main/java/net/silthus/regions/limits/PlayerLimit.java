package net.silthus.regions.limits;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import net.silthus.regions.Constants;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionPlayer;
import net.silthus.regions.util.Enums;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.Collection;
import java.util.stream.Collectors;

@Data
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper = true)
public class PlayerLimit extends Limit {

    private final RegionPlayer player;

    public PlayerLimit(RegionPlayer player, Limit limit) {
        super(limit);
        this.player = player;
    }

    public LimitCheckResult test(Region region) {

        return check(region, player());
    }

    public PlayerLimit loadPlayerLimits() {

        for (String limitKey : player.getBukkitPlayer().stream()
                .map(Permissible::getEffectivePermissions)
                .flatMap(Collection::stream)
                .map(PermissionAttachmentInfo::getPermission)
                .filter(permission -> permission.startsWith(Constants.LIMITS_OVERRIDE_PREFIX))
                .map(s -> s.replace(Constants.LIMITS_OVERRIDE_PREFIX, ""))
                .collect(Collectors.toList())) {
            String[] split = limitKey.split("\\.");
            if (split.length > 1) {
                LimitCheckResult.Type limitType = Enums.searchEnum(LimitCheckResult.Type.class, split[0]);
                if (limitType != null) {
                    switch (limitType) {
                        case REGIONS_IN_GROUP_LIMIT_REACHED:
                            if (split.length == 3) {
                                String groupName = split[1];
                                super.groupRegions().put(groupName, Integer.parseInt(split[2]));
                            }
                            break;
                        case TOTAL_GROUP_LIMIT_REACHED:
                        case TOTAL_LIMIT_REACHED:
                            int limitValue = Integer.parseInt(split[1]);
                            super.set(limitType, limitValue);
                            break;
                    }
                }
            }
        }

        return this;
    }
}
