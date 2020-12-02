package net.silthus.regions.events;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionPlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CreateRegionEvent extends RegionEvent implements Cancellable {

    private static final HandlerList handlerList = new HandlerList();

    private final ProtectedRegion worldGuardRegion;
    private boolean cancelled;

    public CreateRegionEvent(Region region, ProtectedRegion worldGuardRegion) {
        super(region);
        this.worldGuardRegion = worldGuardRegion;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
