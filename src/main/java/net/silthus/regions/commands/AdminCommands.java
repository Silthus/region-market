package net.silthus.regions.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import com.google.common.base.Strings;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.silthus.regions.Messages;
import net.silthus.regions.RegionsPlugin;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionGroup;
import net.silthus.regions.entities.RegionPlayer;
import net.silthus.regions.entities.RegionSign;
import net.silthus.regions.events.CreateRegionEvent;
import net.silthus.regions.events.CreatedRegionEvent;
import net.silthus.regions.events.DeleteRegionEvent;
import net.silthus.regions.events.DeletedRegionEvent;
import net.silthus.regions.util.Enums;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
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
import java.util.stream.Stream;

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
    @CommandCompletion("@wgRegions @groups auto|free|static")
    @CommandPermission("rcregions.region.create")
    public void create(Player player, @Optional String region, @Optional RegionGroup group, @Optional @Default("auto") String price) {

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
        ProtectedRegion wgParent = protectedRegion.getParent();
        if (group == null && plugin.getPluginConfig().isAutoMapParent()) {
            RegionGroup.ofWorldGuardRegion(wgParent).ifPresent(sellRegion::group);
        } else if (group == null) {
            sellRegion.group(RegionGroup.getDefault());
        } else {
            sellRegion.group(group);
        }

        if (Strings.isNullOrEmpty(price)) {
            if (sellRegion.group().priceType() != null) {
                sellRegion.priceType(sellRegion.group().priceType());
            } else {
                sellRegion.priceType(Region.PriceType.DYNAMIC);
            }
        } else if (price.startsWith("dynamic") || price.startsWith("auto")) {
            sellRegion.priceType(Region.PriceType.DYNAMIC);
            if (!Strings.isNullOrEmpty(price)) {
                String[] strings = price.split(":");
                if (strings.length > 1) {
                    try {
                        double sellPrice = Double.parseDouble(strings[1]);
                        if (sellPrice == 0) {
                            sellRegion.priceType(Region.PriceType.FREE);
                        }
                        sellRegion.price(sellPrice);
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

        CreateRegionEvent event = new CreateRegionEvent(sellRegion, protectedRegion);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            throw new InvalidCommandArgument("Die Erstellung der Region wurde von einem Plugin abgebrochen.");
        }

        sellRegion.save();
        player.spigot().sendMessage(new ComponentBuilder("Das Grundstück ").color(net.md_5.bungee.api.ChatColor.GREEN)
                .append(Messages.region(sellRegion, null)).append(" wurde erstellt und kann jetzt gekauft werden.").reset().color(net.md_5.bungee.api.ChatColor.GREEN)
                .create());

        if (protectedRegion instanceof ProtectedPolygonalRegion) {
            BaseComponent[] msg = new ComponentBuilder().append("Bei der Region handelt es sich um eine Polygon Region und das Volumen kann nicht richtig berechnet werden. ")
                    .color(net.md_5.bungee.api.ChatColor.RED).append("Bitte setze die ")
                    .append("[m²]").color(net.md_5.bungee.api.ChatColor.AQUA).bold(true)
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Klicken um die Größe des Grundstücks festzulegen.")))
                    .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/rcra set size " + sellRegion.name() + " "))
                    .append(" oder ").reset().color(net.md_5.bungee.api.ChatColor.RED)
                    .append("[m³]").color(net.md_5.bungee.api.ChatColor.AQUA).bold(true)
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Klicken um das Volumen des Grundstücks festzulegen.")))
                    .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/rcra set volume " + sellRegion.name() + " "))
                    .create();
            player.spigot().sendMessage(msg);
        }

        Bukkit.getPluginManager().callEvent(new CreatedRegionEvent(sellRegion, protectedRegion));
    }

    @Subcommand("delete|del|remove")
    @CommandCompletion("@regions")
    @CommandPermission("rcregions.region.delete")
    public void delete(Region region) {

        DeleteRegionEvent event = new DeleteRegionEvent(region);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            throw new InvalidCommandArgument("Das Löschen der Region wurde von einem Plugin abgebrochen.");
        }

        region.delete();
        getCurrentCommandIssuer().sendMessage(ChatColor.GREEN + "Das Grundstück " + region.name() + " wurde gelöscht.");
        Bukkit.getPluginManager().callEvent(new DeletedRegionEvent(region));
    }

    @Subcommand("reset")
    @CommandCompletion("@regions @schematics")
    @CommandPermission("rcregions.region.reset")
    public void reset(Region region, @Optional String schematic) {

        region.activeSale().ifPresent(sale -> sale.abort(true));
        region.owner(null).status(Region.Status.FREE).save();
        if (!Strings.isNullOrEmpty(schematic)) {
            try {
                plugin.getSchematicManager().restore(region, schematic);
            } catch (Exception e) {
                e.printStackTrace();
                throw new InvalidCommandArgument(e.getMessage());
            }
        }
    }

    @Getter
    private final Map<UUID, SignLinkMode> signLinkModes = new HashMap<>();

    @Subcommand("autolink|al")
    @CommandPermission("rcregions.region.sign.autolink")
    public void autolink(Player player) {

        if (signLinkModes.containsKey(player.getUniqueId())) {
            signLinkModes.remove(player.getUniqueId());
            player.sendMessage(ChatColor.RED + "[AutoLink] deaktiviert.");
        } else {
            signLinkModes.put(player.getUniqueId(), new SignLinkMode());
            player.sendMessage(ChatColor.GREEN + "[AutoLink] aktiviert.");
        }
    }

    @Subcommand("wgparents")
    @CommandCompletion("@groups override")
    @CommandPermission("rcregions.region.worldguard.setparent")
    public void wgParents(RegionGroup group, @Optional String mode) {

        if (group.worldGuardRegion().isEmpty()) {
            throw new InvalidCommandArgument("Die WorldGuard Region der Gruppe " + group.name() + " existiert nicht.");
        }

        group.regions().stream()
                .map(Region::worldGuardRegion)
                .flatMap(protectedRegion -> protectedRegion.stream().flatMap(Stream::of))
                .forEach(protectedRegion -> {
                    try {
                        if (protectedRegion.getParent() == null || "override".equalsIgnoreCase(mode)) {
                            protectedRegion.setParent(group.worldGuardRegion().get());
                        }
                    } catch (ProtectedRegion.CircularInheritanceException e) {
                        getCurrentCommandIssuer().sendMessage(ChatColor.RED + "WorldGuard Region für "
                                + protectedRegion.getId() + " kann nicht gesetzt werden: " + e.getMessage());
                    }
                });
        getCurrentCommandIssuer().sendMessage(ChatColor.GREEN + "Die Parent WorldGuard Regionen für alle Grundstücke in der "
                + group.name() + " Gruppe wurden erfolgreich gesetzt.");
    }

    @Subcommand("autogroup")
    @CommandCompletion("@groups all|existing")
    @CommandPermission("rcregions.region.worldguard.autoparent")
    public void autoParent(RegionGroup group, @Default("existing") String scope) {

        java.util.Optional<ProtectedRegion> parent = group.worldGuardRegion();
        if (parent.isEmpty()) {
            throw new InvalidCommandArgument("Die Gruppe " + group.name() + " hat keine validate WorldGuard Region konfiguriert.");
        }
        World world = Bukkit.getWorld(group.world());
        if (world == null) {
            throw new InvalidCommandArgument("Die Welt " + group.world() + " aus der Gruppenconfig " + group.name() + " existiert nicht.");

        }
        RegionManager regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(new BukkitWorld(world));
        if (regionManager == null) {
            throw new InvalidCommandArgument("Der WorldGuard Regionmanager für die Welt " + world.getName() + " wurde nicht gefunden.");
        }

        List<ProtectedRegion> regions = regionManager.getRegions().values().stream()
                .filter(region -> region.getParent() != null)
                .filter(region -> parent.get().equals(region.getParent()))
                .collect(Collectors.toList());

        int count = 0;
        int newRegions = 0;
        for (ProtectedRegion protectedRegion : regions) {
            java.util.Optional<Region> optionalRegion = Region.of(protectedRegion);
            if (optionalRegion.isEmpty() && "all".equalsIgnoreCase(scope)) {
                new Region(world, protectedRegion.getId())
                        .group(group)
                        .save();
                newRegions++;
                count++;
            } else if (optionalRegion.isPresent()) {
                optionalRegion.get().group(group).save();
                count++;
            }
        }

        getCurrentCommandIssuer().sendMessage(ChatColor.GREEN + "Die Gruppe " + group.name()
                + " wurde bei " + count + "/" + regions.size() + " (neu: " + newRegions + ") als Gruppe gesetzt.");
    }

    @Subcommand("set")
    @CommandPermission("rcregions.region.edit")
    public class SetCommands extends BaseCommand {

        @Subcommand("parent")
        @CommandCompletion("@regions @groups")
        public void setParent(Region region, RegionGroup group) {

            region.group(group).save();

            if (plugin.getPluginConfig().isAutosetWorldGuardParent()) {
                group.worldGuardRegion().ifPresent(parent -> {
                    region.worldGuardRegion().ifPresent(protectedRegion -> {
                        try {
                            protectedRegion.setParent(parent);
                            getCurrentCommandIssuer().sendMessage(ChatColor.GREEN + "Der WorldGuard Parent für die Region wurde auf " + parent.getId() + " gesetzt.");
                        } catch (ProtectedRegion.CircularInheritanceException e) {
                            getCurrentCommandIssuer().sendMessage("Cannot set circular parent of region: " + e.getMessage());
                            e.printStackTrace();
                        }
                    });
                });
            }

            getCurrentCommandIssuer().sendMessage(ChatColor.GREEN + "Die Gruppe des Grundstücks " + region.name()
                    + " wurde erfolgreich zu " + group.name() + " (" + group.identifier() + ") geändert.");
        }

        @Subcommand("price")
        @CommandCompletion("@regions free|static|dynamic")
        public void setPriceType(Region region, String priceType, @Optional String price) {

            Region.PriceType type = Enums.searchEnum(Region.PriceType.class, priceType);
            if (type == null) {
                throw new InvalidCommandArgument("Der Preis Typ " + priceType + " existiert nicht. Bitte nutze einen der folgenden: free, static, dynamic");
            }

            if (!Strings.isNullOrEmpty(price)) {
                try {
                    double value = Double.parseDouble(price);
                    region.priceType(type).price(value).save();
                } catch (NumberFormatException e) {
                    throw new InvalidCommandArgument(e.getMessage());
                }
            } else {
                region.priceType(type).save();
            }
            String message = ChatColor.GREEN + "Der Preis des Grundstücks wird jetzt ";
            switch (type) {
                case STATIC:
                    message += "auf " + plugin.getEconomy().format(region.price()) + " festgelegt.";
                    break;
                case DYNAMIC:
                    message += "automatisch berechnet.";
                    break;
                case FREE:
                    message = ChatColor.GREEN + "Das Grundstück ist jetzt kostenlos.";
                    break;
            }
            getCurrentCommandIssuer().sendMessage(message);
        }

        @Subcommand("size")
        @CommandCompletion("@regions")
        public void setSize(Region region, long size) {

            region.size(size).save();
            getCurrentCommandIssuer().sendMessage(ChatColor.GREEN + "The Größe des Grundstücks wurde auf "
                    + ChatColor.AQUA + region.size() + "m² " + ChatColor.GREEN + " und "
                    + ChatColor.AQUA + region.volume() + "m³ " + ChatColor.GREEN + " gesetzt."
            );
        }

        @Subcommand("volume")
        @CommandCompletion("@regions")
        public void setVolume(Region region, long volume) {

            region.volume(volume).save();
            getCurrentCommandIssuer().sendMessage(ChatColor.GREEN + "The Größe des Grundstücks wurde auf "
                    + ChatColor.AQUA + region.size() + "m² " + ChatColor.GREEN + " und "
                    + ChatColor.AQUA + region.volume() + "m³ " + ChatColor.GREEN + " gesetzt."
            );
        }

        @Subcommand("factor")
        @CommandCompletion("@regions")
        public void setFactor(Player player, Region region, double factor) {

            region.priceMultiplier(factor).save();
            player.spigot().sendMessage(new ComponentBuilder()
                .append("Der individuelle Preis-Faktor des Grundstücks ").color(net.md_5.bungee.api.ChatColor.GREEN)
                    .append(Messages.region(region, null)).color(net.md_5.bungee.api.ChatColor.GOLD).bold(true)
                    .append(" wurde auf ").reset().color(net.md_5.bungee.api.ChatColor.GREEN)
                    .append("x" + factor).color(net.md_5.bungee.api.ChatColor.AQUA)
                    .append(" gesetzt.").color(net.md_5.bungee.api.ChatColor.GREEN)
                    .create());
        }

        @Subcommand("owner")
        @CommandCompletion("@regions @players")
        public void setOwner(Player player, Region region, RegionPlayer regionPlayer) {

            region.owner(regionPlayer).status(Region.Status.OCCUPIED).save();
            player.spigot().sendMessage(new ComponentBuilder("Der Besitzer des Grundstücks ").color(net.md_5.bungee.api.ChatColor.GREEN)
                    .append(Messages.region(region)).color(net.md_5.bungee.api.ChatColor.GOLD).bold(true)
                    .append(" wurde zu ").reset().color(net.md_5.bungee.api.ChatColor.GREEN)
                    .append(Messages.player(regionPlayer)).color(net.md_5.bungee.api.ChatColor.AQUA).bold(true)
                    .append(" geändert.").reset().color(net.md_5.bungee.api.ChatColor.GREEN)
                    .create());
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

    @Data
    @Accessors(fluent = true)
    public static class SignLinkMode {

        private Region region;
    }
}
