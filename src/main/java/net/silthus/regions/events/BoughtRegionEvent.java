package net.silthus.regions.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import net.silthus.regions.actions.BuyAction;
import net.silthus.regions.entities.RegionPlayer;
import org.bukkit.event.HandlerList;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class BoughtRegionEvent extends RegionEvent {

    @Getter
    private static final HandlerList handlerList = new HandlerList();

    private final BuyAction.Result result;

    public BoughtRegionEvent(BuyAction.Result result) {
        super(result.getRegion());
        this.result = result;
    }

    public RegionPlayer getRegionPlayer() {

        return getResult().getRegionPlayer();
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
