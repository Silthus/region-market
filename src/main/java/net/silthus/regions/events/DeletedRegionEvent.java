package net.silthus.regions.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.silthus.regions.entities.Region;
import org.bukkit.event.HandlerList;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class DeletedRegionEvent extends RegionEvent {

    @Getter
    private static final HandlerList handlerList = new HandlerList();

    public DeletedRegionEvent(Region region) {
        super(region);
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
