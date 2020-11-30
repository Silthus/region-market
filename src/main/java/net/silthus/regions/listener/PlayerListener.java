package net.silthus.regions.listener;

import net.silthus.regions.entities.RegionPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.time.Instant;

public class PlayerListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {

        RegionPlayer.getOrCreate(event.getPlayer()).lastOnline(Instant.now()).save();
    }
}
