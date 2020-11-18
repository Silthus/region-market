package net.silthus.regions.costs;

import net.milkbowl.vault.economy.Economy;
import net.silthus.regions.Cost;
import net.silthus.regions.CostType;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionPlayer;
import net.silthus.regions.util.Enums;
import net.silthus.regions.util.Messages;
import org.apache.commons.lang.NotImplementedException;
import org.bukkit.configuration.ConfigurationSection;

@CostType("money")
public class MoneyCost implements Cost {

    private final Economy economy;

    private Type type;
    private double basePrice;
    private double regionMultiplier;
    private double regionGroupMultiplier;

    public MoneyCost(Economy economy) {
        this.economy = economy;
    }

    @Override
    public void load(ConfigurationSection config) {

        this.type = Enums.searchEnum(Type.class, config.getString("type", Type.PER2M.name()));
        this.basePrice = config.getDouble("base", 10.0);
        this.regionMultiplier = config.getDouble("region-multiplier", 1.0);
        this.regionGroupMultiplier = config.getDouble("region-group-multiplier", 1.0);
    }

    @Override
    public String display(RegionPlayer player, Region region) {

        return String.format(Messages.msg("regions.cost.money.price", "%s"),
                economy.format(calculate(player, region)));
    }

    @Override
    public Result check(RegionPlayer player, Region region) {
        return null;
    }

    @Override
    public Result apply(RegionPlayer player, Region region) {
        return null;
    }

    private double calculate(RegionPlayer player, Region region) {

        throw new NotImplementedException();
    }

    public enum Type {
        STATIC,
        PER2M,
        PER3M
    }
}
