package net.silthus.regions.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import com.google.common.base.Strings;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import net.silthus.regions.RegionsPlugin;
import org.bukkit.Location;
import org.bukkit.command.CommandException;

import java.util.List;
import java.util.stream.Collectors;

@CommandAlias("rcra|rcregions:admin|sregions:admin|sra")
public class AdminCommands extends BaseCommand {

    private final RegionsPlugin plugin;

    public AdminCommands(RegionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Subcommand("create")
    @CommandPermission("sregions.region.create")
    public void create(Location location, @Optional String region) {

        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(new BukkitWorld(location.getWorld()));
        if (regionManager == null) {
            throw new CommandException("WorldGuard RegionManager not available.");
        }

        ProtectedRegion protectedRegion;
        if (Strings.isNullOrEmpty(region)) {
            ApplicableRegionSet regions = regionManager.getApplicableRegions(BlockVector3.at(location.getX(), location.getY(), location.getZ()));
            List<ProtectedRegion> regionList = regions.getRegions().stream()
                    .filter(region1 -> !plugin.getPluginConfig().getIgnoredRegions().contains(region1.getId()))
                    .collect(Collectors.toList());

            if (regionList.size() > 1) {
                throw new CommandException("Found more than one region: " + regionList.stream().map(ProtectedRegion::getId).collect(Collectors.joining())
                        + ". Please add region to the ignored regions inside config.yml. Or use the /sregions:admin create <region> command.");
            } else if (regionList.isEmpty()) {
                throw new CommandException("No applicable regions found!");
            }

            protectedRegion = regionList.get(0);
        } else {
            protectedRegion = regionManager.getRegion(region);
            if (protectedRegion == null) {
                throw new CommandException("A region with the id " + region + " does not exist.");
            }
        }

//        Region buyableRegion = Region.of(location.getWorld().getUID(), protectedRegion);
//        Hologram hologram = HologramsAPI.createHologram(plugin, location);
//        hologram.appendTextLine(ChatColor.YELLOW + buyableRegion.worldGuardRegion());
//        hologram.appendTextLine(buyableRegion.)
    }
}
