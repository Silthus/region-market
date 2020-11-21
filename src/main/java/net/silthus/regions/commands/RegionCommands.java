package net.silthus.regions.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import net.silthus.regions.Cost;
import net.silthus.regions.RegionsPlugin;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionPlayer;
import org.bukkit.ChatColor;

@CommandAlias("sregions|sr|rcr|rcregions|sregionmarket|srm")
public class RegionCommands extends BaseCommand {

    private final RegionsPlugin plugin;

    public RegionCommands(RegionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Subcommand("buy")
    @CommandPermission("sregions.region.buy")
    @CommandCompletion("@regions")
    public void buy(RegionPlayer player, Region region) {

        Cost.Result result = checkBuy(player, region);
        if (result.success()) {
            getCurrentCommandIssuer().sendMessage(ChatColor.YELLOW + "Bestätige deinen Grundstückskauf für " + ChatColor.AQUA
                    + plugin.getEconomy().format(result.price()) + ChatColor.YELLOW + " mit /sregions buyconfirm " + region.name());
        }
    }

    @Subcommand("buyconfirm")
    @CommandCompletion("@regions")
    @CommandPermission("sregions.region.buy.confirm")
    public void buyConfirm(RegionPlayer player, Region region) {

        Cost.Result result = checkBuy(player, region);

        region.buy(player);
        getCurrentCommandIssuer().sendMessage(ChatColor.GREEN + "Du hast das Grundstück " + ChatColor.YELLOW + region.name()
                + ChatColor.GREEN + " für " + ChatColor.AQUA + plugin.getEconomy().format(result.price()) + ChatColor.GREEN + " gekauft.");
    }

    private Cost.Result checkBuy(RegionPlayer player, Region region) {
        Cost.Result canBuy = region.canBuy(player);
        if (canBuy.failure()) {
            throw new InvalidCommandArgument("Du kannst dieses Grundstück nicht kaufen: " + canBuy.error());
        }

        return canBuy;
    }
}
