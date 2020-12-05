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
public class BuyRegionFromPlayerEvent extends BuyRegionEvent implements Cancellable {

    @Getter
    private static final HandlerList handlerList = new HandlerList();

    private final RegionPlayer seller;
    private double sellPrice;
    private boolean cancelled;

    public BuyRegionFromPlayerEvent(BuyAction.Result result, RegionPlayer seller, double sellPrice) {
        super(result);
        this.seller = seller;
        this.sellPrice = sellPrice;
    }

    /**
     * @return the player that is buying the region.
     */
    public RegionPlayer getBuyer() {

        return getResult().getRegionPlayer();
    }

    /**
     * Gets the price the buyer needs to pay for the region.
     * This is the base price set by the seller plus any multipliers for owning multiple regions.
     *
     * @return the total buy price of the region
     */
    public double getBuyPrice() {

        return getPrice();
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }
}
