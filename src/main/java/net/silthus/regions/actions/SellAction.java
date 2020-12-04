package net.silthus.regions.actions;

import co.aikar.commands.InvalidCommandArgument;
import com.google.common.base.Strings;
import com.sk89q.worldguard.domains.DefaultDomain;
import io.ebean.annotation.Transactional;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;
import net.milkbowl.vault.economy.Economy;
import net.silthus.regions.RegionsPlugin;
import net.silthus.regions.commands.RegionCommands;
import net.silthus.regions.costs.PriceDetails;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionPlayer;
import net.silthus.regions.entities.RegionTransaction;
import net.silthus.regions.events.SellRegionEvent;
import net.silthus.regions.events.SoldRegionEvent;
import org.bukkit.Bukkit;

@Value
@NonFinal
@EqualsAndHashCode(callSuper = true)
public class SellAction extends RegionAction {

    PriceDetails priceDetails;
    RegionCommands.SellType sellType;
    double price;

    public SellAction(@NonNull Region region, @NonNull RegionPlayer regionPlayer, @NonNull RegionCommands.SellType sellType) {
        super(region, regionPlayer);
        this.priceDetails = region.priceDetails(regionPlayer);
        this.sellType = sellType;
        if (sellType == RegionCommands.SellType.SERVER) {
            price = priceDetails.sellServerPrice();
        } else {
            price = 0;
        }
    }

    public SellAction(@NonNull Region region, @NonNull RegionPlayer regionPlayer, @NonNull RegionCommands.SellType sellType, double price) {
        super(region, regionPlayer);
        this.price = price;
        this.priceDetails = region.priceDetails(regionPlayer);
        this.sellType = sellType;
    }

    public SellAction(@NonNull SellAction sellAction) {
        super(sellAction);
        this.priceDetails = getRegion().priceDetails(getRegionPlayer());
        this.sellType = sellAction.sellType;
        this.price = sellAction.price;
    }

    @Transactional
    public Result run() {

        Economy economy = RegionsPlugin.instance().getEconomy();

        SellRegionEvent event = new SellRegionEvent(getRegion(), getRegionPlayer(), price);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            throw new InvalidCommandArgument("Das Verkaufen des Grundstücks wurde durch ein Plugin abgebrochen.");
        }

        getRegion().worldGuardRegion().ifPresent(protectedRegion -> {
            protectedRegion.setMembers(new DefaultDomain());
            protectedRegion.setOwners(new DefaultDomain());
        });

        getRegion().owner(null)
                .status(Region.Status.FREE)
                .save();

        economy.depositPlayer(getRegionPlayer().getOfflinePlayer(), price);

        Bukkit.getPluginManager().callEvent(new SoldRegionEvent(getRegion(), getRegionPlayer()));

        RegionTransaction.of(getRegion(), getRegionPlayer(), RegionTransaction.Action.SELL)
                .data("price", price)
                .data("type", getSellType())
                .save();

        return new Result(this, price);
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class Result extends SellAction {

        String error;
        double price;

        Result(@NonNull SellAction sellAction, String error, double price) {
            super(sellAction);
            this.error = error;
            this.price = price;
        }

        Result(SellAction sellAction, double price) {
            super(sellAction);
            this.error = null;
            this.price = price;
        }

        public boolean success() {
            return Strings.isNullOrEmpty(error);
        }

        public boolean failure() {
            return !success();
        }
    }
}
