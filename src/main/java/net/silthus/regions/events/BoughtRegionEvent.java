package net.silthus.regions.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.silthus.regions.Cost;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionPlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class BoughtRegionEvent extends RegionEvent {

    @Getter
    private static final HandlerList handlerList = new HandlerList();

    private final RegionPlayer player;
    private Cost.Result buyResult;

    public BoughtRegionEvent(Region region, RegionPlayer player, Cost.Result buyResult) {
        super(region);
        this.player = player;
        this.buyResult = buyResult;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
