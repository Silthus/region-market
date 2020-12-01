package net.silthus.regions.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.*;
import com.google.common.base.Strings;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.silthus.regions.Messages;
import net.silthus.regions.RegionsPlugin;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionGroup;
import net.silthus.regions.entities.RegionSign;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@CommandAlias("rcra|rcregions:admin|sregions:admin|sra|srma")
public class AdminCommands extends BaseCommand implements Listener {

    private final RegionsPlugin plugin;

    public AdminCommands(RegionsPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Subcommand("reload")
    @CommandPermission("rcregions.admin.reload")
    public void reload() {

        plugin.reload();
        getCurrentCommandIssuer().sendMessage(ChatColor.GREEN + "RCRegions wurde erfolgreich neugeladen.");
    }

    @Subcommand("create")
    @CommandCompletion("@wgRegions auto|free|static")
    @CommandPermission("rcregions.region.create")
    public void create(Player player, @Optional String region, @Optional @Default("auto") String price) {

        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(new BukkitWorld(player.getWorld()));
        if (regionManager == null) {
            throw new InvalidCommandArgument("WorldGuard RegionManager not available.");
        }

        ProtectedRegion protectedRegion;
        if (Strings.isNullOrEmpty(region)) {
            Location location = player.getLocation();
            ApplicableRegionSet regions = regionManager.getApplicableRegions(
                    BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ())
            );
            List<ProtectedRegion> regionList = regions.getRegions().stream()
                    .filter(region1 -> !plugin.getPluginConfig().getIgnoredRegions().contains(region1.getId()))
                    .collect(Collectors.toList());

            if (regionList.size() > 1) {
                throw new InvalidCommandArgument("Found more than one region: " + regionList.stream().map(ProtectedRegion::getId).collect(Collectors.joining())
                        + ". Please add region to the ignored regions inside config.yml. Or use the /rcregions:admin create <region> command.");
            } else if (regionList.isEmpty()) {
                throw new InvalidCommandArgument("No applicable regions found!");
            }

            protectedRegion = regionList.get(0);
        } else {
            protectedRegion = regionManager.getRegion(region);
            if (protectedRegion == null) {
                throw new InvalidCommandArgument("A region with the id " + region + " does not exist.");
            }
        }

        if (Region.exists(player.getWorld(), protectedRegion)) {
            throw new InvalidCommandArgument("A region for " + protectedRegion.getId() + " already exists. Use the edit or set command instead.");
        }

        Region sellRegion = new Region(player.getWorld(), protectedRegion.getId());
        if (Strings.isNullOrEmpty(price) || price.startsWith("dynamic") || price.startsWith("auto")) {
            sellRegion.priceType(Region.PriceType.DYNAMIC);
            if (!Strings.isNullOrEmpty(price)) {
                String[] strings = price.split(":");
                if (strings.length > 1) {
                    try {
                        sellRegion.price(Double.parseDouble(strings[1]));
                    } catch (NumberFormatException e) {
                        throw new InvalidCommandArgument(strings[1] + " is not a valid price.");
                    }
                }
            }
        } else if (price.equalsIgnoreCase("free")) {
            sellRegion.priceType(Region.PriceType.FREE);
            sellRegion.price(0);
        } else {
            price = price.replace("static:", "");
            sellRegion.priceType(Region.PriceType.STATIC);
            try {
                sellRegion.price(Double.parseDouble(price));
            } catch (NumberFormatException e) {
                throw new InvalidCommandArgument(price + " is not a valid price.");
            }
        }

        sellRegion.save();
        player.spigot().sendMessage(new ComponentBuilder("Das Grundstück ").color(net.md_5.bungee.api.ChatColor.GREEN)
                .append(Messages.region(sellRegion, null)).append(" wurde erstellt und kann jetzt gekauft werden.").reset().color(net.md_5.bungee.api.ChatColor.GREEN)
                .create());
    }

    @Subcommand("delete|del|remove")
    @CommandCompletion("@regions")
    @CommandPermission("rcregions.region.delete")
    public void delete(Region region) {

        region.delete();
        getCurrentCommandIssuer().sendMessage(ChatColor.GREEN + "Das Grundstück " + region.name() + " wurde gelöscht.");
    }

    @Getter
    private final Map<UUID, SignLinkMode> signLinkModes = new HashMap<>();

    @Subcommand("autolink|al")
    @CommandPermission("rcregions.region.sign.autolink")
    public void autolink(Player player) {

        if (signLinkModes.containsKey(player.getUniqueId())) {
            signLinkModes.remove(player.getUniqueId());
            plugin.message(player, "autolink-deactivated");
        } else {
            signLinkModes.put(player.getUniqueId(), new SignLinkMode());
            plugin.message(player, "autolink-activated");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(PlayerInteractEvent event) {

        if (!signLinkModes.containsKey(event.getPlayer().getUniqueId())) {
            return;
        }

        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(new BukkitWorld(event.getPlayer().getWorld()));
        Block block = event.getClickedBlock();
        if (regionManager == null || block == null) {
            return;
        }

        if (block.getState() instanceof Sign) {
            Region region = signLinkModes.get(event.getPlayer().getUniqueId()).region();
            if (region != null) {
                new RegionSign(region, event.getClickedBlock()).save();
                region.updateSigns();
                event.getPlayer().sendMessage(ChatColor.GREEN + "Die Region " + region.name() + " wurde mit dem Schild verknüpft.");
            }
            event.setCancelled(true);
            return;
        }

        List<String> regions = regionManager.getApplicableRegionsIDs(BlockVector3.at(block.getX(), block.getY(), block.getZ())).stream()
                .filter(s -> !plugin.getPluginConfig().getIgnoredRegions().contains(s))
                .collect(Collectors.toList());
        if (regions.isEmpty()) return;
        if (regions.size() > 1) {
            event.getPlayer().sendMessage(ChatColor.RED + "Es befinden sich mehrere Regionen an der Stelle: " + String.join("", regions));
            event.getPlayer().sendMessage(ChatColor.RED + "Bitte erstelle manuell ein Schild. 1. Zeile: [region] - 2. Zeile: Name der WorldGuard Region");
        } else {
            java.util.Optional<Region> region = Region.of(event.getPlayer().getWorld(), regions.get(0));
            if (region.isEmpty()) return;
            signLinkModes.get(event.getPlayer().getUniqueId()).region(region.get());
            event.getPlayer().sendMessage(ChatColor.GRAY + "Du hast die Region " + region.get().name() + " ausgewählt. Klicke jetzt auf ein Schild.");
        }
    }

    @Subcommand("set")
    @CommandPermission("rcregions.region.edit")
    public class SetCommands {

        @Subcommand("parent")
        @CommandAlias("group|area|stadtteil")
        @CommandCompletion("@regions @groups")
        public void setParent(Region region, RegionGroup group) {

            region.group(group).save();
            getCurrentCommandIssuer().sendMessage(ChatColor.GREEN + "Die Gruppe des Grundstücks " + region.name()
                    + " wurde erfolgreich zu " + group.name() + " (" + group.identifier() + ") geändert.");
        }
    }

    @Data
    @Accessors(fluent = true)
    public static class SignLinkMode {

        private Region region;
    }
}
