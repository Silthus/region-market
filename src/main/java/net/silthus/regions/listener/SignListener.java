package net.silthus.regions.listener;

import com.google.common.base.Strings;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.ebean.Model;
import net.silthus.regions.RegionSignParseException;
import net.silthus.regions.RegionsPlugin;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionGroup;
import net.silthus.regions.entities.RegionPlayer;
import net.silthus.regions.entities.RegionSign;
import net.silthus.regions.Messages;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;

import java.util.Optional;

import static net.silthus.regions.Constants.PERMISSION_SIGN_CREATE;
import static net.silthus.regions.Constants.SIGN_TAG;

public class SignListener implements Listener {

    private final RegionsPlugin plugin;

    public SignListener(RegionsPlugin plugin) {
        this.plugin = plugin;
    }

    private RegionManager getRegionManager(World world) {
        return WorldGuard.getInstance().getPlatform().getRegionContainer().get(new BukkitWorld(world));
    }

    @EventHandler(ignoreCancelled = true)
    public void onRegionInfo(SignSendEvent event) {

        String line1 = event.getLine(0);
        if (Strings.isNullOrEmpty(line1) || !line1.equalsIgnoreCase(SIGN_TAG)) {
            return;
        }

        Optional<Region> optionalRegion = Region.of(event.getLocation());
        if (optionalRegion.isEmpty()) {
            event.setCancelled(true);
            return;
        }

        Region region = optionalRegion.get();
        RegionPlayer player = RegionPlayer.of(event.getPlayer());

        if (region.status() != Region.Status.OCCUPIED) {
            event.setLine(0, ChatColor.GREEN + "[Grundstück]");
            event.setLine(1, ChatColor.WHITE + region.name());
            event.setLine(3, ChatColor.GREEN + "- Verfügbar -");
            event.setLine(4, ChatColor.GREEN + "Kosten: " + ChatColor.YELLOW + region.costs(player));
        } else {
            event.setLine(0, ChatColor.RED + "[Grundstück]");
            event.setLine(1, ChatColor.WHITE + region.name());
            event.setLine(3, ChatColor.RED + "- Besitzer -");
            event.setLine(4, ChatColor.YELLOW + region.owner().name());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreateRegion(SignChangeEvent event) {

        String line1 = event.getLine(0);
        if (Strings.isNullOrEmpty(line1) || !line1.equalsIgnoreCase(SIGN_TAG)) {
            return;
        }

        Player player = event.getPlayer();
        Region region;
        try {
            region = tryGetOrCreateRegion(event.getPlayer(), event.getBlock(), event.getLines());
        } catch (RegionSignParseException e) {
            event.getPlayer().sendMessage(ChatColor.RED + e.getMessage());
            event.setCancelled(true);
            event.getBlock().setType(Material.AIR);
            return;
        }

        String groupLine = event.getLine(2);

        if (!Strings.isNullOrEmpty(groupLine)) {
            Optional<RegionGroup> regionGroup = plugin.getRegionManager().getRegionGroup(groupLine);
            if (regionGroup.isEmpty()) {
                player.sendMessage(String.format(ChatColor.RED + "Die Grundstücksgruppe %s in Zeile 3 existiert nicht.", groupLine));
                event.setCancelled(true);
                event.getBlock().setType(Material.AIR);
                return;
            }
        }

        String costLine = event.getLine(3);
        if (Strings.isNullOrEmpty(costLine) || costLine.startsWith("auto") || costLine.startsWith("dynamic")) {
            region.priceType(Region.PriceType.DYNAMIC);
        } else if (costLine.equalsIgnoreCase("free")) {
            region.priceType(Region.PriceType.FREE);
            region.price(0);
        } else {
            try {
                region.price(Double.parseDouble(costLine));
                region.priceType(Region.PriceType.STATIC);
            } catch (NumberFormatException e) {
                player.sendMessage(String.format(ChatColor.RED + "Der Grundstückspreis in Zeile 4 %s ist keine gültige Zahl.", costLine));
                event.setCancelled(true);
                event.getBlock().setType(Material.AIR);
                return;
            }
        }

        RegionSign regionSign = new RegionSign(region, event.getBlock());
        regionSign.save();
        region.signs().add(regionSign);

        region.save();
        player.sendMessage(String.format(ChatColor.GREEN + "Das Grundstück $1%s wurde erfolgreich erstellt. Preis: $2%s ($3%s)", region.name(), region.price(), region.priceType()));
    }

    private Region tryGetOrCreateRegion(Player player, Block sign, String[] lines) throws RegionSignParseException {

        if (!player.hasPermission(PERMISSION_SIGN_CREATE)) {
            throw new RegionSignParseException("Du hast nicht genügend Rechte um das Grundstück zu erstellen.");
        }

        String regionName = lines[1];

        if (Strings.isNullOrEmpty(regionName)) {
            throw new RegionSignParseException("Du musst eine WorldGuard Region in Zeile 2 angeben.");
        }

        RegionManager regionManager = getRegionManager(player.getWorld());
        if (regionManager == null) {
            throw new RegionSignParseException("Der WorldGuard RegionManager konnte für diese Welt nicht gefunden werden.");
        }

        ProtectedRegion protectedRegion = regionManager.getRegion(regionName);
        if (protectedRegion == null) {
            throw new RegionSignParseException(String.format("Die WorldGuard Region '%s' in Zeile 2 existiert nicht.", regionName));
        }

        return Region.of(sign.getWorld(), protectedRegion).orElse(new Region(sign.getWorld(), regionName));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {

        if (!(event.getBlock().getState() instanceof Sign)) {
            return;
        }
        Optional<RegionSign> regionSign = RegionSign.of(event.getBlock().getLocation());
        regionSign.ifPresent(Model::delete);
    }
}
