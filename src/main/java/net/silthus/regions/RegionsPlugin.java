package net.silthus.regions;

import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import com.google.common.base.Strings;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.WorldGuard;
import de.raidcraft.achievements.RCAchievements;
import de.raidcraft.achievements.TypeRegistrationException;
import io.ebean.Database;
import kr.entree.spigradle.annotations.PluginMain;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import net.silthus.ebean.Config;
import net.silthus.ebean.EbeanWrapper;
import net.silthus.regions.achievements.RegionAchievement;
import net.silthus.regions.commands.AdminCommands;
import net.silthus.regions.commands.RegionCommands;
import net.silthus.regions.entities.*;
import net.silthus.regions.limits.LimitsConfig;
import net.silthus.regions.listener.PlayerListener;
import net.silthus.regions.listener.SignClickListener;
import net.silthus.regions.listener.SignListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@PluginMain
@Getter
public class RegionsPlugin extends JavaPlugin {

    @Getter
    @Accessors(fluent = true)
    private static RegionsPlugin instance;

    @Setter(AccessLevel.PACKAGE)
    private Economy economy;
    @Setter(AccessLevel.PACKAGE)
    private Permission permission;
    private Database database;
    private RegionManager regionManager;
    private SchematicManager schematicManager;
    private PaperCommandManager commandManager;
    private SalesManager salesManager;

    private SignListener signListener;
    private SignClickListener signClickListener;
    private PlayerListener playerListener;

    private RegionsPluginConfig pluginConfig;
    private LimitsConfig limitsConfig;

    private boolean testing = false;

    public RegionsPlugin() {
        super();
        instance = this;
    }

