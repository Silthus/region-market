package net.silthus.regions.listener;

import net.silthus.regions.Constants;
import net.silthus.regions.Messages;
import net.silthus.regions.RegionsPlugin;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionPlayer;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Optional;

public class SignClickListener implements Listener {

    private final RegionsPlugin plugin;

    public SignClickListener(RegionsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(PlayerInteractEvent event) {

        if (event.getClickedBlock() == null || event.getClickedBlock().getType() == Material.AIR) {
            return;
        }

        if (!(event.getClickedBlock().getState() instanceof Sign)) {
            return;
        }

        Optional<Region> optionalRegion = Region.atSign(event.getClickedBlock().getLocation());
        if (optionalRegion.isEmpty()) {
            return;
        }

        event.setCancelled(true);
        Region region = optionalRegion.get();
        RegionPlayer player = RegionPlayer.getOrCreate(event.getPlayer());
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.getPlayer().spigot().sendMessage(Messages.regionInfo(region, player));
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            if (region.isOwner(event.getPlayer())) {
                event.setCancelled(false);
            } else if (event.getPlayer().getGameMode() == GameMode.CREATIVE && event.getPlayer().isSneaking() && event.getPlayer().hasPermission(Constants.PERMISSION_SIGN_DESTROY)) {
                event.setCancelled(false);
            } else if (event.getPlayer().hasPermission(Constants.PERMISSION_SIGN_DESTROY)) {
                event.setCancelled(false);
            }
        }
    }
}
