package net.silthus.regions.listener;

import net.silthus.regions.RegionsPlugin;
import net.silthus.regions.entities.Region;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Optional;

public class ClickListener implements Listener {

    private final RegionsPlugin plugin;

    public ClickListener(RegionsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(PlayerInteractEvent event) {

        if (!(event.getClickedBlock() instanceof Sign)) {
            return;
        }

        Optional<Region> region = Region.of(event.getClickedBlock().getLocation());
    }
}
