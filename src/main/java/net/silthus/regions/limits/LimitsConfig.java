package net.silthus.regions.limits;

import de.exlll.configlib.annotation.Comment;
import de.exlll.configlib.annotation.Ignore;
import de.exlll.configlib.configs.yaml.BukkitYamlConfiguration;
import de.exlll.configlib.format.FieldNameFormatters;
import net.silthus.regions.Constants;
import net.silthus.regions.RegionsPlugin;
import net.silthus.regions.entities.RegionPlayer;
import net.silthus.regions.util.Enums;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class LimitsConfig extends BukkitYamlConfiguration {

    @Ignore
    private final RegionsPlugin plugin;

    @Comment({
            "Define your limits in this config.",
            "Each limit has a unique key that can be assigned to players by giving them the rcregions.limits.<your-limit> permission.",
            "You can overwrite specific limits per player with the following permissions:",
            "   - rcregions.player-limits.total.<limit>",
            "   - rcregions.player-limits.regions.<group>.<limit>",
            "   - rcregions.player-limits.groups.<limit>"
    })
    public Map<String, Limit> limits = new HashMap<>();

    public LimitsConfig(RegionsPlugin plugin, Path path) {

        super(path, BukkitYamlProperties.builder().setFormatter(FieldNameFormatters.LOWER_UNDERSCORE).build());
        this.plugin = plugin;
        limits.put("default", new Limit()
                .priority(1)
        );
    }

    public Optional<PlayerLimit> getPlayerLimit(RegionPlayer player) {

        return player.getBukkitPlayer().stream()
                .map(Permissible::getEffectivePermissions)
                .flatMap(Collection::stream)
                .map(PermissionAttachmentInfo::getPermission)
                .filter(permission -> permission.startsWith(Constants.LIMITS_PREFIX))
                .map(s -> s.replace(Constants.LIMITS_PREFIX, ""))
                .map(s -> limits.get(s))
                .filter(Objects::nonNull)
                .reduce(Limit::combine)
                .map(limit -> new PlayerLimit(player, limit))
                .map(PlayerLimit::loadPlayerLimits);
    }
}
