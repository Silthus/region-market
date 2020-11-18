package net.silthus.regions;

import io.ebean.Database;
import kr.entree.spigradle.annotations.PluginMain;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import net.silthus.ebean.Config;
import net.silthus.ebean.EbeanWrapper;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionAcl;
import net.silthus.regions.entities.RegionGroup;
import net.silthus.regions.entities.RegionPlayer;
import net.silthus.regions.entities.RegionTransaction;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginLoader;

import java.io.File;

@PluginMain
@Getter
public class RegionsPlugin extends JavaPlugin {

    private Economy economy;
    private Database database;
    private RegionManager regionManager;
    private RegionsPluginConfig pluginConfig;

    public RegionsPlugin() {
    }

    public RegionsPlugin(
            JavaPluginLoader loader, PluginDescriptionFile description, File dataFolder, File file) {
        super(loader, description, dataFolder, file);
    }

    @Override
    public void onEnable() {

        if (!setupEconomy() ) {
            getLogger().severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        loadConfig();
        setupDatabase();
        setupRegionManager();
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

        this.database = new EbeanWrapper(Config.builder(this).entities(
                Region.class,
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
}
