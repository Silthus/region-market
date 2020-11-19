package net.silthus.regions;

import co.aikar.commands.PaperCommandManager;
import com.comphenix.protocol.ProtocolLibrary;
import io.ebean.Database;
import kr.entree.spigradle.annotations.PluginMain;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import net.silthus.ebean.Config;
import net.silthus.ebean.EbeanWrapper;
import net.silthus.regions.commands.AdminCommands;
import net.silthus.regions.entities.*;
import net.silthus.regions.listener.ClickListener;
import net.silthus.regions.listener.SignListener;
import net.silthus.regions.listener.SignPacketListener;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

@PluginMain
@Getter
public class RegionsPlugin extends JavaPlugin {

    private Economy economy;
    private Database database;
    private RegionManager regionManager;
    private RegionsPluginConfig pluginConfig;
    private PaperCommandManager commandManager;

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

        if (!testing && !setupEconomy() ) {
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
        setupDatabase();
        setupRegionManager();
        setupListeners();
        setupCommands();
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
                .runMigrations(true)
                .entities(
                    Region.class,
                    RegionSign.class,
                    RegionAcl.class,
                    RegionGroup.class,
                    RegionPlayer.class,
                    RegionTransaction.class
        ).build()).connect();
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
        try {
            saveResource("lang_de.yml", false);
            commandManager.addSupportedLanguage(Locale.GERMAN);
            commandManager.getLocales().loadYamlLanguageFile("lang_de.yml", Locale.GERMAN);
            commandManager.getLocales().setDefaultLocale(Locale.GERMAN);
        } catch (IOException | InvalidConfigurationException e) {
            getLogger().severe("unable to load locales");
            e.printStackTrace();
        }

        commandManager.registerCommand(new AdminCommands(this));
    }
}
