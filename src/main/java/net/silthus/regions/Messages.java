package net.silthus.regions;

import lombok.NonNull;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.milkbowl.vault.economy.Economy;
import net.silthus.regions.actions.BuyAction;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionGroup;
import net.silthus.regions.entities.RegionPlayer;
import net.silthus.regions.limits.PlayerLimit;
import net.silthus.regions.util.MathUtil;
import net.silthus.regions.util.TimeUtil;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Messages {

    public static String[] formatRegionSign(@NonNull Region region) {

        String[] lines = new String[4];
        Economy economy = RegionsPlugin.instance().getEconomy();

        if (region.status() != Region.Status.OCCUPIED) {
            lines[0] = ChatColor.WHITE + region.name();
            lines[1] = ChatColor.GREEN + "- Verfügbar -";
            lines[2] = ChatColor.GREEN + economy.format(region.basePrice()) + ChatColor.YELLOW + " | " + ChatColor.AQUA + region.size() + "m²";
            lines[3] = ChatColor.GRAY + "" + ChatColor.ITALIC + "Klick für Details.";
        } else {
            lines[0] = ChatColor.WHITE + region.name();
            lines[1] = ChatColor.RED + "- Belegt durch -";
            lines[2] = ChatColor.GOLD + region.owner().map(RegionPlayer::name).orElse("Unbekannt");
            lines[3] = ChatColor.YELLOW + "seit " + ChatColor.AQUA
                    + TimeUtil.formatDateTime(region.whenModified());
        }

        return lines;
    }

    public static BaseComponent[] regionInfo(@NonNull Region region, @NonNull RegionPlayer player) {

        return regionInfo(region, player, true);
    }

    public static BaseComponent[] regionInfo(@NonNull Region region, @NonNull RegionPlayer player, boolean showBuyInfo) {

        ComponentBuilder builder = new ComponentBuilder("\n")
                .append("Grundstück: ").color(ChatColor.YELLOW)
                    .append(region(region, player))
                    .append(" ").reset().append(showBuyInfo ? buy(region, player) : new BaseComponent[0])
                    .append("\n")
                .append("Größe: ").reset().color(ChatColor.YELLOW)
                    .append(region.size() + "m²").reset().color(ChatColor.AQUA)
                    .append(" | ").color(ChatColor.YELLOW)
                    .append(region.volume() + "m³").color(ChatColor.AQUA)
                    .append("\n")
                .append("Stadtteil: ").reset().color(ChatColor.YELLOW)
                    .append(group(region)).append("\n")
                .append("Besitzer: ").reset().color(ChatColor.YELLOW)
                    .append(owner(region)).append("\n")
                .append("Kosten: ").reset().color(ChatColor.YELLOW).append("\n")
                    .append(costs(region, player));

        return builder.create();
    }

    public static BaseComponent[] region(@NonNull Region region, @Nullable RegionPlayer player) {

        return new ComponentBuilder().reset()
                .append(region.name()).bold(true).color(ChatColor.GOLD)
                .event(regionHover(region, player))
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/region info " + region.name()))
                .create();
    }

    public static BaseComponent[] owner(@NonNull Region region) {

        ComponentBuilder builder = new ComponentBuilder();

        Optional<RegionPlayer> owner = region.owner();
        if (owner.isPresent()) {
            builder.reset()
                    .append(owner.get().name()).color(ChatColor.AQUA).bold(true)
                    .event(playerHover(owner.get()));
        } else {
            builder.reset().append("N/A").color(ChatColor.GRAY);
        }

        return builder.create();
    }

    public static BaseComponent[] group(@NonNull Region region) {

        ComponentBuilder builder = new ComponentBuilder();

        if (region.group() == null) {
            return builder.append("N/A").color(ChatColor.GRAY).italic(true).create();
        }

        return builder.append(region.group().name()).bold(true).color(ChatColor.DARK_AQUA)
                .event(groupHover(region.group())).create();
    }

    public static BaseComponent[] buy(@NonNull Region region, @NonNull RegionPlayer player) {

        ComponentBuilder builder = new ComponentBuilder();

        BuyAction.Result canBuy = new BuyAction(region, player).check();
        Economy economy = RegionsPlugin.instance().getEconomy();
        if (canBuy.success()) {
            return builder.append("[Kaufen]").color(ChatColor.GREEN)
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new ComponentBuilder()
                            .append("Grundstück: ").color(ChatColor.YELLOW)
                            .append(region.name()).color(ChatColor.GREEN).append("\n")
                            .append("Kosten: ").color(ChatColor.YELLOW).append("\n")
                            .append(costs(region, player)).append("\n")
                            .append("Klicken um das Grundstück zu kaufen.").reset().italic(true).color(ChatColor.GRAY)
                            .create())))
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format(Constants.BUY_REGION_COMMAND, region.id())))
                    .create();
        } else {
            if (canBuy.getStatuses().contains(BuyAction.Result.Status.OWNED_BY_SELF)) {
                return builder.append("[Verkaufen]").color(ChatColor.GREEN)
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Klicke um das Grundstück zu verkaufen. Es folgt ein Dialog mit mehr Details.")))
                        .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rcr sell " + region.id()))
                        .create();
            } else {
                for (BuyAction.Result.Status status : canBuy.getStatuses()) {
                    switch (status) {
                        case OWNED_BY_OTHER:
                            return builder.append("[?]").color(ChatColor.GRAY)
                                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new ComponentBuilder()
                                            .append("Dieses Grundstück gehört bereits ").color(ChatColor.RED)
                                            .append(region.owner().map(RegionPlayer::name).orElse("jemandem")).color(ChatColor.AQUA)
                                            .append(".").color(ChatColor.RED)
                                            .create()
                                    ))).create();
                        case LIMITS_REACHED:
                            return builder.append("[Kaufen]").color(ChatColor.DARK_RED)
                                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new ComponentBuilder()
                                            .append("Du hast dein Grundstückslimit erreicht.").color(ChatColor.RED).append("\n")
                                            .append(limits(player))
                                            .create()
                                    ))).create();
                        case NOT_ENOUGH_MONEY:
                            double balance = economy.getBalance(player.getOfflinePlayer());
                            return builder.append("[Kaufen]").color(ChatColor.RED).bold(true)
                                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new ComponentBuilder()
                                            .append("Du hast nicht genügend Geld um das Grundstück zu kaufen.").color(ChatColor.DARK_RED).append("\n")
                                            .append("Du benötigst mindestens ").color(ChatColor.YELLOW).italic(true)
                                            .append(canBuy.formattedPrice()).color(ChatColor.AQUA)
                                            .append(" hast aber nur ").color(ChatColor.YELLOW).italic(true)
                                            .append(economy.format(balance)).color(ChatColor.GREEN).append("\n")
                                            .append("Kosten: ").color(ChatColor.YELLOW).append("\n")
                                            .append(costs(region, player)).create()
                                    ))).create();
                        default:
                        case COSTS_NOT_MET:
                            return builder.append("[Kaufen]").color(ChatColor.RED).bold(true)
                                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new ComponentBuilder()
                                            .append("Du erfüllst nicht die Vorraussetzungen um das Grundstück zu kaufen.").color(ChatColor.DARK_RED).append("\n")
                                            .append("Kosten: ").color(ChatColor.YELLOW).append("\n")
                                            .append(costs(region, player)).create()
                                    ))).create();
                    }
                }
            }
        }

        return new BaseComponent[0];
    }

    public static BaseComponent[] sell(@NonNull Region region, @Nullable RegionPlayer player) {

        return new ComponentBuilder().append("-->  ").color(ChatColor.DARK_AQUA).append("[").color(ChatColor.YELLOW).append("VERKAUF").color(ChatColor.RED)
                .append("] ").color(ChatColor.YELLOW).append("--- ").color(ChatColor.DARK_AQUA)
                .append(region(region, player)).append(" --- ").reset().color(ChatColor.DARK_AQUA)
                .append("[").color(ChatColor.YELLOW).append("VERKAUF").color(ChatColor.RED).append("] ").color(ChatColor.YELLOW)
                .append("<---").color(ChatColor.DARK_AQUA).append("\n").reset()
                .append("--> ").color(ChatColor.DARK_AQUA)
                .append(" [").color(ChatColor.YELLOW).append("SERVER").color(ChatColor.GREEN)
                .event(sellServerHover(region))
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rcr sell server " + region.id()))
                .append("] ").reset().color(ChatColor.YELLOW)
                .append("--- ").color(ChatColor.DARK_AQUA).append("[").color(ChatColor.YELLOW)
                .append("DIREKT").color(ChatColor.GRAY).strikethrough(true)
                .event(sellDirectHover(region))
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rcr sell direct " + region.id()))
                .append("] ").reset().color(ChatColor.YELLOW)
                .append("--- ").color(ChatColor.DARK_AQUA).append("[").color(ChatColor.YELLOW)
                .append("AUKTION").color(ChatColor.GRAY).strikethrough(true)
                .event(sellAuctionHover(region))
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rcr sell auction " + region.id()))
                .append("] ").reset().color(ChatColor.YELLOW)
                .append("<---").color(ChatColor.DARK_AQUA)
                .create();
    }

    public static HoverEvent sellServerHover(@NonNull Region region) {

        Economy economy = RegionsPlugin.instance().getEconomy();

        BaseComponent[] msg = new ComponentBuilder("Du kannst dein Grundstück für ").color(ChatColor.WHITE)
                .append(MathUtil.toPercent(region.group().sellModifier())).color(ChatColor.GREEN)
                .append(" des Basispreises (").color(ChatColor.WHITE).append(economy.format(region.basePrice())).color(ChatColor.GRAY)
                .append(") an den Server verkaufen: ").color(ChatColor.WHITE)
                .append(economy.format(region.basePrice() * region.group().sellModifier())).color(ChatColor.GREEN)
                .append("\n\n")
                .append("Wenn du an den Server verkaufst erhältst du sofort das Geld und verlierst aber auch sofort alle Rechte auf dem Grundstück.").color(ChatColor.GRAY).append("\n\n")
                .append("Stelle sicher dass du vorher alle deine Kisten geleert hast.").color(ChatColor.RED).append("\n\n")
                .append("Klicke um dein Grundstück an den Server zu verkaufen.").color(ChatColor.GRAY).italic(true)
                .create();
        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(msg));
    }

    private static HoverEvent sellDirectHover(@NonNull Region region) {

        Economy economy = RegionsPlugin.instance().getEconomy();

        BaseComponent[] msg = new ComponentBuilder("Beim direkten Verkauf kannst du selbst einen Preis für dein Grundstück bestimmen.").append("\n\n")
                .append("Das Grundstück steht dann für andere Spieler frei zum Verkauf und sobald es gekauft wurde erhältst du dein Geld und verlierst die Rechte auf dem Grundstück.").append("\n")
                .append("Der Mindestpreis für das Grundstück darf nicht unterhalb des Basispreises von ")
                .append(economy.format(region.basePrice())).color(ChatColor.AQUA).append(" liegen.").reset().append("\n\n")
                .append("Der Käufer zahlt zusätzlich Steuern basierend auf der Menge seiner Grundstücke.").color(ChatColor.GRAY).append("\n\n")
                .append("Bedenke dass du zwar die Rechte behältst, dass Grundstück aber jederzeit gekauft werden kann und deine Rechte dann sofort weg sind.").color(ChatColor.RED).append("\n\n")
                .append("Klicke um dein Grundstück direkt an Spieler zu verkaufen.").color(ChatColor.GRAY).italic(true)
                .create();

        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(msg));
    }

    private static HoverEvent sellAuctionHover(@NonNull Region region) {

        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new ComponentBuilder("Diese Funktion steht aktuell noch nicht zur Verfügung.").create()));
    }

    private static HoverEvent groupHover(@Nullable RegionGroup group) {

        if (group == null) {
            return new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Der Stadtteil existiert nicht."));
        }
        ComponentBuilder builder = new ComponentBuilder();

        List<Region> regions = group.regions();
        long occupiedRegions = regions.stream().filter(region -> region.status() == Region.Status.OCCUPIED).count();

        double sellFactor = 1.0 - group.sellModifier();
        ChatColor sellColor;
        if (sellFactor <= 0.2) {
            sellColor = ChatColor.GREEN;
        } else if (sellFactor >= 0.8) {
            sellColor = ChatColor.RED;
        } else {
            sellColor = ChatColor.GOLD;
        }

        builder.append(group.name()).color(ChatColor.DARK_AQUA).append("\n")
                .append(group.description()).color(ChatColor.GRAY).italic(true).append("\n\n")
                .append("Regionen: ").reset().color(ChatColor.YELLOW)
                .append(occupiedRegions + "").color(ChatColor.AQUA).append("/").color(ChatColor.YELLOW)
                .append(regions.size() + "").color(ChatColor.RED).append(" (belegt/gesamt)").color(ChatColor.GRAY).italic(true).append("\n")
                .append("Verkaufssteuern: ").reset().color(ChatColor.YELLOW).append(MathUtil.toPercent(sellFactor)).color(sellColor);

        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(builder.create()));
    }

    public static BaseComponent[] costs(@NonNull Region region, @Nullable RegionPlayer player) {

        ComponentBuilder builder = new ComponentBuilder();
        List<Cost> costs = region.costs();
        if (costs.isEmpty()) {
            return builder.append(" - Kostenlos").color(ChatColor.GREEN).create();
        }

        for (Cost cost : costs) {
            Cost.Result check = cost.check(region, player);
            builder.append("  - ").reset().append(cost.display(region, player))
                    .bold(true);
            if (check.success()) {
                builder.color(ChatColor.GREEN).append("\n");
            } else {
                builder.color(ChatColor.RED).append("\n");
            }
        }

        return builder.create();
    }

    public static BaseComponent[] limits(@NonNull RegionPlayer player) {

        Optional<PlayerLimit> optional = RegionsPlugin.instance().getLimitsConfig().getPlayerLimit(player);
        if (optional.isEmpty()) {
            return new ComponentBuilder("Der Spieler hat keine Limits konfiguriert.").color(ChatColor.GRAY).create();
        }
        PlayerLimit limit = optional.get();
        return new ComponentBuilder()
                .append("--- [ ").color(ChatColor.DARK_AQUA).append("Grundstück Limits").color(ChatColor.GOLD)
                .append(" ] ---").color(ChatColor.DARK_AQUA).append("\n")
                .append(limit("Gesamt: ", player.regions().size(), limit.total())).append("\n")
                .append(limit("Stadtteile: ", player.regionGroups().size(), limit.groups())).append("\n")
                .append(groupLimits(limit)).append("\n")
                .create();
    }

    private static BaseComponent[] groupLimits(PlayerLimit limit) {

        RegionPlayer player = limit.player();

        ComponentBuilder builder = new ComponentBuilder();
        for (Map.Entry<String, Integer> entry : limit.groupRegions().entrySet()) {
            Optional<RegionGroup> group = RegionGroup.of(entry.getKey());
            group.ifPresent(regionGroup -> builder.append(" - ").color(ChatColor.GRAY)
                    .append(limit(regionGroup.name() + ": ", player.regions(regionGroup).size(), entry.getValue()))
            .append("\n"));
        }
        return builder.create();
    }

    private static BaseComponent[] limit(String text, int current, int max) {

        ComponentBuilder builder = new ComponentBuilder(text).reset().color(ChatColor.YELLOW);

        if (max == -1) {
            return builder.append(" unlimitiert").reset().color(ChatColor.AQUA).italic(true).create();
        } else {
            ChatColor color;
            double percentageUsed = (max * 1.0) / (current * 1.0);
            if (percentageUsed == 1.0) {
                color = ChatColor.DARK_RED;
            } else if (percentageUsed >= 0.95) {
                color = ChatColor.RED;
            } else if (percentageUsed >= 0.7) {
                color = ChatColor.GOLD;
            } else {
                color = ChatColor.GREEN;
            }

            return builder
                    .append(current + "").color(color)
                    .append("/").color(ChatColor.YELLOW)
                    .append(max + "").color(ChatColor.DARK_RED)
                    .create();
        }
    }

    public static HoverEvent regionHover(@NonNull Region region, @Nullable RegionPlayer player) {

        ComponentBuilder builder = new ComponentBuilder()
                .append("Grundstück: ").color(ChatColor.YELLOW)
                .append(region.name()).color(ChatColor.GOLD).append("\n")
                .append("Besitzer: ").color(ChatColor.YELLOW)
                .append(owner(region)).append("\n")
                .append("Kosten: ").reset().color(ChatColor.YELLOW).append("\n")
                .append(costs(region, player)).append("\n")
                .append("\nKlicken um die WorldGuard Informationen zu dem Grundstück anzuzeigen.")
                .reset().italic(true).color(ChatColor.GRAY);

        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(builder.create()));
    }

    public static HoverEvent playerHover(@Nullable RegionPlayer player) {

        if (player == null) {
            return new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Der Spieler wurde nicht gefunden."));
        }

        ComponentBuilder builder = new ComponentBuilder();
        if (player.lastOnline() != null) {
            ChatColor onlineColor;
            if (player.lastOnline().isAfter(Instant.now().minus(31, ChronoUnit.DAYS))) {
                onlineColor = ChatColor.GREEN;
            } else {
                onlineColor = ChatColor.GRAY;
            }
            builder.append("--- ").color(ChatColor.GRAY)
                    .append(player.name()).color(onlineColor).bold(true)
                    .append(" ---").reset().color(ChatColor.GRAY).append("\n");

            if (player.lastOnline() != null) {
                builder.append("Zuletzt online: ").color(ChatColor.YELLOW)
                        .append(TimeUtil.formatDateTime(player.lastOnline())).color(onlineColor).append("\n\n");
            }
        }

        builder.append(limits(player));

        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(builder.create()));
    }
}
