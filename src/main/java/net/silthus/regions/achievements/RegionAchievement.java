package net.silthus.regions.achievements;

import de.raidcraft.achievements.AbstractAchievementType;
import de.raidcraft.achievements.AchievementContext;
import de.raidcraft.achievements.Progressable;
import de.raidcraft.achievements.TypeFactory;
import de.raidcraft.achievements.entities.AchievementPlayer;
import de.raidcraft.achievements.events.AchievementProgressChangeEvent;
import de.raidcraft.achievements.types.CountAchievement;
import de.raidcraft.economy.events.PlayerBalanceChangedEvent;
import de.raidcraft.economy.wrapper.Economy;
import net.silthus.regions.Messages;
import net.silthus.regions.RegionsPlugin;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionGroup;
import net.silthus.regions.entities.RegionPlayer;
import net.silthus.regions.events.BoughtRegionEvent;
import net.silthus.regions.events.SoldRegionEvent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import shadow.RCAchievements.text.adventure.text.Component;
import shadow.RCAchievements.text.adventure.text.TextComponent;

import java.util.*;
import java.util.stream.Stream;

import static de.raidcraft.achievements.Messages.Colors.*;
import static shadow.RCAchievements.text.adventure.text.Component.newline;
import static shadow.RCAchievements.text.adventure.text.Component.text;

public class RegionAchievement extends AbstractAchievementType implements Progressable {

    public static class Factory implements TypeFactory<RegionAchievement>, Listener {

        @Override
        public String identifier() {
            return "region";
        }

        @Override
        public Class<RegionAchievement> typeClass() {
            return RegionAchievement.class;
        }

        @Override
        public RegionAchievement create(AchievementContext context) {
            return new RegionAchievement(context);
        }

    }

    int count = 0;
    boolean requireAll = false;
    boolean showMoneyProgress = false;
    final List<RegionGroup> groups = new ArrayList<>();
    final List<Region> regions = new ArrayList<>();

    protected RegionAchievement(AchievementContext context) {
        super(context);
    }

    @Override
    public boolean load(ConfigurationSection config) {

        this.count = config.getInt("count", count);
        this.showMoneyProgress = config.getBoolean("money_progress", false);
        this.requireAll = config.getBoolean("require_all", false);

        for (String groupName : config.getStringList("groups")) {
            RegionGroup.of(groupName).ifPresent(groups::add);
        }

        for (String region : config.getStringList("regions")) {
            Region.of(region).ifPresent(regions::add);
        }

        return super.load(config);
    }

    @Override
    public Component progressText(AchievementPlayer player) {

        TextComponent.Builder builder = text();
        RegionPlayer regionPlayer = RegionPlayer.getOrCreate(player.offlinePlayer());
        if (showMoneyProgress) {
            OptionalDouble lowestPrice = getLowestPrice(regionPlayer);
            if (lowestPrice.isPresent()) {
                Economy economy = Economy.get();
                builder.append(text("Benötigtes Geld: ", TEXT))
                        .append(text(economy.format(economy.getBalance(player.offlinePlayer())), DARK_HIGHLIGHT))
                        .append(text("/", ACCENT))
                        .append(text(economy.format(lowestPrice.getAsDouble()), HIGHLIGHT))
                        .append(text(" " + economy.currencyNamePlural(), TEXT));
            }
        }

        if (count > 0) {
            builder.append(newline())
                    .append(text("Anzahl: ", TEXT))
                    .append(text(getCurrentCount(regionPlayer), DARK_HIGHLIGHT))
                    .append(text("/", ACCENT))
                    .append(text(count, HIGHLIGHT))
                    .append(text(" Grundstücke", TEXT));
        }
        if (!groups.isEmpty()) {
            builder.append(newline())
                    .append(text("Stadtteile: ", TEXT));
            for (RegionGroup group : groups) {
                builder.append(text("[", NOTE))
                        .append(text(group.name(), regionPlayer.regionGroups().contains(group) ? SUCCESS : ERROR))
                        .append(text("]", NOTE));
            }
        }
        if (!regions.isEmpty()) {
            builder.append(newline())
                    .append(text("Grundstücke: ", TEXT));
            for (Region region : regions) {
                builder.append(text("[", NOTE))
                        .append(text(region.name(), regionPlayer.regions().contains(region) ? SUCCESS : ERROR))
                        .append(text("]", NOTE));
            }
        }

        return builder.build();
    }

