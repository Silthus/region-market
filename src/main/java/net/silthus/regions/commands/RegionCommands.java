package net.silthus.regions.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Flags;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.milkbowl.vault.economy.Economy;
import net.silthus.regions.Constants;
import net.silthus.regions.Messages;
import net.silthus.regions.RegionsPlugin;
import net.silthus.regions.actions.BuyAction;
import net.silthus.regions.actions.RegionAction;
import net.silthus.regions.actions.SellAction;
import net.silthus.regions.actions.SellDirectAction;
import net.silthus.regions.actions.SellResult;
import net.silthus.regions.actions.SellServerAction;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionPlayer;
import net.silthus.regions.entities.Sale;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@CommandAlias("sregions|sr|rcr|rcregions|sregionmarket|srm")
public class RegionCommands extends BaseCommand {

    private final RegionsPlugin plugin;
    private final Map<UUID, BuyAction> buyActions = new HashMap<>();

    public RegionCommands(RegionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Default
    @Subcommand("info")
    @CommandPermission("rcregions.region.info")
    @CommandCompletion("@regions @players")
    public void info(Player player, Region region, RegionPlayer regionPlayer) {

        player.spigot().sendMessage(Messages.regionInfo(region, regionPlayer));
    }


    @Subcommand("limits")
    @CommandAlias("regionlimits|limits")
    @CommandPermission("rcregions.region.limits")
    @CommandCompletion("@players")
    public void limits(Player player, RegionPlayer regionPlayer) {

        player.spigot().sendMessage(Messages.limits(regionPlayer));
    }

    @Subcommand("buy")
    @CommandPermission("rcregions.region.buy")
    @CommandCompletion("@regions")
    public void buy(Player player, Region region, RegionPlayer regionPlayer) {

        if (!getCurrentCommandIssuer().isPlayer()) {
            throw new InvalidCommandArgument("Dieser Befehl kann nur als Spieler ausgeführt werden.");
        }

        if (!player.getUniqueId().equals(regionPlayer.id())
                && !player.hasPermission(Constants.Permissions.BUY_REGION_FOR_OTHERS)) {
            throw new InvalidCommandArgument("Du hast nicht genügend Rechte um Grundstücke für andere Spieler zu kaufen.");
        }

        BuyAction buyAction = new BuyAction(region, regionPlayer);
        BuyAction.Result result = buyAction.check();

        if (result.success()) {
            player.spigot().sendMessage(new ComponentBuilder()
                    .append("Grundstück ").color(ChatColor.YELLOW)
                    .append(Messages.region(region, regionPlayer))
                    .append(" für ").reset().color(ChatColor.YELLOW)
                    .append(result.formattedPrice()).color(ChatColor.AQUA)
                    .append(" kaufen?").reset().color(ChatColor.YELLOW)
                    .append(Messages.confirm(
                            "Klicken um das Grundstück zu kaufen.",
                            "/rcregions buyconfirm",
                            "Klicken um den Grundstückskauf abzubrechen.",
                            "/rcregions buyabort"
                    ))
                    .create()
            );
            buyActions.put(regionPlayer.id(), buyAction);
            Bukkit.getScheduler().runTaskLater(plugin, () -> buyAbort(player), plugin.getPluginConfig().getBuyTimeTicks());
        } else {
            player.spigot().sendMessage(new ComponentBuilder()
                    .append("Du erfüllst nicht die Vorraussetzungen um das Grundstück ").color(ChatColor.DARK_RED)
                    .append(Messages.region(result.getRegion(), result.getRegionPlayer())).color(ChatColor.GOLD).bold(true)
                    .append(" für ").bold(false).color(ChatColor.DARK_RED)
                    .append(result.formattedPrice()).color(ChatColor.AQUA)
                    .append(" zu kaufen.").color(ChatColor.DARK_RED)
                    .create());
        }
    }

    @Subcommand("buyconfirm|confirm")
    @CommandPermission("rcregions.region.buy")
    public void buyConfirm(Player player) {

        if (!getCurrentCommandIssuer().isPlayer()) {
            throw new InvalidCommandArgument("Dieser Befehl kann nur als Spieler ausgeführt werden.");
        }

        BuyAction buyAction = buyActions.remove(player.getUniqueId());
        if (buyAction == null) {
            getCurrentCommandIssuer().sendMessage(ChatColor.RED + "Du hast aktuell keinen ausstehenden Grundstückskauf. Du musst erst auf ein Grundstücksschild klicken um es zu kaufen.");
            return;
        }

        BuyAction.Result result = buyAction.run();

        if (result.failure()) {
            throw new InvalidCommandArgument("Du erfüllst nicht mehr die Vorraussetzungen um das Grundstück zu kaufen.");
        }

        player.spigot().sendMessage(new ComponentBuilder().append("Du hast das Grundstück ").color(ChatColor.GREEN)
                .append(Messages.region(result.getRegion(), result.getRegionPlayer())).append(" für ").reset().color(ChatColor.GREEN)
                .append(result.formattedPrice()).color(ChatColor.AQUA)
                .append(" gekauft.").reset().color(ChatColor.GREEN)
                .create());
    }

    @Subcommand("buyabort|abort|cancel")
    @CommandCompletion("@regions")
    @CommandPermission("rcregions.region.buy")
    public void buyAbort(Player player) {

        BuyAction buyAction = buyActions.remove(player.getUniqueId());

        if (buyAction != null) {
            player.spigot().sendMessage(new ComponentBuilder().append("Der Grundstückskauf von ").color(ChatColor.RED)
                    .append(Messages.region(buyAction.getRegion(), buyAction.getRegionPlayer())).color(ChatColor.GOLD)
                    .append(" wurde abgebrochen.").reset().color(ChatColor.RED)
                    .create());
        } else if (getCurrentCommandIssuer() != null) {
            player.sendMessage(ChatColor.RED + "Du hast aktuell keinen ausstehenden Grundstückskauf. Du musst erst auf ein Grundstücksschild klicken um es zu kaufen.");
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
                builder.append(Messages.sellOthersRegion(regionPlayer));
            }

            SellServerAction sellAction = new SellServerAction(region, regionPlayer);
            double sellServerPrice = sellAction.getPriceDetails().sellServerPrice();

            builder.append("Willst du das Grundstück ").color(ChatColor.YELLOW)
                    .append(Messages.region(region, regionPlayer)).color(ChatColor.GOLD).bold(true)
                    .append(" für ").color(ChatColor.YELLOW).bold(false)
                    .append(plugin.getEconomy().format(sellServerPrice)).color(ChatColor.AQUA)
                    .append(" an den ").color(ChatColor.YELLOW).append("Server").color(ChatColor.RED)
                    .append(" verkaufen?").color(ChatColor.YELLOW)
                    .append(Messages.confirm(
                    "Klicken um das Grundstück an den Server zu verkaufen.",
                    "/rcregions sell confirm",
                    "Klicken um den Grundstücksverkauf abzubrechen.",
                    "/rcregions sell abort"
                    ));

            ((Player) getCurrentCommandIssuer().getIssuer()).spigot().sendMessage(builder.create());

            sellActions.put(getCurrentCommandIssuer().getUniqueId(), sellAction);
            Bukkit.getScheduler().runTaskLater(plugin, () -> sellAbort(getCurrentCommandIssuer().getIssuer(), null), plugin.getPluginConfig().getBuyTimeTicks());
        }

        @Subcommand("direct")
        @CommandPermission("rcregions.region.sell.direct")
        @CommandCompletion("@regions @players")
        public void sellDirect(Region region, RegionPlayer regionPlayer) {

            if (!getCurrentCommandIssuer().isPlayer()) {
                throw new InvalidCommandArgument("Nur Spieler können Grundstücke verkaufen.");
            }

            if (!getCurrentCommandIssuer().hasPermission(Constants.SELL_REGION_FOR_OTHERS_PERMISSION) && !region.isOwner(getCurrentCommandIssuer().getIssuer())) {
                throw new InvalidCommandArgument("Du bist nicht der Besitzer des Grundstücks.");
            }

            ComponentBuilder builder = new ComponentBuilder();

            if (!getCurrentCommandIssuer().getUniqueId().equals(regionPlayer.id())) {
                builder.append(Messages.sellOthersRegion(regionPlayer));
            }

            new AnvilGUI.Builder()
                    .title("Grundstückspreis für " + region.name())
                    .text("Preis")
                    .onClose(player -> player.sendMessage(ChatColor.RED + "Bitte gebe einen Preis für das Grundstück ein um es zu verkaufen."))
                    .onComplete((player, s) -> {
                        try {
                            double price = Double.parseDouble(s);

                            SellDirectAction action = new SellDirectAction(region, regionPlayer, price);
                            sellActions.put(getCurrentCommandIssuer().getUniqueId(), action);

                            BaseComponent[] msg = builder.append("Willst du das Grundstück ").color(ChatColor.YELLOW)
                                    .append(Messages.region(region, regionPlayer)).color(ChatColor.GOLD).bold(true)
                                    .append(" für ").color(ChatColor.YELLOW).bold(false)
                                    .append(plugin.getEconomy().format(price)).color(ChatColor.AQUA)
                                    .append(" zum Verkauf für andere Spieler freigeben?").color(ChatColor.YELLOW)
                                    .append(Messages.confirm(
                                            "Klicken um das Grundstück zum Verkauf freizugeben.",
                                            "/rcregions sell confirm",
                                            "Klicken um den Grundstücksverkauf abzubrechen.",
                                            "/rcregions sell abort"
                                    )).append("\n").reset()
                                    .append("Du behältst bis zum Kauf durch einen anderen Spieler die Rechte auf deinem Grundstück.\n").color(ChatColor.GRAY).italic(true)
                                    .append("Das Grundstück kann jederzeit gekauft werden, auch wenn du nicht online bist, " +
                                            "und sämtlicher Besitzt der sich zu diesem Zeitpunkt darauf befindet geht an den Käufer über.").color(ChatColor.DARK_RED).italic(true)
                                    .create();
                            player.spigot().sendMessage(msg);

                            return AnvilGUI.Response.close();
                        } catch (NumberFormatException e) {
                            return AnvilGUI.Response.text(s + " ist keine Zahl. Bitte gebe einen gültigen Preis für das Grundstück ein.");
                        }
                    }).open(getCurrentCommandIssuer().getIssuer());
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

            ComponentBuilder builder = new ComponentBuilder().append("Das Grundstück ").color(ChatColor.YELLOW)
                    .append(Messages.region(result.getRegion(), result.getRegionPlayer())).color(ChatColor.GOLD).bold(true);

            if (sellAction instanceof SellServerAction) {
                player.spigot().sendMessage(builder
                        .append(" wurde für ").reset().color(ChatColor.YELLOW)
                        .append(economy.format(result.getPrice())).color(ChatColor.AQUA)
                        .append(" an den Server verkauft.").color(ChatColor.YELLOW)
                        .create());
            } else if (sellAction instanceof SellDirectAction) {
                player.spigot().sendMessage(builder
                        .append(" wurde zum Verkauf an andere Spieler freigegeben.").color(ChatColor.YELLOW).append("\n")
                        .append("Du erhältst eine Nachricht sobald dein Grundstück gekauft wurde.").color(ChatColor.YELLOW).append("\n")
                        .append("Du kannst den Verkauf des Grundstücks jederzeit mit ").color(ChatColor.GRAY)
                        .append("/rcr sell abort " + sellAction.getRegion().name()).bold(true).color(ChatColor.DARK_GRAY)
                        .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/rcr sell abort " + sellAction.getRegion().name()))
                        .append(" abbrechen.").reset().color(ChatColor.GRAY)
                        .create());
            }
        }

        @Subcommand("abort")
        @CommandPermission("rcregions.region.sell")
        @CommandCompletion("@sales")
        public void sellAbort(Player player, @Optional Region region) {

            RegionAction sellAction = sellActions.remove(player.getUniqueId());
            if (sellAction != null) {
                player.spigot().sendMessage(new ComponentBuilder().append("Der Grundstücksverkauf von ").color(ChatColor.RED)
                        .append(Messages.region(sellAction.getRegion(), sellAction.getRegionPlayer())).color(ChatColor.GOLD).bold(true)
                        .append(" wurde abgebrochen.").bold(false).color(ChatColor.RED)
                        .create());
            } else if (region != null) {
                java.util.Optional<Sale> optionalSale = Sale.getActiveSale(region);
                optionalSale.ifPresent(sale -> {
                    sale.abort();
                    player.spigot().sendMessage(new ComponentBuilder().append("Der Grundstücksverkauf von ").color(ChatColor.RED)
                            .append(Messages.region(sale.region())).color(ChatColor.GOLD).bold(true)
                            .append(" wurde abgebrochen.").bold(false).color(ChatColor.RED)
                            .create());
                });
            }
        }

        @Subcommand("ack|acknowledge")
        @CommandPermission("rcregions.region.sell")
        @CommandCompletion("@sales")
        public void sellAck(Player player, Region region) {

            Sale.of(region).stream()
                    .filter(Sale::needsAcknowledgement)
                    .forEach(sale -> {
                        sale.acknowledged(Instant.now()).save();
                        player.sendMessage(ChatColor.GREEN + "Der Verkauf der Region " + region.name() + " wurde bestätigt.");
                    });
        }
    }

    public enum SellType {
        SERVER,
        DIRECT,
        AUCTION;
    }
}
