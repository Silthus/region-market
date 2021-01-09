package net.silthus.regions.actions;

import de.raidcraft.economy.wrapper.Economy;
import net.silthus.regions.RegionsPlugin;
import net.silthus.regions.commands.RegionCommands;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionPlayer;
import net.silthus.regions.entities.Sale;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class SellDirectAction extends SellAction {

    public SellDirectAction(Region region, RegionPlayer regionPlayer, double price) {
        super(region, regionPlayer, RegionCommands.SellType.DIRECT, price);
    }

    public SellDirectAction(SellServerAction sellAction) {
        super(sellAction);
    }

    @Override
    public SellResult run() {


        if (getPrice() < getPriceDetails().regionBasePrice()) {
            return new SellResult(this, "Der Verkaufspreis des Grundstücks darf nicht unterhalb des Grundpreises von " + Economy.get().format(getPriceDetails().regionBasePrice()) + " liegen.");
        }

        if (Sale.getActiveSale(getRegion()).isPresent()) {
            return new SellResult(this, "Das Grundstück steht bereits zum Verkauf.");
        }

        Sale sale = new Sale(getRegion(), getRegionPlayer(), Sale.Type.DIRECT, getPrice());
        long timeout = RegionsPlugin.instance().getPluginConfig().getRegionSellTimeout();
        if (timeout > 0) {
            sale.expires(Instant.now().plus(timeout, ChronoUnit.MINUTES));
        }
        sale.start(Instant.now());
        sale.save();

        getRegion().priceType(Region.PriceType.STATIC)
                .price(getPrice())
                .status(Region.Status.FOR_DIRECT_SALE)
                .save();

        return new SellResult(this);
    }
}
