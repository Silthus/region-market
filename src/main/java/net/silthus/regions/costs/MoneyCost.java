package net.silthus.regions.costs;

import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.silthus.regions.Cost;
import net.silthus.regions.CostType;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionPlayer;
import net.silthus.regions.util.Enums;
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
    public String display(Region region, RegionPlayer player) {

        return economy.format(calculate(region, player));
    }

    @Override
    public BaseComponent[] details(@NonNull Region region, @Nullable RegionPlayer player) {

        ComponentBuilder builder = new ComponentBuilder();

        if (region.priceType() == Region.PriceType.FREE) {
            return builder.append("Das Grundstück ist kostenlos.")
                    .color(ChatColor.GRAY).italic(true).create();
        }

        double totalCosts = calculate(region, player);

        ChatColor color = ChatColor.AQUA;
        if (player != null) {
            color = economy.has(player.getOfflinePlayer(), totalCosts) ? ChatColor.GREEN : ChatColor.DARK_RED;
        }

        double basePrice = calculateBasePrice(region);
        builder.append("o ").append(economy.format(basePrice)).color(ChatColor.AQUA);
        switch (region.priceType()) {
            case STATIC:
                builder.append(" (Fix-Preis)");
                break;
            case DYNAMIC:
                switch (type()) {
                    case STATIC:
                        builder.append(" (Fix-Preis)");
                        break;
                    case PER2M:
                        builder.append(" (").append(economy.format(basePrice())).append(" pro m²)");
                        break;
                    case PER3M:
                        builder.append(" (").append(economy.format(basePrice())).append(" pro m³)");
                        break;
                }
        }
        builder.reset().color(ChatColor.GRAY).italic(true).append("\n").reset();

        double adjustedBasePrice = basePrice * region.priceMultiplier();
        double basePriceDiff = adjustedBasePrice - basePrice;
        if (basePriceDiff < 0.0) {
            builder.append(" -").append(economy.format(Math.abs(basePriceDiff))).color(ChatColor.GREEN);
        } else if (basePriceDiff > 0.0) {
            builder.append(" +").append(economy.format(basePriceDiff)).color(ChatColor.RED);
        }
        if (basePriceDiff != 0.0) {
            builder.append(" (x" + region.priceMultiplier()).append(" Grundstücksfaktor)")
                    .color(ChatColor.GRAY).italic(true).append("\n").reset();
        }

        if (player != null) {
            double regionMultiplier = calculatePlayerRegionMultiplier(player, adjustedBasePrice);
        }

        builder.append("Gesamt: ").color(ChatColor.YELLOW)
                .append(economy().format(totalCosts)).color(color).append("\n");


        return builder.create();
    }

    @Override
    public Result check(@NonNull Region region, @Nullable RegionPlayer player) {

        if (player == null) {
            return new Result(false, "Player is NULL", ResultStatus.OTHER);
        }
        OfflinePlayer offlinePlayer = player.getOfflinePlayer();

        double cost = calculate(region, player);
        if (economy().has(offlinePlayer, cost)) {
            return new Result(true, null, cost);
        } else {
            return new Result(false, "Nicht genug Geld. Kosten: " + economy().format(cost));
        }
    }

    @Override
    public Result apply(Region region, RegionPlayer player) {

        EconomyResponse economyResponse = economy().withdrawPlayer(player.getOfflinePlayer(), calculate(region, player));
        return new Result(economyResponse.transactionSuccess(), economyResponse.errorMessage);
    }

    public double calculate(Region region) {

        return calculate(region, null);
    }

    public double calculate(Region region, @Nullable RegionPlayer player) {

        double price = calculateBasePrice(region) * region.priceMultiplier();

        if (player != null) {
            double basePrice = price;

            price += calculatePlayerRegionMultiplier(player, basePrice);
            price += calculateMultiplier(player.regionGroups().size(), regionGroupCountMultiplierPower(), regionGroupCountMultiplier(), basePrice);
            price += calculateMultiplier(player.regions(region.group()).size(), sameGroupCountMultiplierPower(), sameGroupCountMultiplier(), basePrice);

            price = price * player.priceMultiplier();
        }

        return price;
    }

    private double calculateBasePrice(Region region) {

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

        return price;
    }

    private double calculatePlayerRegionMultiplier(RegionPlayer player, double basePrice) {

        return calculateMultiplier(player.regions().size(), regionCountMultiplierPower(), regionCountMultiplier(), basePrice);
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
