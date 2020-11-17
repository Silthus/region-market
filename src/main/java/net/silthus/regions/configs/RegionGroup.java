package net.silthus.regions.configs;

import de.exlll.configlib.configs.yaml.BukkitYamlConfiguration;
import de.exlll.configlib.format.FieldNameFormatters;

import java.nio.file.Path;

public class RegionGroup extends BukkitYamlConfiguration {

    private String identifier;
    private String name;
    private String description;
    private double basePrice;

    public RegionGroup(Path path) {
        super(path, BukkitYamlProperties.builder().setFormatter(FieldNameFormatters.LOWER_UNDERSCORE).build());
    }
}
