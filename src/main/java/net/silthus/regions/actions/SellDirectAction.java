package net.silthus.regions.actions;

import io.ebean.annotation.Transactional;
import net.milkbowl.vault.economy.Economy;
import net.silthus.regions.RegionsPlugin;
import net.silthus.regions.commands.RegionCommands;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionPlayer;
import net.silthus.regions.entities.Sale;

public class SellDirectAction extends SellAction {

    public SellDirectAction(Region region, RegionPlayer regionPlayer, double price) {
        super(region, regionPlayer, RegionCommands.SellType.DIRECT, price);
    }

    public SellDirectAction(SellServerAction sellAction) {
        super(sellAction);
    }

    @Override
    @Transactional
    public SellResult run() {

        Economy economy = RegionsPlugin.instance().getEconomy();

        if (getPriceDetails().regionBasePrice() < getPrice()) {
            return new SellResult(this, "Der Verkaufspreis des Grundstücks darf nicht unterhalb des Grundpreises von " + economy.format(getPriceDetails().regionBasePrice()) + " liegen.");
        }

        if (Sale.of(getRegion()).isPresent()) {
            return new SellResult(this, "Das Grundstück steht bereits zum Verkauf.");
        }

        new Sale(getRegion(), getRegionPlayer(), Sale.Type.DIRECT, getPrice()).save();

        getRegion().priceType(Region.PriceType.STATIC)
                .price(getPrice())
                .priceMultiplier(1.0)
                .save();

        return new SellResult(this);
    }
}
