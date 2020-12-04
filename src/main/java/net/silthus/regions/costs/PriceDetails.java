package net.silthus.regions.costs;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.annotation.Nullable;

@Data
@Accessors(fluent = true)
public class PriceDetails {

    private double basePrice;
    private double regionModifier;
    private double playerRegionsModifier;
    private double groupModifier;
    private double sameGroupModifier;
    private double playerMultiplier;
    private double sellServerModifier;

    public double total() {

        return basePrice + regionModifier() + additionalPlayerCosts();
    }

    public double regionBasePrice() {

        return basePrice() + regionModifier();
    }

    public double additionalPlayerCosts() {

        return playerRegionsModifier + groupModifier + sameGroupModifier + playerMultiplier;
    }

    public double sellServerPrice() {

        return regionBasePrice() * sellServerModifier();
    }

    public PriceDetails combine(@Nullable PriceDetails other) {

        if (other == null) {
            return this;
        }

        return new PriceDetails()
                .basePrice(basePrice() + other.basePrice())
                .regionModifier(regionModifier() + other.regionModifier())
                .playerRegionsModifier(playerRegionsModifier() + other.playerRegionsModifier())
                .groupModifier(groupModifier() + other.groupModifier())
                .sameGroupModifier(sameGroupModifier() + other.sameGroupModifier())
                .playerMultiplier(playerMultiplier() + other.playerMultiplier());
    }
}
