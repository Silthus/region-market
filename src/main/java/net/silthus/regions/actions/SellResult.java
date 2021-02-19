package net.silthus.regions.actions;

import com.google.common.base.Strings;
import lombok.NonNull;
import lombok.Value;
import net.silthus.regions.costs.PriceDetails;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionPlayer;

@Value
public class SellResult {

    SellAction action;
    String error;

    SellResult(@NonNull SellAction sellAction, String error) {
        this.action = sellAction;
        this.error = error;
    }

    SellResult(SellAction sellAction) {
        this(sellAction, null);
    }

    public Region getRegion() {
        return getAction().getRegion();
    }

    public RegionPlayer getRegionPlayer() {
        return getAction().getRegionPlayer();
    }

    public PriceDetails getPriceDetails() {
        return getAction().getPriceDetails();
    }

    public double getPrice() {

        return getAction().getPrice();
    }

    public boolean success() {
        return Strings.isNullOrEmpty(error);
    }

    public boolean failure() {
        return !success();
    }
}
