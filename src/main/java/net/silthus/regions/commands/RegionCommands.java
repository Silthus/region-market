package net.silthus.regions.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.*;
import lombok.NonNull;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.milkbowl.vault.economy.Economy;
import net.silthus.regions.*;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@CommandAlias("sregions|sr|rcr|rcregions|sregionmarket|srm")
public class RegionCommands extends BaseCommand {

    private final RegionsPlugin plugin;
    private final Map<UUID, Region> queuedRegionBuys = new HashMap<>();

    public RegionCommands(RegionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Default
    @Subcommand("info")
    @CommandPermission("rcregions.region.info")
    @CommandCompletion("@regions")
    public void info(Player player, RegionPlayer regionPlayer, Region region) {

        player.spigot().sendMessage(Messages.regionInfo(region, regionPlayer));
    }


    @Subcommand("limits")
    @CommandPermission("rcregions.region.limits")
    @CommandCompletion("@players")
    public void limits(Player player, RegionPlayer regionPlayer) {

        player.spigot().sendMessage(Messages.limits(regionPlayer));
    }

    @Subcommand("buy")
    @CommandPermission("rcregions.region.buy")
    @CommandCompletion("@regions")
    public void buy(RegionPlayer player, Region region) {

        if (!getCurrentCommandIssuer().isPlayer()) {
            throw new InvalidCommandArgument("Dieser Befehl kann nur als Spieler ausgeführt werden.");
        }

        Optional<Player> bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer.isEmpty()) {
            throw new InvalidCommandArgument("Der Spieler " + player.name() + " ist nicht online oder konnte nicht gefunden werden.");
        }

        Cost.Result result = checkBuy(region, player);
        if (result.success()) {
            bukkitPlayer.get().spigot().sendMessage(new ComponentBuilder()
                    .append("Grundstück ").color(ChatColor.YELLOW)
                    .append(Messages.region(region, player))
                    .append(" für ").reset().color(ChatColor.YELLOW)
                    .append(plugin.getEconomy().format(result.price().total())).color(ChatColor.AQUA)
                    .append(" kaufen?").reset().color(ChatColor.YELLOW)
                    .append(" [JA]").reset().color(ChatColor.GREEN).bold(true)
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new ComponentBuilder()
                            .append("Klicken um das Grundstück zu kaufen.").italic(true).color(ChatColor.GRAY).create())))
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rcregions buyconfirm"))
                    .append(" [ABBRECHEN]").reset().color(ChatColor.RED).bold(true)
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new ComponentBuilder()
                            .append("Klicken um den Grundstückskauf abzubrechen.").italic(true).color(ChatColor.GRAY).create())))
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rcregions buyabort"))
                    .create()
            );
            queuedRegionBuys.put(player.id(), region);
            Bukkit.getScheduler().runTaskLater(plugin, () -> buyAbort(player), plugin.getPluginConfig().getBuyTimeTicks());
        }
    }

    @Subcommand("buyconfirm|confirm")
    @CommandPermission("rcregions.region.buy")
    public void buyConfirm(RegionPlayer player) {

        if (!getCurrentCommandIssuer().isPlayer()) {
            throw new InvalidCommandArgument("Dieser Befehl kann nur als Spieler ausgeführt werden.");
        }

        Optional<Player> bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer.isEmpty()) {
            throw new InvalidCommandArgument("Der Spieler " + player.name() + " ist nicht online oder konnte nicht gefunden werden.");
        }

        Region region = queuedRegionBuys.remove(player.id());

        if (region == null) {
            getCurrentCommandIssuer().sendMessage(ChatColor.RED + "Du hast aktuell keinen ausstehenden Grundstückskauf. Du musst erst auf ein Grundstücksschild klicken um es zu kaufen.");
            return;
        }

        Cost.Result result = checkBuy(region, player);

        region.buy(plugin, player);
        bukkitPlayer.get().spigot().sendMessage(new ComponentBuilder().append("Du hast das Grundstück ").color(ChatColor.GREEN)
                .append(Messages.region(region, player)).append(" für ").reset().color(ChatColor.GREEN)
                .append(plugin.getEconomy().format(result.price().total())).color(ChatColor.AQUA)
                .append(" gekauft.").reset().color(ChatColor.GREEN)
                .create());
    }

    @Subcommand("buyabort|abort|cancel")
    @CommandCompletion("@regions")
    @CommandPermission("rcregions.region.buy")
    public void buyAbort(RegionPlayer player) {

        Region region = queuedRegionBuys.remove(player.id());

        Optional<Player> bukkitPlayer = player.getBukkitPlayer();
        if (bukkitPlayer.isEmpty()) {
            throw new InvalidCommandArgument("Der Spieler " + player.name() + " ist nicht online oder konnte nicht gefunden werden.");
        }

        if (region != null) {
            bukkitPlayer.get().spigot().sendMessage(new ComponentBuilder().append("Der Grundstückskauf von ").color(ChatColor.RED)
                    .append(Messages.region(region, player)).color(ChatColor.GOLD)
                    .append(" wurde abgebrochen.").reset().color(ChatColor.RED)
                    .create());
        } else if (getCurrentCommandIssuer() != null) {
            bukkitPlayer.get().sendMessage(ChatColor.RED + "Du hast aktuell keinen ausstehenden Grundstückskauf. Du musst erst auf ein Grundstücksschild klicken um es zu kaufen.");
        }
    }

    @Subcommand("sell")
    @CommandPermission("rcregions.region.sell")
    public class Sell extends BaseCommand {

        private final Map<UUID, SellAction> sellActions = new HashMap<>();

        @Default
        @CommandCompletion("@regions")
        public void sell(RegionPlayer player, @Flags("owner") Region region) {

            if (!getCurrentCommandIssuer().isPlayer()) {
                throw new InvalidCommandArgument("Nur Spieler können Grundstücke verkaufen.");
            }

            Player bukkitPlayer = getCurrentCommandIssuer().getIssuer();
            bukkitPlayer.spigot().sendMessage(Messages.sell(region, player));
        }

        @Subcommand("server")
        @CommandPermission("rcregions.region.sell.server")
        @CommandCompletion("@regions @players")
        public void sellServer(Region region, RegionPlayer regionPlayer) {

            if (!getCurrentCommandIssuer().isPlayer()) {
                throw new InvalidCommandArgument("Nur Spieler können Grundstücke verkaufen.");
            }

            if (!getCurrentCommandIssuer().hasPermission(Constants.SELL_REGION_FOR_OTHERS_PERMISSION) && !region.isOwner(getCurrentCommandIssuer().getIssuer())) {
                throw new InvalidCommandArgument("Du bist nicht der Besitzer des Grundstücks.");
            }

            ComponentBuilder builder = new ComponentBuilder();

            if (!getCurrentCommandIssuer().getUniqueId().equals(regionPlayer.id())) {
                builder.append("[WARNUNG]").color(ChatColor.DARK_RED).append(" Du bist dabei die Region von ")
                        .append(regionPlayer.name()).color(ChatColor.GOLD).bold(true)
                        .event(Messages.playerHover(regionPlayer))
                        .append(" zu verkaufen.\n").color(ChatColor.RED);
            }

            builder.append("Willst du das Grundstück ").color(ChatColor.YELLOW)
                    .append(Messages.region(region, regionPlayer)).color(ChatColor.GOLD).bold(true)
                    .append(" für ").color(ChatColor.YELLOW).bold(false)
                    .append(plugin.getEconomy().format(region.sellServerPrice(regionPlayer))).color(ChatColor.AQUA)
                    .append(" an den ").color(ChatColor.YELLOW).append("Server").color(ChatColor.RED)
                    .append(" verkaufen?").color(ChatColor.YELLOW);

            builder.append(" [JA]").reset().color(ChatColor.GREEN)
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new ComponentBuilder()
                            .append("Klicken um das Grundstück an den Server zu verkaufen.").italic(true).color(ChatColor.GRAY).create())))
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rcregions sell confirm"))
                    .append(" [ABBRECHEN]").reset().color(ChatColor.RED)
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new ComponentBuilder()
                            .append("Klicken um den Grundstücksverkauf abzubrechen.").italic(true).color(ChatColor.GRAY).create())))
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rcregions sell abort"))
                    .create();

            ((Player) getCurrentCommandIssuer().getIssuer()).spigot().sendMessage(builder.create());

            sellActions.put(getCurrentCommandIssuer().getUniqueId(), new SellAction(regionPlayer, region, SellType.SERVER));
            Bukkit.getScheduler().runTaskLater(plugin, () -> sellAbort(getCurrentCommandIssuer().getIssuer()), plugin.getPluginConfig().getBuyTimeTicks());
        }

        @Subcommand("direct")
        @CommandPermission("rcregions.region.sell.direct")
        public void sellDirect(RegionPlayer player, Region region) {

            if (!getCurrentCommandIssuer().isPlayer()) {
                throw new InvalidCommandArgument("Nur Spieler können Grundstücke verkaufen.");
            }

            getCurrentCommandIssuer().sendMessage(ChatColor.RED + "Grundstücke können aktuell noch nicht direkt verkauft werden.");
        }

        @Subcommand("auction")
        @CommandPermission("rcregions.region.sell.auction")
        public void sellAuction(RegionPlayer player, Region region) {

            if (!getCurrentCommandIssuer().isPlayer()) {
                throw new InvalidCommandArgument("Nur Spieler können Grundstücke verkaufen.");
            }

            getCurrentCommandIssuer().sendMessage(ChatColor.RED + "Grundstücke können aktuell noch nicht als Auktion verkauft werden.");
        }

        @Subcommand("confirm")
        @CommandPermission("rcregions.region.sell")
        public void sellConfirm(Player player) {

            SellAction sellAction = sellActions.remove(player.getUniqueId());
            if (sellAction == null) {
                throw new InvalidCommandArgument("Du hast keinen anstehenden Grundstücksverkauf. Klicke ein Grundstücksschild an um es zu verkaufen.");
            }

            Economy economy = RegionsPlugin.instance().getEconomy();

            SellResult result = sellAction.run();
            player.spigot().sendMessage(new ComponentBuilder().append("Das Grundstück ").color(ChatColor.YELLOW)
                    .append(Messages.region(result.getRegion(), result.getRegionPlayer())).color(ChatColor.GOLD).bold(true)
                    .append(" wurde für ").color(ChatColor.YELLOW).bold(false)
                    .append(economy.format(result.getPrice())).color(ChatColor.AQUA)
                    .append(" an den Server verkauft.")
                    .create());
        }

        @Subcommand("abort")
        @CommandPermission("rcregions.region.sell")
        public void sellAbort(Player player) {

            SellAction sellAction = sellActions.remove(player.getUniqueId());
            if (sellAction != null) {
                player.spigot().sendMessage(new ComponentBuilder().append("Der Grundstücksverkauf von ").color(ChatColor.RED)
                        .append(Messages.region(sellAction.getRegion(), sellAction.getRegionPlayer())).color(ChatColor.GOLD).bold(true)
                        .append(" wurde abgebrochen.").bold(false).color(ChatColor.RED)
                        .create());
            }
        }
    }

    private Cost.Result checkBuy(@NonNull Region region, @NonNull RegionPlayer player) {
        Cost.Result canBuy = region.canBuy(player);
        if (canBuy.failure()) {
            throw new InvalidCommandArgument("Du kannst dieses Grundstück nicht kaufen: " + canBuy.error());
        }

        return canBuy;
    }

    public enum SellType {
        SERVER,
        DIRECT,
        AUCTION;
    }
}
