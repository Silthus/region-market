package net.silthus.regions;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import lombok.Getter;
import net.silthus.ebean.BaseEntity;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionTransaction;
import net.silthus.regions.events.CreatedRegionEvent;
import net.silthus.regions.events.DeletedRegionEvent;
import net.silthus.regions.events.SoldRegionEvent;
import net.silthus.regions.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

@Getter
public class SchematicManager implements Listener {

    private static final String SCHEMATIC_NAME_ORIGINAL = "original";
    private static final String SCHEMATIC_NAME_DELETED = "deleted";
    private static final String SCHEMATIC_NAME_SOLD = "sold";

    private final RegionsPlugin plugin;
    private final WorldEdit worldEdit;
    private final File schematicBaseDir;

    public SchematicManager(RegionsPlugin plugin, WorldEdit worldEdit) {
        this.plugin = plugin;
        this.worldEdit = worldEdit;
        schematicBaseDir = new File(plugin.getDataFolder(), plugin.getPluginConfig().getSchematics());
        schematicBaseDir.mkdirs();
    }

    @EventHandler
    public void onRegionCreated(CreatedRegionEvent event) {

        saveSchematic(event.getRegion(), SCHEMATIC_NAME_ORIGINAL);
    }

    @EventHandler
    public void onRegionDeleted(DeletedRegionEvent event) {

        saveSchematic(event.getRegion(), SCHEMATIC_NAME_DELETED);
    }

    @EventHandler
    public void onSellRegion(SoldRegionEvent event) {

        String dateTime = TimeUtil.formatDateTime(Instant.now(), "dd-MM-yyyy_HH-mm-ss");
        saveSchematic(event.getRegion(), SCHEMATIC_NAME_SOLD + "_" + event.getPlayer().name() + "_" + dateTime);
    }

    private void saveSchematic(Region region, String name) {

        createClipboard(region).ifPresent(clipboard -> {
            File destination = new File(getSchematicLocation(region), name);
            saveSchematic(clipboard, destination);
            plugin.getLogger().info("saved " + name + " state of region " + region.name() + " as a schematic at: " + destination.getAbsolutePath());
        });
    }

    private void saveSchematic(Clipboard clipboard, File destination) {
        try (ClipboardWriter writer = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getWriter(new FileOutputStream(destination))) {
            writer.write(clipboard);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Optional<Clipboard> createClipboard(Region region) {

        World world = Bukkit.getWorld(region.world());
        if (world == null) {
            return Optional.empty();
        }

        BukkitWorld bukkitWorld = new BukkitWorld(world);
        return region.worldGuardRegion().map(protectedRegion -> new BlockArrayClipboard(new Polygonal2DRegion(
                bukkitWorld,
                protectedRegion.getPoints(),
                protectedRegion.getMinimumPoint().getY(),
                protectedRegion.getMaximumPoint().getY()))
        );
    }

    private File getSchematicLocation(Region region) {

        File location = new File(new File(getSchematicBaseDir(), region.name()), region.id().toString());
        location.mkdirs();
        return location;
    }
}
