package net.silthus.regions;

import co.aikar.commands.PaperCommandManager;
import com.comphenix.protocol.ProtocolLibrary;
import com.google.common.base.Strings;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.WorldGuard;
import io.ebean.Database;
import kr.entree.spigradle.annotations.PluginMain;
import lombok.Getter;
import me.wiefferink.interactivemessenger.processing.Message;
import me.wiefferink.interactivemessenger.source.LanguageManager;
import net.milkbowl.vault.economy.Economy;
import net.silthus.ebean.Config;
import net.silthus.ebean.EbeanWrapper;
import net.silthus.regions.commands.AdminCommands;
import net.silthus.regions.commands.RegionCommands;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionAcl;
import net.silthus.regions.entities.RegionGroup;
import net.silthus.regions.entities.RegionPlayer;
import net.silthus.regions.entities.RegionSign;
import net.silthus.regions.entities.RegionTransaction;
import net.silthus.regions.listener.ClickListener;
import net.silthus.regions.listener.SignListener;
import net.silthus.regions.listener.SignPacketListener;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandException;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.io.File;
import java.util.HashSet;
import java.util.Optional;
import java.util.stream.Collectors;

@PluginMain
@Getter
public class RegionsPlugin extends JavaPlugin {

    private Economy economy;
    private Database database;
    private RegionManager regionManager;
    private RegionsPluginConfig pluginConfig;
    private PaperCommandManager commandManager;
    private LanguageManager languageManager;

    private SignPacketListener signPacketListener;
    private SignListener signListener;
    private ClickListener clickListener;

    private boolean testing = false;

    public RegionsPlugin() {
        super();
    }

    public RegionsPlugin(
            JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, dataFolder, file);
        testing = true;
    }

    @Override
    public void onEnable() {

        if (!isTesting() && !setupEconomy()) {
            getLogger().severe(String.format("[%s] - No Vault dependency found!", getDescription().getName()));
            getLogger().severe("*** This plugin will be disabled. ***");
            this.setEnabled(false);
            return;
        }

//        if (!testing && !Bukkit.getPluginManager().isPluginEnabled("HolographicDisplays")) {
//            getLogger().severe("*** HolographicDisplays is not installed or not enabled. ***");
//            getLogger().severe("*** This plugin will be disabled. ***");
//            this.setEnabled(false);
//            return;
//        }

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

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return true;
    }

    private void loadConfig() {

        getDataFolder().mkdirs();
        pluginConfig = new RegionsPluginConfig(new File(getDataFolder(), "config.yml").toPath());
        pluginConfig.loadAndSave();
    }

    private void setupDatabase() {

        this.database = new EbeanWrapper(Config.builder(this)
                .entities(
                        Region.class,
                        RegionSign.class,
                        RegionAcl.class,
                        RegionGroup.class,
                        RegionPlayer.class,
                        RegionTransaction.class
                )
                .build()).connect();
    }

    private void setupRegionManager() {

        this.regionManager = new RegionManager(this, pluginConfig);
        regionManager.registerDefaults();

        regionManager.load();
    }

    private void setupListeners() {

        signPacketListener = new SignPacketListener(this, ProtocolLibrary.getProtocolManager());
        Bukkit.getPluginManager().registerEvents(signPacketListener, this);

        signListener = new SignListener(this);
        Bukkit.getPluginManager().registerEvents(signListener, this);

        clickListener = new ClickListener(this);
        Bukkit.getPluginManager().registerEvents(clickListener, this);
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

        commandManager.getCommandContexts().registerIssuerOnlyContext(RegionPlayer.class, c -> RegionPlayer.of(c.getPlayer()));
    }

    private void registerRegionContext(PaperCommandManager commandManager) {

        commandManager.getCommandContexts().registerContext(Region.class, c -> {
            String regionName = c.popFirstArg();

            Optional<Region> region;
            if (Strings.isNullOrEmpty(regionName)) {
                region = Region.of(c.getPlayer().getLocation());
                if (region.isEmpty()) {
                    throw new CommandException("Unable to find a region at the current position.");
                }
            } else {
                region = Region.of(c.getPlayer().getWorld(), regionName);
                if (region.isEmpty()) {
                    throw new CommandException("Unable to find a region with the name " + regionName);
                }
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
