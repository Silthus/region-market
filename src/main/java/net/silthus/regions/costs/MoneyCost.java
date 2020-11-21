package net.silthus.regions.costs;

import lombok.Data;
import lombok.experimental.Accessors;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.silthus.regions.Cost;
import net.silthus.regions.CostType;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionGroup;
import net.silthus.regions.entities.RegionPlayer;
import net.silthus.regions.util.Enums;
import net.silthus.regions.Messages;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

@Data
@Accessors(fluent = true)
@CostType("money")
public class MoneyCost implements Cost {

    private final Economy economy;

    private Type type;
    private double basePrice;
    private double regionCountMultiplier;
    private double regionGroupCountMultiplier;
    private double sameGroupCountMultiplier;

    public MoneyCost(Economy economy) {
        this.economy = economy;
    }

    @Override
    public void load(ConfigurationSection config) {

        this.type = Enums.searchEnum(Type.class, config.getString("type", Type.PER2M.name()));
        this.basePrice = config.getDouble("base", 10.0);
        this.regionCountMultiplier = config.getDouble("region-count-multiplier", 0.0);
        this.regionGroupCountMultiplier = config.getDouble("region-group-count-multiplier", 0.0);
        this.sameGroupCountMultiplier = config.getDouble("same-group-count-multiplier", 0.0);
    }

    @Override
    public String display(RegionPlayer player, Region region) {

        try {
            return Messages.msg("costs.money.price", "%s", calculate(player, region));
        } catch (CostCalucationException e) {
            return Messages.msg("costs.exception", "Es ist ein Fehler aufgetreten: %s", e.getMessage());
        }
    }

    @Override
    public Result check(RegionPlayer player, Region region) {

        OfflinePlayer offlinePlayer = player.getOfflinePlayer();

        try {
            double cost = calculate(player, region);
            if (economy().has(offlinePlayer, cost)) {
                return new Result(true, null);
            } else {
                return new Result(false, Messages.msg("costs.money.not-enough-money", "Nicht genug Geld. Kosten: %s", economy().format(cost)));
            }
        } catch (CostCalucationException e) {
            return new Result(false, e.getMessage());
        }
    }

    @Override
    public Result apply(RegionPlayer player, Region region) {

        try {
            EconomyResponse economyResponse = economy().withdrawPlayer(player.getOfflinePlayer(), calculate(player, region));
            return new Result(economyResponse.transactionSuccess(), economyResponse.errorMessage);
        } catch (CostCalucationException e) {
            return new Result(false, e.getMessage());
        }
    }

    public double calculate(RegionPlayer player, Region region) throws CostCalucationException {

        double price;
        switch (region.priceType()) {
            case FREE:
                return 0;
            case DYNAMIC:
                price = calculateDynamicPrice(player, region);
                switch (type()) {
                    case PER2M:
                        price = price * region.size();
                        break;
                    case PER3M:
                        price = price * region.volume();
                        break;
                }
                break;
            default:
            case STATIC:
                price = region.price();
                break;
        }

        return price * region.priceMultiplier() * player.priceMultiplier();
    }

    private double calculateDynamicPrice(RegionPlayer player, Region region) throws CostCalucationException {

        RegionGroup group = region.group();
        if (group == null) {
            throw new CostCalucationException("Cannot calculate price of region '" + region.name() + " (" + region.id() + ")' without a region group.");
        }

        int regionCount = player.regions().size();
        int groupCount = player.regionGroups().size();
        int sameGroupCount = player.regions(region.group()).size();

        return region.price() + basePrice
                + (basePrice * regionCount * regionCountMultiplier())
                + (basePrice * groupCount * regionGroupCountMultiplier())
                + (basePrice * sameGroupCount * sameGroupCountMultiplier());
    }

    public enum Type {
        STATIC,
        PER2M,
        PER3M
    }
}
