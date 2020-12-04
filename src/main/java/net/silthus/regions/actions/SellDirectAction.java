package net.silthus.regions.actions;

import net.milkbowl.vault.economy.Economy;
import net.silthus.regions.RegionsPlugin;
import net.silthus.regions.commands.RegionCommands;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionPlayer;

public class SellDirectAction extends SellAction {

    public SellDirectAction(RegionPlayer regionPlayer, Region region, double price) {
        super(region, regionPlayer, RegionCommands.SellType.DIRECT, price);
    }

    public SellDirectAction(SellServerAction sellAction) {
        super(sellAction);
    }

    @Override
    public SellResult run() {

        Economy economy = RegionsPlugin.instance().getEconomy();

        if (getPriceDetails().regionBasePrice() < getPrice()) {
            return new SellResult(this, "Der Verkaufspreis des Grundstücks darf nicht unterhalb des Grundpreises von " + economy.format(getPriceDetails().regionBasePrice()) + " liegen.");
        }

        // TODO: create extra table and sell entries

        return new SellResult(this);
    }
}
