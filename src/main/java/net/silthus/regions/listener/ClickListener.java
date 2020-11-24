package net.silthus.regions.listener;

import net.silthus.regions.Constants;
import net.silthus.regions.Cost;
import net.silthus.regions.RegionsPlugin;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionPlayer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Optional;

public class ClickListener implements Listener {

    private final RegionsPlugin plugin;

    public ClickListener(RegionsPlugin plugin) {
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

        Optional<Region> optionalRegion = Region.of(event.getClickedBlock().getLocation());
        if (optionalRegion.isEmpty()) {
            return;
        }

        event.setCancelled(true);
        Region region = optionalRegion.get();
        RegionPlayer player = RegionPlayer.getOrCreate(event.getPlayer());
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getPlayer().isSneaking()) {
                if (region.status() != Region.Status.OCCUPIED) {
                    event.getPlayer().sendMessage(ChatColor.RED + "Diese Region kann nicht verkauft werden.");
                    return;
                }
                if (!player.equals(region.owner())) {
                    event.getPlayer().sendMessage(ChatColor.RED + "Nur der Besitzer " + region.owner().name() + " kann die Region verkaufen.");
                    return;
                }
                event.getPlayer().sendMessage(ChatColor.YELLOW + "Regionen können aktuell noch nicht verkauft werden.");
                // TODO: implement
            } else {
                if (!event.getPlayer().hasPermission(Constants.PERMISSION_SIGN_BUY)) {
                    event.getPlayer().sendMessage(ChatColor.RED + "Du hast nicht genügend Rechte um Grundstücke zu kaufen.");
                    return;
                }
                if (region.status() == Region.Status.OCCUPIED) {
                    event.getPlayer().sendMessage(ChatColor.RED + "Dieses Grundstück gehört bereits: " + region.owner().name());
                    return;
                }

                Cost.Result canBuy = region.canBuy(player);
                if (canBuy.failure()) {
                    event.getPlayer().sendMessage(ChatColor.RED + "Du kannst diese Region nicht kaufen: " + canBuy.error());
                    return;
                }

                event.getPlayer().sendMessage(ChatColor.YELLOW + "Bestätige deinen Grundstückskauf für " + ChatColor.AQUA
                        + plugin.getEconomy().format(canBuy.price()) + ChatColor.YELLOW + " mit /sregions buyconfirm " + region.name());
            }
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK && event.getPlayer().isSneaking() && event.getPlayer().hasPermission(Constants.PERMISSION_SIGN_DESTROY)) {
            event.setCancelled(false);
        }
    }
}
