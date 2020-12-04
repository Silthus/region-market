package net.silthus.regions.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.silthus.regions.actions.BuyAction;
import net.silthus.regions.entities.RegionPlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class BuyRegionEvent extends RegionEvent implements Cancellable {

    @Getter
    private static final HandlerList handlerList = new HandlerList();

    private final RegionPlayer player;
    private BuyAction.Result result;
    private boolean cancelled;

    public BuyRegionEvent(BuyAction.Result result) {
        super(result.getRegion());
        this.player = result.getRegionPlayer();
        this.result = result;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
