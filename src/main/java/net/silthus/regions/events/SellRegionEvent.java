package net.silthus.regions.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.silthus.regions.actions.SellAction;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionPlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class SellRegionEvent extends RegionEvent implements Cancellable {

    @Getter
    private static final HandlerList handlerList = new HandlerList();

    private final SellAction action;
    private boolean cancelled;

    public SellRegionEvent(SellAction action) {
        super(action.getRegion());
        this.action = action;
    }

    public RegionPlayer getPlayer() {

        return getAction().getRegionPlayer();
    }

    public double getPrice() {

        return getAction().getPrice();
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
