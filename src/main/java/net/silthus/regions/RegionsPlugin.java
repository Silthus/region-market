package net.silthus.regions;

import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.PaperCommandManager;
import com.google.common.base.Strings;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.WorldGuard;
import io.ebean.Database;
import kr.entree.spigradle.annotations.PluginMain;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.wiefferink.interactivemessenger.processing.Message;
import me.wiefferink.interactivemessenger.source.LanguageManager;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import net.silthus.ebean.Config;
import net.silthus.ebean.EbeanWrapper;
import net.silthus.regions.commands.AdminCommands;
import net.silthus.regions.commands.RegionCommands;
import net.silthus.regions.entities.*;
import net.silthus.regions.limits.LimitsConfig;
import net.silthus.regions.listener.PlayerListener;
import net.silthus.regions.listener.SignClickListener;
import net.silthus.regions.listener.SignListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.io.File;
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
    private PaperCommandManager commandManager;
    private LanguageManager languageManager;

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

        loadConfig();
        setupLanguageManager();
        setupDatabase();
        setupRegionManager();
        if (!isTesting()) {
            setupListeners();
            setupCommands();
        }
    }

    public void reload() {

        loadConfig();
        getRegionManager().reload();
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
                        OwnedRegion.class
                )
                .build()).connect();
    }

    private void setupRegionManager() {

        this.regionManager = new RegionManager(this, pluginConfig);
        regionManager.registerDefaults();

        regionManager.load();
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
        registerRegionsCompletion(commandManager);
        registerWorldGuardRegionCompletion(commandManager);

        commandManager.registerCommand(new AdminCommands(this));
        commandManager.registerCommand(new RegionCommands(this));
    }

    private void registerRegionPlayerContext(PaperCommandManager commandManager) {

        commandManager.getCommandContexts().registerIssuerOnlyContext(RegionPlayer.class, c -> RegionPlayer.getOrCreate(c.getPlayer()));
    }

    private void registerRegionContext(PaperCommandManager commandManager) {

        commandManager.getCommandContexts().registerContext(Region.class, c -> {
            String regionName = c.popFirstArg();

            Optional<Region> region;
            if (Strings.isNullOrEmpty(regionName)) {
                region = Region.of(c.getPlayer().getLocation());
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

    private void registerRegionsCompletion(PaperCommandManager commandManager) {

        commandManager.getCommandCompletions().registerAsyncCompletion("regions", context ->
                Region.find.all().stream()
                .filter(region -> region.world() == null
                        || context.getPlayer().getWorld().getUID().equals(region.world()))
                .map(Region::name)
                .collect(Collectors.toSet()));
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

    private void setupLanguageManager() {

        languageManager = new LanguageManager(
                this,                                  // The plugin (used to get the languages bundled in the jar file)
                "lang",                           // Folder where the languages are stored
                getConfig().getString("language"),     // The language to use indicated by the plugin user
                "EN",                                  // The default language, expected to be shipped with the plugin and should be complete, fills in gaps in the user-selected language
                getConfig().getStringList("chatPrefix") // Chat prefix to use with Message#prefix(), could of course come from the config file
        );
    }

    /**
     * Send a message to a target without a prefix.
     * @param target       The target to send the message to
     * @param key          The key of the language string
     * @param replacements The replacements to insert in the message
     */
    public void messageNoPrefix(Object target, String key, Object... replacements) {
        Message.fromKey(key).replacements(replacements).send(target);
    }

    /**
     * Send a message to a target, prefixed by the default chat prefix.
     * @param target       The target to send the message to
     * @param key          The key of the language string
     * @param replacements The replacements to insert in the message
     */
    public void message(Object target, String key, Object... replacements) {
        Message.fromKey(key).prefix().replacements(replacements).send(target);
    }
}
