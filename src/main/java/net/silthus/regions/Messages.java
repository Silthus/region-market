package net.silthus.regions;

import lombok.NonNull;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.milkbowl.vault.economy.Economy;
import net.silthus.regions.entities.OwnedRegion;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionPlayer;
import net.silthus.regions.limits.PlayerLimit;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;

public final class Messages {

    public static String[] formatRegionSign(@NonNull Region region) {

        String[] lines = new String[4];

        if (region.status() != Region.Status.OCCUPIED) {
            lines[0] = ChatColor.WHITE + region.name();
            lines[1] = ChatColor.GREEN + "- Verfügbar -";
            lines[2] = region.size() + "m²" + ChatColor.YELLOW + " | " + ChatColor.AQUA + region.volume() + "m³";
            lines[3] = ChatColor.GRAY + "" + ChatColor.ITALIC + "Klick für Details.";
        } else {
            lines[0] = ChatColor.WHITE + region.name();
            lines[1] = ChatColor.RED + "- Belegt durch -";
            lines[2] = ChatColor.GOLD + region.owner().map(RegionPlayer::name).orElse("Unbekannt");
            lines[3] = ChatColor.YELLOW + "seit " + ChatColor.AQUA
                    + DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(Locale.GERMAN)
                    .withZone(ZoneId.systemDefault())
                    .format(region.whenModified());
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

    public static BaseComponent[] buy(@NonNull Region region, @NonNull RegionPlayer player) {

        ComponentBuilder builder = new ComponentBuilder();

        Cost.Result canBuy = region.canBuy(player);
        Economy economy = RegionsPlugin.instance().getEconomy();
        if (canBuy.success()) {
            return builder.append("[Kaufen]").color(ChatColor.GREEN).bold(true)
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new ComponentBuilder()
                            .append("Grundstück: ").color(ChatColor.YELLOW)
                            .append(region.name()).color(ChatColor.GREEN).append("\n")
                            .append("Kosten: ").color(ChatColor.YELLOW).append("\n")
                            .append(costs(region, player)).append("\n")
                            .append("Klicken um das Grundstück zu kaufen.").italic(true).color(ChatColor.GRAY)
                            .create())))
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format(Constants.BUY_REGION_COMMAND, region.id())))
                    .create();
        } else {
            if (canBuy.status() == Cost.ResultStatus.OWNED_BY_SELF) {
                return builder.append("[Verkaufen]").color(ChatColor.GRAY).strikethrough(true)
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Diese Option steht aktuell noch nicht zur Verfügung.")))
                        .create();
            } else {
                switch (canBuy.status()) {
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
                                        .append(economy.format(canBuy.price())).color(ChatColor.AQUA)
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

    public static BaseComponent[] costs(@NonNull Region region, @NonNull RegionPlayer player) {

        ComponentBuilder builder = new ComponentBuilder();
        for (Cost cost : region.costs()) {
            Cost.Result check = cost.check(region, player);
            if (check.success()) {
                builder.append("  - ").reset().append(cost.display(region, player)).reset().color(ChatColor.GREEN).append("\n");
            } else {
                builder.append("  - ").reset().append(cost.display(region, player)).reset().color(ChatColor.RED).bold(true)
                        .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new ComponentBuilder(check.error()).color(ChatColor.RED).create())))
                        .append("\n");
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
                .append("Gesamt: ").color(ChatColor.YELLOW).append(player.regions().size() + "").color(ChatColor.AQUA)
                .append("/").color(ChatColor.YELLOW).append(limit.total() + "").color(ChatColor.RED)
                .append(" Grundstücke").color(ChatColor.YELLOW).append("\n")
                .create();
        // TODO: add rest of the limits to the display
    }

    public static HoverEvent regionHover(@NonNull Region region, @Nullable RegionPlayer player) {

        ComponentBuilder builder = new ComponentBuilder()
                .append("Grundstück: ").reset().bold(true).color(ChatColor.YELLOW)
                .append(region.name()).reset().color(ChatColor.GOLD).append("\n")
                .append("Besitzer: ").reset().bold(true).color(ChatColor.YELLOW)
                .append(owner(region)).append("\n")
                .append("Kosten: ").reset().bold(true).color(ChatColor.YELLOW).append("\n");

        for (String cost : region.displayCosts(player)) {
            builder.append(cost).color(ChatColor.AQUA).append("\n");
        }

        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(builder.create()));
    }

    public static HoverEvent playerHover(@NonNull RegionPlayer player) {

        ComponentBuilder builder = new ComponentBuilder();
        ChatColor onlineColor;
        if (player.lastOnline().isAfter(Instant.now().minus(31, ChronoUnit.DAYS))) {
            onlineColor = ChatColor.GREEN;
        } else {
            onlineColor = ChatColor.GRAY;
        }
        builder.append("--- ").color(ChatColor.GRAY)
                .append(player.name()).color(onlineColor).bold(true)
                .append(" ---").reset().color(ChatColor.GRAY).append("\n")
                .append("Zuletzt online: ").color(onlineColor)
                .append(limits(player));

        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(builder.create()));
    }
}
