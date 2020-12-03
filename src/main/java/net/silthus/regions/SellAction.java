package net.silthus.regions;

import co.aikar.commands.InvalidCommandArgument;
import com.sk89q.worldguard.domains.DefaultDomain;
import io.ebean.annotation.Transactional;
import lombok.Value;
import lombok.experimental.NonFinal;
import net.milkbowl.vault.economy.Economy;
import net.silthus.regions.commands.RegionCommands;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionPlayer;
import net.silthus.regions.entities.RegionTransaction;
import net.silthus.regions.events.SellRegionEvent;
import net.silthus.regions.events.SoldRegionEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

@Value
@NonFinal
public class SellAction {

    RegionPlayer regionPlayer;
    Region region;
    RegionCommands.SellType sellType;

    public SellAction(RegionPlayer regionPlayer, Region region, RegionCommands.SellType sellType) {
        this.regionPlayer = regionPlayer;
        this.region = region;
        this.sellType = sellType;
    }

    public SellAction(SellAction sellAction) {
        this(sellAction.regionPlayer, sellAction.region, sellAction.sellType);
    }

    @Transactional
    public SellResult run() {

        Economy economy = RegionsPlugin.instance().getEconomy();

        double price = 0;
        switch (getSellType()) {
            case SERVER:
                price = getRegion().sellServerPrice(getRegionPlayer());
                break;
        }

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

        return new SellResult(this, price);
    }
}