    @Override
    public float progress(AchievementPlayer player) {

        OfflinePlayer offlinePlayer = player.offlinePlayer();
        RegionPlayer regionPlayer = RegionPlayer.getOrCreate(offlinePlayer);
        if (showMoneyProgress) {
            OptionalDouble lowestPrice = getLowestPrice(regionPlayer);
            if (lowestPrice.isPresent() && !Economy.get().has(offlinePlayer, lowestPrice.getAsDouble())) {
                double percent = Economy.get().getBalance(offlinePlayer) / lowestPrice.getAsDouble();
                if (percent > 1.0f) percent = 1.0f;
                return (float) percent;
            }
        }

        long regionGroupCount = regionPlayer.regionGroups().stream()
                .filter(groups::contains)
                .count();
        float groupPercent = groups.isEmpty() ? 1.0f : regionGroupCount * 1.0f / groups.size();
        long regionCount = regionPlayer.regions().stream()
                .filter(regions::contains)
                .count();
        float regionPercent = regions.isEmpty() ? 1.0f : regionCount * 1.0f / regions.size();

        return (groupPercent + regionPercent) / 2.0f;
    }

    @EventHandler(ignoreCancelled = true)
    public void onRegionBuy(BoughtRegionEvent event) {

        if (notApplicable(event.getRegionPlayer().getOfflinePlayer())) return;

        Bukkit.getPluginManager().callEvent(
                new AchievementProgressChangeEvent(
                        playerAchievement(AchievementPlayer.of(event.getRegionPlayer().getOfflinePlayer())),
                        this
                )
        );

        check(event.getRegionPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onRegionSell(SoldRegionEvent event) {

        if (notApplicable(event.getPlayer().getOfflinePlayer())) return;

        check(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {

        if (notApplicable(event.getPlayer())) return;

        Bukkit.getScheduler().runTaskLater(RegionsPlugin.instance(),
                () -> check(RegionPlayer.getOrCreate(event.getPlayer())),
                400L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBalanceChange(PlayerBalanceChangedEvent event) {

        if (showMoneyProgress) {
            Bukkit.getPluginManager().callEvent(
                    new AchievementProgressChangeEvent(
                            playerAchievement(AchievementPlayer.of(event.getPlayer())),
                            this
                    )
            );
        }
    }

    void check(RegionPlayer player) {

        boolean countReached = count < 1 || player.ownedRegions().size() >= count;
        boolean groupsReached = groups.isEmpty()
                || (
                        (requireAll && player.regionGroups().containsAll(groups))
                        || player.regionGroups().stream().anyMatch(groups::contains)
                    );
        boolean regionsReached = regions.isEmpty()
                || (
                (requireAll && player.regions().containsAll(regions))
                        || player.regions().stream().anyMatch(regions::contains)
        );

        if (countReached && groupsReached && regionsReached) {
            addTo(player(player.getOfflinePlayer()));
        } else {
            removeFrom(player(player.getOfflinePlayer()));
        }
    }

    OptionalDouble getLowestPrice(RegionPlayer player) {

        return Stream.concat(
                groups.stream()
                        .map(RegionGroup::regions)
                        .flatMap(Collection::stream),
                regions.stream())
                .filter(region -> region.status() == Region.Status.FREE)
                .mapToDouble(region -> region.priceDetails(player).total())
                .min();
    }

    long getCurrentCount(RegionPlayer player) {

        if (regions.isEmpty() && groups.isEmpty()) return player.regions().size();

        return Stream.concat(regions.stream(),
                groups.stream()
                        .map(RegionGroup::regions)
                        .flatMap(Collection::stream)
        ).filter(player::isOwner).count();
    }
}
