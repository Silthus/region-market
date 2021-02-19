package net.silthus.regions.actions;

import io.ebean.annotation.Transactional;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;
import net.silthus.regions.commands.RegionCommands;
import net.silthus.regions.costs.PriceDetails;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionPlayer;

@Value
@NonFinal
@EqualsAndHashCode(callSuper = true)
public abstract class SellAction extends RegionAction {

    PriceDetails priceDetails;
    RegionCommands.SellType sellType;
    @NonFinal
    @Setter
    double price;

    public SellAction(Region region, RegionPlayer player, RegionCommands.SellType sellType, double price) {
        super(region, player);
        this.priceDetails = region.priceDetails(player);
        this.sellType = sellType;
        this.price = price;
    }

    public SellAction(Region region, RegionPlayer player, RegionCommands.SellType sellType) {
        super(region, player);
        this.priceDetails = region.priceDetails(player);
        this.sellType = sellType;
    }

    public SellAction(SellAction action) {
        super(action);
        this.priceDetails = action.priceDetails;
        this.sellType = action.sellType;
        this.price = action.price;
    }

    @Transactional
    public abstract SellResult run();
}
