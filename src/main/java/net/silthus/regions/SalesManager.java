package net.silthus.regions;

import net.silthus.regions.entities.RegionPlayer;
import net.silthus.regions.entities.Sale;
import net.silthus.regions.events.SoldRegionEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class SalesManager implements Listener {

    private final RegionsPlugin plugin;

    public SalesManager(RegionsPlugin plugin) {

        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {

        if (!plugin.getPluginConfig().isDisplaySalesLoginNotification()) {
            return;
        }

        final List<Sale> sales = RegionPlayer.getOrCreate(event.getPlayer()).activeSales();
        if (sales.isEmpty()) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            event.getPlayer().spigot().sendMessage(Messages.sales("Du verkaufst aktuell folgende Grundstücke: ", sales));
        }, plugin.getPluginConfig().getSalesLoginDelay());
    }

    @EventHandler(ignoreCancelled = true)
    public void showSoldSales(PlayerJoinEvent event) {

        List<Sale> sales = RegionPlayer.getOrCreate(event.getPlayer()).sales().stream()
                .filter(Sale::needsAcknowledgement)
                .collect(Collectors.toList());

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            event.getPlayer().spigot().sendMessage(Messages.sales("Du hast seit dem letzten Login folgende Grundstücke verkauft", sales));
            sales.forEach(sale -> sale.acknowledged(Instant.now()).save());
        }, plugin.getPluginConfig().getSalesLoginDelay() + 1L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onRegionSell(SoldRegionEvent event) {

        for (String command : plugin.getPluginConfig().getSellCommands()) {
            command = command.replace("%player%", event.getPlayer().name())
                    .replace("%region%", event.getRegion().name())
                    .replace("%wgregion%", event.getRegion().name())
                    .replace("%group%", event.getRegion().group().identifier());
            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
        }
    }
}
