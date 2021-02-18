package net.silthus.regions;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EditSessionBuilder;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.Getter;
import net.silthus.ebean.BaseEntity;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionTransaction;
import net.silthus.regions.events.BoughtRegionEvent;
import net.silthus.regions.events.CreatedRegionEvent;
import net.silthus.regions.events.DeletedRegionEvent;
import net.silthus.regions.events.SoldRegionEvent;
import net.silthus.regions.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class SchematicManager implements Listener {

    private static final String SCHEMATIC_NAME_ORIGINAL = "original";
    private static final String SCHEMATIC_NAME_DELETED = "deleted";
    private static final String SCHEMATIC_NAME_SOLD = "sold";
    private static final String SCHEMATIC_NAME_BUY = "buy";

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
    public void onRegionBuy(BoughtRegionEvent event) {

        String dateTime = TimeUtil.formatDateTime(Instant.now(), "dd-MM-yyyy_HH-mm-ss");
        saveSchematic(event.getRegion(), SCHEMATIC_NAME_BUY + "_" + event.getRegionPlayer().name() + "_" + dateTime);
    }

    @EventHandler
    public void onSellRegion(SoldRegionEvent event) {

        String dateTime = TimeUtil.formatDateTime(Instant.now(), "dd-MM-yyyy_HH-mm-ss");
        saveSchematic(event.getRegion(), SCHEMATIC_NAME_SOLD + "_" + event.getPlayer().name() + "_" + dateTime);
    }

    private void saveSchematic(Region region, String name) {

        try {
            createClipboard(region).ifPresent(clipboard -> {
                File destination = new File(getSchematicLocation(region), name);
                saveSchematic(clipboard, destination);
                plugin.getLogger().info("saved " + name + " state of region " + region.name() + " as a schematic at: " + destination.getAbsolutePath());
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }
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
        return region.worldGuardRegion().map(protectedRegion -> {
                    try {
                        Polygonal2DRegion polygonal2DRegion = new Polygonal2DRegion(
                                bukkitWorld,
                                protectedRegion.getPoints(),
                                protectedRegion.getMinimumPoint().getY(),
                                protectedRegion.getMaximumPoint().getY());
                        BlockArrayClipboard clipboard = new BlockArrayClipboard(polygonal2DRegion);
                        clipboard.setOrigin(protectedRegion.getMinimumPoint());

                        try (EditSession editSession = WorldEdit.getInstance().newEditSession(bukkitWorld)) {
                            ForwardExtentCopy copy = new ForwardExtentCopy(editSession, polygonal2DRegion, clipboard, protectedRegion.getMinimumPoint());
                            copy.setRemovingEntities(true);
                            Operations.complete(copy);
                        }
                        return clipboard;
                    } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }
        );
    }

    private File getSchematicLocation(Region region) {

        File location = new File(new File(getSchematicBaseDir(), region.name()), region.id().toString());
        location.mkdirs();
        return location;
    }

    public void restore(Region region, String schematic) throws Exception {

        Optional<ProtectedRegion> protectedRegion = region.worldGuardRegion();
        if (protectedRegion.isPresent()) {
            try (ClipboardReader reader = BuiltInClipboardFormat.SPONGE_SCHEMATIC.getReader(new FileInputStream(new File(getSchematicLocation(region), schematic)));
                 EditSession editSession = WorldEdit.getInstance().newEditSession(new BukkitWorld(Bukkit.getWorld(region.world())))) {
                Operation operation = new ClipboardHolder(reader.read())
                        .createPaste(editSession)
                        .ignoreAirBlocks(false)
                        .copyEntities(false)
                        .to(protectedRegion.get().getMinimumPoint())
                        .build();
                Operations.complete(operation);
            } catch (IOException | WorldEditException e) {
                throw new Exception("Failed to paste WorldEdit schematic.", e);
            }
        }
    }

    public Set<String> getSchematics(@Nullable Region region) {

        if (region == null) return new HashSet<>();

        File location = getSchematicLocation(region);
        location.mkdirs();
        return Arrays.stream(location.listFiles())
                .map(File::getName)
                .collect(Collectors.toSet());
    }
}
