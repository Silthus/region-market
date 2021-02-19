package net.silthus.regions.events;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.silthus.regions.entities.Region;
import org.bukkit.event.HandlerList;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CreatedRegionEvent extends RegionEvent {

    @Getter
    private static final HandlerList handlerList = new HandlerList();

    private final ProtectedRegion worldGuardRegion;
    private boolean cancelled;

    public CreatedRegionEvent(Region region, ProtectedRegion worldGuardRegion) {
        super(region);
        this.worldGuardRegion = worldGuardRegion;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
