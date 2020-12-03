package net.silthus.regions.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionPlayer;
import org.bukkit.event.HandlerList;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class SoldRegionEvent extends RegionEvent {

    @Getter
    private static final HandlerList handlerList = new HandlerList();

    private final RegionPlayer player;
    private double price;

    public SoldRegionEvent(Region region, RegionPlayer player) {
        super(region);
        this.player = player;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
