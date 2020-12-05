package net.silthus.regions;

import lombok.extern.java.Log;
import net.silthus.regions.costs.MoneyCost;
import net.silthus.regions.entities.RegionGroup;
import net.silthus.regions.util.ConfigUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log(topic = "RCRegions")
public final class RegionManager {

    private final RegionsPlugin plugin;
    private final RegionsPluginConfig config;
    private final Map<String, Cost.Registration<?>> costTypes = new HashMap<>();
    private final Map<String, RegionGroup> groups = new HashMap<>();

    public RegionManager(RegionsPlugin plugin, RegionsPluginConfig config) {

        this.plugin = plugin;
        this.config = config;
    }

    public void load() {

        loadRegionGroups(new File(plugin.getDataFolder(), config.getRegionGroupsConfig()).toPath());
    }

    public void unload() {

        groups.clear();
    }

    public void reload() {
        unload();
        load();
    }

    public void registerDefaults() {

        register(MoneyCost.class, () -> new MoneyCost(plugin.getEconomy()));
    }

    public void loadRegionGroups(Path path) {
        plugin.saveResource("groups.yml", false);
        if (Files.isRegularFile(path)) {
            loadRegionGroupsFromFile(path.toFile());
        } else {
            loadRegionGroupsFromPath(path);
        }
    }

    private void loadRegionGroupsFromFile(File file) {

        if (!file.exists()) {
            log.warning("Region groups config does not exist: " + file.getAbsolutePath());
            return;
        }
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.load(file);

            ConfigurationSection groups = config.getConfigurationSection("groups");
            if (groups != null) {
                Set<String> keys = groups.getKeys(false);
                List<RegionGroup> regionGroups = keys.stream()
                        .map(s -> loadRegionGroup(s, Objects.requireNonNull(groups.getConfigurationSection(s))))
                        .flatMap(regionGroup -> regionGroup.stream().flatMap(Stream::of))
                        .collect(Collectors.toList());

                log.info("Loaded " + regionGroups.size() + "/" + keys.size() + " region groups from config " + file.getAbsolutePath());
            } else {
                log.warning("Region Groups config " + file.getAbsolutePath() + " contains no 'groups' section.");
            }
        } catch (IOException | InvalidConfigurationException e) {
            log.severe("Invalid region groups config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadRegionGroupsFromPath(Path path) {

        try {
            Files.createDirectories(path);
            List<File> files = Files.find(path, Integer.MAX_VALUE,
                    (file, fileAttr) -> fileAttr.isRegularFile())
                    .map(Path::toFile).collect(Collectors.toList());


            int fileCount = files.size();
            List<RegionGroup> regionGroups = files.stream().map(file -> loadRegionGroup(path, file))
                    .flatMap(regionGroup -> regionGroup.stream().flatMap(Stream::of))
                    .collect(Collectors.toList());

            log.info("Loaded " + regionGroups.size() + "/" + fileCount + " region groups from path " + path);
        } catch (IOException e) {
            log.severe("unable to load region groups from " + path + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Optional<RegionGroup> loadRegionGroup(Path base, File file) {

        if (!file.exists() || !(file.getName().endsWith(".yml") && !file.getName().endsWith(".yaml"))) {
            return Optional.empty();
        }

        try {
            YamlConfiguration config = new YamlConfiguration();
            config.load(file);
            return loadRegionGroup(ConfigUtil.getFileIdentifier(base, file), config);
        } catch (IOException | InvalidConfigurationException e) {
            log.severe("unable to load region group config " + file.getAbsolutePath() + ": " + e.getMessage());
            e.printStackTrace();
        }

        return Optional.empty();
    }

    public Optional<RegionGroup> loadRegionGroup(String identifier, ConfigurationSection config) {

        config.set("id", config.getString("id", identifier));

        RegionGroup regionGroup = RegionGroup.getOrCreate(config.getString("id"));
        regionGroup.load(this, config);
        regionGroup.save();

        groups.put(regionGroup.identifier(), regionGroup);
        log.info("Loaded region group: " + regionGroup.name() + " (" + regionGroup.identifier() + ")");

        return Optional.of(regionGroup);
    }

    public Optional<RegionGroup> getRegionGroup(String identifier) {

        return Optional.ofNullable(groups.get(identifier));
    }

    public <TCost extends Cost> RegionManager register(Class<TCost> costClass, Supplier<TCost> supplier) {

        if (!costClass.isAnnotationPresent(CostType.class)) {
            log.severe(costClass.getCanonicalName() + " is missing the @CostType annotation. Cannot register it.");
            return this;
        }

        String type = costClass.getAnnotation(CostType.class).value().toLowerCase();

        if (costTypes.containsKey(type)) {
            log.warning("Cannot register cost type " + costClass.getCanonicalName()
                    + "! A duplicate cost with the identifier " + type + " already exists: "
                    + costTypes.get(type).costClass().getCanonicalName());
            return this;
        }

        costTypes.put(type, new Cost.Registration<>(type, costClass, supplier));
        log.info("registered cost type \"" + type + "\": " + costClass.getCanonicalName());
        return this;
    }

    public Optional<Cost> getCost(String type, ConfigurationSection section) {

        if (!costTypes.containsKey(type)) {
            return Optional.empty();
        }

        Cost cost = costTypes.get(type).supplier().get();
        cost.load(section);

        return Optional.of(cost);
    }
}