    public RegionsPlugin(
            JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, dataFolder, file);
        instance = this;
        testing = true;
    }

    @Override
    public void onEnable() {

        if (!isTesting() && !setupVault()) {
            getLogger().severe(String.format("[%s] - No Vault dependency found!", getDescription().getName()));
            getLogger().severe("*** This plugin will be disabled. ***");
            this.setEnabled(false);
            return;
        }

        if (!isTesting()) {
            setupAchievements();
        }

        loadConfig();
        setupDatabase();
        setupRegionManager();
        setupSchematicManager();
        if (!isTesting()) {
            setupSalesManager();
            setupListeners();
            setupCommands();
        }
    }

    public void reload() {

        loadConfig();
        getRegionManager().reload();
        getSalesManager().start();
    }

    @Override
    public void onDisable() {

        if (!isTesting()) {
            HandlerList.unregisterAll(this);
            getCommandManager().unregisterCommands();
            getSalesManager().stop();
        }
        getRegionManager().unload();
    }

    private boolean setupVault() {

        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();

        RegisteredServiceProvider<Permission> registration = getServer().getServicesManager().getRegistration(Permission.class);
        if (registration == null) {
            return false;
        }
        permission = registration.getProvider();

        return true;
    }

    private void setupAchievements() {

        Plugin achievements = Bukkit.getPluginManager().getPlugin("RCAchievements");
        if (achievements != null) {
            try {
                ((RCAchievements) achievements).achievementManager().register(new RegionAchievement.Factory());
            } catch (TypeRegistrationException e) {
                getLogger().warning("failed to register region achievement type: " + e.getMessage());
            }
        }
    }

    private void loadConfig() {

        getDataFolder().mkdirs();
        this.pluginConfig = new RegionsPluginConfig(new File(getDataFolder(), "config.yml").toPath());
        this.pluginConfig.loadAndSave();

        this.limitsConfig = new LimitsConfig(this, new File(getDataFolder(), pluginConfig.getLimitsConfig()).toPath());
        this.limitsConfig.loadAndSave();
    }

    private void setupDatabase() {

        this.database = new EbeanWrapper(Config.builder(this)
                .entities(
                        Region.class,
                        RegionSign.class,
                        RegionAcl.class,
                        RegionGroup.class,
                        RegionPlayer.class,
                        RegionTransaction.class,
                        OwnedRegion.class,
                        Sale.class
                )
                .build()).connect();
    }

    private void setupRegionManager() {

        this.regionManager = new RegionManager(this, pluginConfig);
        regionManager.registerDefaults();

        regionManager.load();
    }

    private void setupSalesManager() {

        this.salesManager = new SalesManager(this);
        Bukkit.getPluginManager().registerEvents(salesManager, this);
        salesManager.start();
    }

    private void setupSchematicManager() {

        if (pluginConfig.isEnableSchematics()) {
            this.schematicManager = new SchematicManager(this, WorldEdit.getInstance());
            Bukkit.getPluginManager().registerEvents(schematicManager, this);
        }
    }

    private void setupListeners() {

        signListener = new SignListener(this);
        Bukkit.getPluginManager().registerEvents(signListener, this);

        signClickListener = new SignClickListener(this);
        Bukkit.getPluginManager().registerEvents(signClickListener, this);

        playerListener = new PlayerListener();
        Bukkit.getPluginManager().registerEvents(playerListener, this);
    }


    private void setupCommands() {

        this.commandManager = new PaperCommandManager(this);

        registerRegionPlayerContext(commandManager);
        registerRegionContext(commandManager);
        registerGroupsContext(commandManager);

        registerRegionsCompletion(commandManager);
        registerWorldGuardRegionCompletion(commandManager);
        registerGroupsCompletion(commandManager);
        registerSalesCompletion(commandManager);
        registerSchematicsCompletion(commandManager);
        registerOwnRegionsCompletion(commandManager);

        commandManager.registerCommand(new AdminCommands(this));
        commandManager.registerCommand(new RegionCommands(this));
    }

    private void registerSchematicsCompletion(PaperCommandManager commandManager) {

        if (schematicManager == null) return;
        commandManager.getCommandCompletions().registerAsyncCompletion("schematics", context -> getSchematicManager().getSchematics(context.getContextValue(Region.class)));
    }

    private void registerRegionPlayerContext(PaperCommandManager commandManager) {

        commandManager.getCommandContexts().registerIssuerAwareContext(RegionPlayer.class, c -> {
            String arg = c.popFirstArg();
            if (Strings.isNullOrEmpty(arg)) {
                return RegionPlayer.getOrCreate(c.getPlayer());
            }
            Optional<RegionPlayer> player = RegionPlayer.of(arg);
            if (player.isEmpty()) {
                throw new InvalidCommandArgument("Kein Spieler mit dem Namen " + arg + " gefunden!");
            }
            return player.get();
        });
    }

    private void registerRegionContext(PaperCommandManager commandManager) {

        commandManager.getCommandContexts().registerIssuerAwareContext(Region.class, c -> {
            String regionName = c.popFirstArg();

            Optional<Region> region;
            if (Strings.isNullOrEmpty(regionName)) {
                Location location = c.getPlayer().getLocation();
                region = Region.atSign(location)
                            .or(() -> {
                                Collection<Region> regions = Region.at(location);
                                if (regions.size() > 1) {
                                    throw new InvalidCommandArgument("Es wurden mehrere Grundstücke an deiner Position gefunden: "
                                            + regions.stream().map(Region::name).collect(Collectors.joining(",")));
                                }
                                return regions.stream().findAny();
                            });
                if (region.isEmpty()) {
                    throw new InvalidCommandArgument("Unable to find a region at the current position.");
                }
            } else {
                try {
                    region = Optional.ofNullable(Region.find.byId(UUID.fromString(regionName)));
                } catch (IllegalArgumentException e) {
                    region = Region.of(c.getPlayer().getWorld(), regionName);
                }
            }

            if (region.isEmpty()) {
                throw new InvalidCommandArgument("Unable to find a region with the name or id " + regionName);
            }

            return region.get();
        });
    }

    private void registerOwnerCondition(PaperCommandManager commandManager) {

        commandManager.getCommandConditions().addCondition(Region.class, "owner", (context, execContext, value) -> {
            if (context.getIssuer().hasPermission(Constants.SELL_REGION_FOR_OTHERS_PERMISSION)) {
                return;
            }
            if (!value.isOwner(context.getIssuer().getPlayer())) {
                throw new ConditionFailedException("Du bist nicht der Besitzer dieser Region.");
            }
        });
    }

    private void registerGroupsContext(PaperCommandManager commandManager) {

        commandManager.getCommandContexts().registerContext(RegionGroup.class, c -> {
            String identifier = c.popFirstArg();

            Optional<RegionGroup> group = RegionGroup.of(identifier);
            if (group.isEmpty()) {
                throw new InvalidCommandArgument("Es wurde keine Regions Gruppe mit der ID " + identifier + " gefunden.");
            }
            return group.get();
        });
    }

    private void registerRegionsCompletion(PaperCommandManager commandManager) {

        commandManager.getCommandCompletions().registerAsyncCompletion("regions", context ->
                Region.find.all().stream()
                        .filter(region -> region.world() == null
                                || context.getPlayer().getWorld().getUID().equals(region.world()))
                        .map(Region::name)
                        .collect(Collectors.toSet()));
    }

    private void registerOwnRegionsCompletion(PaperCommandManager commandManager) {

        commandManager.getCommandCompletions().registerAsyncCompletion("ownregions", context ->
                Region.find.all().stream()
                        .filter(region -> region.isOwner(context.getPlayer()))
                        .map(Region::name)
                        .collect(Collectors.toSet()));
    }


    private void registerSalesCompletion(PaperCommandManager commandManager) {

        commandManager.getCommandCompletions().registerAsyncCompletion("sales", context -> Sale.find.query().where()
                .eq("seller_id", context.getPlayer().getUniqueId())
                .isNull("end")
                .findList()
                .stream().map(Sale::region)
                .map(Region::name)
                .collect(Collectors.toSet()));
    }

    private void registerGroupsCompletion(PaperCommandManager commandManager) {

        commandManager.getCommandCompletions().registerAsyncCompletion("groups", context ->
                RegionGroup.find.all().stream().map(RegionGroup::identifier).collect(Collectors.toSet()));
    }

    private void registerWorldGuardRegionCompletion(PaperCommandManager commandManager) {

        commandManager.getCommandCompletions().registerAsyncCompletion("wgRegions", context -> {
            com.sk89q.worldguard.protection.managers.RegionManager regionManager = WorldGuard.getInstance().getPlatform()
                    .getRegionContainer().get(new BukkitWorld(context.getPlayer().getWorld()));

            if (regionManager == null) {
                return new HashSet<>();
            }

            return regionManager.getApplicableRegionsIDs(new BukkitPlayer(context.getPlayer()).getLocation().toVector().toBlockPoint())
                    .stream().filter(s -> !getPluginConfig().getIgnoredRegions().contains(s))
                    .collect(Collectors.toSet());
        });
    }
}
