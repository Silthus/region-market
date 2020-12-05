package net.silthus.regions;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class SalesManager implements Listener {

    private final RegionsPlugin plugin;

    public SalesManager(RegionsPlugin plugin) {

        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {


    }
}
