package net.silthus.regions.actions;

import lombok.Value;
import lombok.experimental.NonFinal;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionPlayer;

@Value
@NonFinal
public abstract class RegionAction {

    Region region;
    RegionPlayer regionPlayer;

    protected RegionAction(Region region, RegionPlayer regionPlayer) {
        this.region = region;
        this.regionPlayer = regionPlayer;
    }

    protected RegionAction(RegionAction action) {
        this(action.getRegion(), action.getRegionPlayer());
    }
}
