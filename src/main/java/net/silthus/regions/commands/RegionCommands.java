package net.silthus.regions.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import lombok.NonNull;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.silthus.regions.Cost;
import net.silthus.regions.Messages;
import net.silthus.regions.RegionsPlugin;
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
                    .append(plugin.getEconomy().format(result.price())).color(ChatColor.AQUA)
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
                .append(plugin.getEconomy().format(result.price())).color(ChatColor.AQUA)
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
                    .append(Messages.region(region, player))
                    .append(" wurde abgebrochen").reset().color(ChatColor.DARK_GRAY)
                    .create());
        } else if (getCurrentCommandIssuer() != null) {
            bukkitPlayer.get().sendMessage(ChatColor.RED + "Du hast aktuell keinen ausstehenden Grundstückskauf. Du musst erst auf ein Grundstücksschild klicken um es zu kaufen.");
        }
    }

    private Cost.Result checkBuy(@NonNull Region region, @NonNull RegionPlayer player) {
        Cost.Result canBuy = region.canBuy(player);
        if (canBuy.failure()) {
            throw new InvalidCommandArgument("Du kannst dieses Grundstück nicht kaufen: " + canBuy.error());
        }

        return canBuy;
    }
}
