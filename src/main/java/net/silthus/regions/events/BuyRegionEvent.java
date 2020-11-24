package net.silthus.regions.events;

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
public class BuyRegionEvent extends RegionEvent implements Cancellable {

    private static final HandlerList handlerList = new HandlerList();

    private final RegionPlayer player;
    private double price;
    private boolean cancelled;

    protected BuyRegionEvent(Region region, RegionPlayer player, double price) {
        super(region);
        this.player = player;
        this.price = price;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
