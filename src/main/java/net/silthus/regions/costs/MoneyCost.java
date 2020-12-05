package net.silthus.regions.costs;

import lombok.Data;
import lombok.NonNull;
import lombok.experimental.Accessors;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
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
        if (type == null) type = Type.PER2M;
        this.basePrice = config.getDouble("base", basePrice);

        this.regionCountMultiplier = config.getDouble("region-count-multiplier", regionCountMultiplier);
        this.regionCountMultiplierPower = config.getDouble("region-count-multiplier-power", regionCountMultiplierPower);

        this.regionGroupCountMultiplier = config.getDouble("region-group-count-multiplier", regionGroupCountMultiplier);
        this.regionGroupCountMultiplierPower = config.getDouble("region-group-count-multiplier-power", regionGroupCountMultiplierPower);

        this.sameGroupCountMultiplier = config.getDouble("same-group-count-multiplier", sameGroupCountMultiplier);
        this.sameGroupCountMultiplierPower = config.getDouble("same-group-count-multiplier-power", sameGroupCountMultiplierPower);
    }

    @Override
    public BaseComponent[] display(@NonNull Region region, @Nullable RegionPlayer player) {

        if (region.priceType() == Region.PriceType.FREE) {
            return new ComponentBuilder().append("Kostenlos").color(ChatColor.GREEN)
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(new ComponentBuilder()
                            .append("Das Grundstück ist kostenlos.")
                            .color(ChatColor.GRAY).italic(true).create()
                    ))).create();
        }

        PriceDetails cost = calculate(region, player);

        return new ComponentBuilder().append(economy().format(cost.total())).color(costColor(player, cost.total()))
                .event(createCostHover(region, player, cost)).create();
    }

    private HoverEvent createCostHover(@NonNull Region region, @Nullable RegionPlayer player, PriceDetails cost) {

        ComponentBuilder builder = new ComponentBuilder();
        double basePrice = cost.basePrice();
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

        if (cost.regionModifier() < 0.0) {
            builder.append(" -").append(economy.format(Math.abs(cost.regionModifier()))).color(ChatColor.GREEN);
        } else if (cost.regionModifier() > 0.0) {
            builder.append(" +").append(economy.format(cost.regionModifier())).color(ChatColor.RED);
        }
        if (cost.regionModifier() != 0.0) {
            builder.append(" (x" + region.priceMultiplier()).append(" Grundstücksfaktor)")
                    .color(ChatColor.GRAY).italic(true).append("\n").reset();
        }

        if (player != null) {
            if (cost.playerRegionsModifier() > 0) {
                builder.append(" +").append(economy().format(cost.playerRegionsModifier())).color(ChatColor.RED);
            } else if (cost.playerRegionsModifier() < 0) {
                builder.append(" -").append(economy().format(cost.playerRegionsModifier())).color(ChatColor.GREEN);
            }
            if (cost.playerRegionsModifier() != 0.0) {
                builder.append(" (x" + player.regions().size()).append(" Grundstücke)")
                        .color(ChatColor.GRAY).italic(true).append("\n").reset();
            }

            if (cost.groupModifier() > 0) {
                builder.append(" +").append(economy().format(cost.groupModifier())).color(ChatColor.RED);
            } else if (cost.groupModifier() < 0) {
                builder.append(" -").append(economy().format(cost.groupModifier())).color(ChatColor.GREEN);
            }
            if (cost.groupModifier() != 0.0) {
                builder.append(" (x" + player.regionGroups().size()).append(" Stadtteile)")
                        .color(ChatColor.GRAY).italic(true).append("\n").reset();
            }

            if (cost.sameGroupModifier() > 0) {
                builder.append(" +").append(economy().format(cost.sameGroupModifier())).color(ChatColor.RED);
            } else if (cost.sameGroupModifier() < 0) {
                builder.append(" -").append(economy().format(cost.sameGroupModifier())).color(ChatColor.GREEN);
            }
            if (cost.sameGroupModifier() != 0.0) {
                builder.append(" (x" + player.regions(region.group()).size()).append(" ")
                        .append(region.group().name()).append(" Grundstücke)")
                        .color(ChatColor.GRAY).italic(true).append("\n").reset();
            }
        }

        builder.append("--------------------").color(ChatColor.DARK_GRAY).append("\n")
                .append("Summe: ").color(ChatColor.YELLOW)
                .append(economy().format(cost.total())).color(costColor(player, cost.total())).append("\n");

        return new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(builder.create()));
    }

    @Override
    public Result check(@NonNull Region region, @Nullable RegionPlayer player) {

        if (player == null) {
            return new Result(false, "Player is NULL", ResultStatus.OTHER);
        }
        OfflinePlayer offlinePlayer = player.getOfflinePlayer();

        PriceDetails cost = calculate(region, player);
        if (economy().has(offlinePlayer, cost.total())) {
            return new Result(true, null, cost);
        } else {
            return new Result(false, "Nicht genug Geld. Kosten: " + economy().format(cost.total()));
        }
    }

    @Override
    public Result apply(Region region, RegionPlayer player) {

        PriceDetails price = calculate(region, player);
        EconomyResponse economyResponse = economy().withdrawPlayer(player.getOfflinePlayer(), price.total());
        return new Result(economyResponse.transactionSuccess(), economyResponse.errorMessage, price, ResultStatus.SUCCESS);
    }

    public PriceDetails calculate(Region region) {

        return calculate(region, null);
    }

    public PriceDetails calculate(Region region, @Nullable RegionPlayer player) {

        PriceDetails price = new PriceDetails()
                .basePrice(calculateBasePrice(region));
        if (region.priceType() != Region.PriceType.STATIC) {
            price.regionModifier((price.basePrice() * region.priceMultiplier()) - price.basePrice());
        }
        price.sellServerModifier(region.group().sellModifier());

        if (player != null) {
            double basePrice = price.regionBasePrice();

            price.playerRegionsModifier(calculatePlayerRegionMultiplier(player, basePrice));
            price.groupModifier(calculatePlayerRegionGroupMultiplier(player, basePrice));
            price.sameGroupModifier(calculatePlayerSameGroupMultiplier(region, player, basePrice));

            double playerCosts = price.additionalPlayerCosts();
            price.playerMultiplier((playerCosts * player.priceMultiplier()) - playerCosts);
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

    private double calculatePlayerRegionGroupMultiplier(RegionPlayer player, double basePrice) {

        return calculateMultiplier(player.regionGroups().size(), regionGroupCountMultiplierPower(), regionGroupCountMultiplier(), basePrice);
    }

    private double calculatePlayerSameGroupMultiplier(Region region, RegionPlayer player, double basePrice) {

        return calculateMultiplier(player.regions(region.group()).size(), sameGroupCountMultiplierPower(), sameGroupCountMultiplier(), basePrice);
    }

    private double calculateMultiplier(int count, double power, double multiplier, double basePrice) {

        return (basePrice * ((Math.pow(count, power) * multiplier) + 1.0)) - basePrice;
    }

    private ChatColor costColor(RegionPlayer player, double cost) {

        if (player != null) {
            return economy().has(player.getOfflinePlayer(), cost) ? ChatColor.GREEN : ChatColor.DARK_RED;
        } else {
            return ChatColor.AQUA;
        }
    }

    public enum Type {
        STATIC,
        PER2M,
        PER3M
    }

}
