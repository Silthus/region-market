package net.silthus.regions;

import lombok.NonNull;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionPlayer;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.Locale;

public final class Messages {

    public static String[] formatRegionSign(@NonNull Region region, @Nullable RegionPlayer player) {

        String[] lines = new String[4];

        if (region.status() != Region.Status.OCCUPIED) {
            lines[0] = ChatColor.WHITE + region.name();
            lines[1] = ChatColor.GREEN + "- Verfügbar -";
            lines[2] = ChatColor.YELLOW + "Größe: " + ChatColor.AQUA + region.size() + "m²";
            lines[3] = ChatColor.YELLOW + "Kosten: " + ChatColor.AQUA + region.costs(player);
        } else {
            lines[0] = ChatColor.WHITE + region.name();
            lines[1] = ChatColor.RED + "- Belegt durch -";
            lines[2] = ChatColor.AQUA + region.owner().name();
            lines[3] = ChatColor.YELLOW + "seit " + ChatColor.AQUA
                    + DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                    .withLocale(Locale.GERMAN)
                    .withZone(ZoneId.systemDefault())
                    .format(region.whenModified());
        }

        return lines;
    }

    public static BaseComponent[] formatRegionInfo(@NonNull Region region, @NonNull RegionPlayer player) {

        Cost.Result canBuy = region.canBuy(player);
        ChatColor canBuyColor = canBuy.success() ? ChatColor.GREEN : ChatColor.RED;
        ComponentBuilder builder = new ComponentBuilder("\n")
                .append("Grundstück: ").bold(true).color(ChatColor.YELLOW)
                .append(region.name()).reset().color(canBuyColor)
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(formatRegionHover(region, player))))
                .append("Besitzer: ").reset().bold(true).color(ChatColor.YELLOW)
                .append(owner(region)).append("\n")
                .append("Kosten: ").reset().bold(true).color(ChatColor.YELLOW).append("\n");

        for (String cost : region.costs(player)) {
            builder.append(cost).color(ChatColor.AQUA).append("\n");
        }

    }

    public static BaseComponent[] formatRegionHover(@NonNull Region region, @NonNull RegionPlayer player) {

        Cost.Result canBuy = region.canBuy(player);
        ChatColor canBuyColor = canBuy.success() ? ChatColor.GREEN : ChatColor.RED;
        ComponentBuilder builder = new ComponentBuilder()
                .append("[Grundstück]").color(canBuyColor).append("\n")
                .append("Name: ").reset().bold(true).color(ChatColor.YELLOW)
                .append(region.name()).reset().color(canBuyColor).append("\n")
                .append("Besitzer: ").reset().bold(true).color(ChatColor.YELLOW)
                    .append(owner(region)).append("\n")
                .append("Kosten: ").reset().bold(true).color(ChatColor.YELLOW).append("\n");

        for (String cost : region.costs(player)) {
            builder.append(cost).color(ChatColor.AQUA).append("\n");
        }

        return builder.create();
    }

    public static BaseComponent[] region(@NonNull Region region) {

    }

    public static BaseComponent[] owner(@NonNull Region region) {

        ComponentBuilder builder = new ComponentBuilder();

        if (region.owner() != null) {
            builder.append("[").reset().color(ChatColor.GRAY)
                    .append(region.owner().name()).color(ChatColor.AQUA)
                    .event(playerHover(region.owner()))
                    .append("]").color(ChatColor.GRAY);
        } else {
            builder.append("N/A").color(ChatColor.GRAY);
        }

        return builder.create();
    }

    public static HoverEvent playerHover(@NonNull RegionPlayer player) {

        ComponentBuilder builder = new ComponentBuilder();
        ChatColor onlineColor;
        if (player.lastOnline().isAfter(Instant.now().minus(1, ChronoUnit.MONTHS))) {
            onlineColor = ChatColor.GREEN;
        } else {
            onlineColor = ChatColor.GRAY;
        }
        builder.append("--- ").color(ChatColor.GRAY)
                .append(player.name()).color(onlineColor).bold(true)
                .append(" ---").reset().color(ChatColor.GRAY).append("\n")
                .append("Zuletzt online: ").color(onlineColor);
    }
}
