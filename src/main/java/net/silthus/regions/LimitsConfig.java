package net.silthus.regions;

import de.exlll.configlib.annotation.Comment;
import de.exlll.configlib.configs.yaml.BukkitYamlConfiguration;
import de.exlll.configlib.format.FieldNameFormatters;

import java.nio.file.Path;

public class LimitsConfig extends BukkitYamlConfiguration {

    @Comment({
            "Sets the total limit of regions for all players. Use -1 for an infinite number of regions.",
            "You can always override the per player limit with a permission node: rcregions.limits.overrides.total.<limit_number>"
    })
    private int total = -1;

    public LimitsConfig(Path path) {

        super(path, BukkitYamlProperties.builder().setFormatter(FieldNameFormatters.LOWER_UNDERSCORE).build());
    }
}
