package net.silthus.regions.costs;

import lombok.Data;
import lombok.experimental.Accessors;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.silthus.regions.Cost;
import net.silthus.regions.CostType;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionPlayer;
import net.silthus.regions.util.Enums;
import net.silthus.regions.Messages;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

import javax.annotation.Nullable;

@Data
@Accessors(fluent = true)
@CostType("money")
public class MoneyCost implements Cost {

    private final Economy economy;

    private Type type = Type.PER2M;
    private double basePrice = 1.0;

    private double regionCountMultiplier = 0.0;
    private double regionCountMultiplierPower = 1.0;

    private double regionGroupCountMultiplier = 0.0;
    private double regionGroupCountMultiplierPower = 1.0;

    private double sameGroupCountMultiplier = 0.0;
    private double sameGroupCountMultiplierPower = 1.0;

    public MoneyCost(Economy economy) {
        this.economy = economy;
    }

    @Override
    public void load(ConfigurationSection config) {

        this.type = Enums.searchEnum(Type.class, config.getString("type", type.name()));
        this.basePrice = config.getDouble("base", basePrice);

        this.regionCountMultiplier = config.getDouble("region-count-multiplier", regionCountMultiplier);
        this.regionCountMultiplierPower = config.getDouble("region-count-multiplier-power", regionCountMultiplierPower);

        this.regionGroupCountMultiplier = config.getDouble("region-group-count-multiplier", regionGroupCountMultiplier);
        this.regionGroupCountMultiplierPower = config.getDouble("region-group-count-multiplier-power", regionGroupCountMultiplierPower);

        this.sameGroupCountMultiplier = config.getDouble("same-group-count-multiplier", sameGroupCountMultiplier);
        this.sameGroupCountMultiplierPower = config.getDouble("same-group-count-multiplier-power", sameGroupCountMultiplierPower);
    }

    @Override
    public String display(RegionPlayer player, Region region) {

        return economy.format(calculate(player, region));
    }

    @Override
    public Result check(RegionPlayer player, Region region) {

        OfflinePlayer offlinePlayer = player.getOfflinePlayer();

        double cost = calculate(player, region);
        if (economy().has(offlinePlayer, cost)) {
            return new Result(true, null, cost);
        } else {
            return new Result(false, "Nicht genug Geld. Kosten: " + economy().format(cost));
        }
    }

    @Override
    public Result apply(RegionPlayer player, Region region) {

        EconomyResponse economyResponse = economy().withdrawPlayer(player.getOfflinePlayer(), calculate(player, region));
        return new Result(economyResponse.transactionSuccess(), economyResponse.errorMessage);
    }

    public double calculate(Region region) {

        return calculate(null, region);
    }

    public double calculate(@Nullable RegionPlayer player, Region region) {

        double price;
        switch (region.priceType()) {
            case FREE:
                return 0;
            case DYNAMIC:
                switch (type()) {
                    case PER2M:
                        price = basePrice() * region.size();
                        break;
                    case PER3M:
                        price = basePrice() * region.volume();
                        break;
                    case STATIC:
                    default:
                        price = basePrice();
                }
                price += region.price();
                break;
            default:
            case STATIC:
                price = region.price();
                break;
        }

        if (player != null) {
            double basePrice = price;

            price += calculateMultiplier(player.regions().size(), regionCountMultiplierPower(), regionCountMultiplier(), basePrice);
            price += calculateMultiplier(player.regionGroups().size(), regionGroupCountMultiplierPower(), regionGroupCountMultiplier(), basePrice);
            price += calculateMultiplier(player.regions(region.group()).size(), sameGroupCountMultiplierPower(), sameGroupCountMultiplier(), basePrice);

            price = price * player.priceMultiplier();
        }

        return price * region.priceMultiplier();
    }

    private double calculateMultiplier(int count, double power, double multiplier, double basePrice) {

        return (basePrice * ((Math.pow(count, power) * multiplier) + 1.0)) - basePrice;
    }

    public enum Type {
        STATIC,
        PER2M,
        PER3M
    }
}
