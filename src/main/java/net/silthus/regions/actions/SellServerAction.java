package net.silthus.regions.actions;

import co.aikar.commands.InvalidCommandArgument;
import com.sk89q.worldguard.domains.DefaultDomain;
import io.ebean.annotation.Transactional;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;
import net.milkbowl.vault.economy.Economy;
import net.silthus.regions.RegionsPlugin;
import net.silthus.regions.commands.RegionCommands;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionPlayer;
import net.silthus.regions.entities.RegionTransaction;
import net.silthus.regions.events.SellRegionEvent;
import net.silthus.regions.events.SoldRegionEvent;
import org.bukkit.Bukkit;

@Value
@NonFinal
@EqualsAndHashCode(callSuper = true)
public class SellServerAction extends SellAction {

    public SellServerAction(@NonNull Region region, @NonNull RegionPlayer regionPlayer) {
        super(region, regionPlayer, RegionCommands.SellType.SERVER);
        setPrice(getPriceDetails().sellServerPrice());
    }

    public SellServerAction(@NonNull SellServerAction sellAction) {
        super(sellAction);
    }

    @Override
    @Transactional
    public SellResult run() {

        Economy economy = RegionsPlugin.instance().getEconomy();

        SellRegionEvent event = new SellRegionEvent(this);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            return new SellResult(this, "Der Kauf des Grundstücks wurde durch ein Plugin verhindert.");
        }

        getRegion().worldGuardRegion().ifPresent(protectedRegion -> {
            protectedRegion.setMembers(new DefaultDomain());
            protectedRegion.setOwners(new DefaultDomain());
        });

        getRegion().owner(null)
                .status(Region.Status.FREE)
                .save();

        economy.depositPlayer(getRegionPlayer().getOfflinePlayer(), getPrice());

        Bukkit.getPluginManager().callEvent(new SoldRegionEvent(getRegion(), getRegionPlayer()));

        RegionTransaction.of(getRegion(), getRegionPlayer(), RegionTransaction.Action.SELL)
                .data("price", getPrice())
                .data("type", getSellType())
                .save();

        return new SellResult(this);
    }

}
