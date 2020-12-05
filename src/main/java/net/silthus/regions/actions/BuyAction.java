package net.silthus.regions.actions;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;
import net.milkbowl.vault.economy.Economy;
import net.silthus.regions.Cost;
import net.silthus.regions.RegionsPlugin;
import net.silthus.regions.costs.PriceDetails;
import net.silthus.regions.entities.Region;
import net.silthus.regions.entities.RegionPlayer;
import net.silthus.regions.entities.RegionTransaction;
import net.silthus.regions.entities.Sale;
import net.silthus.regions.events.BoughtRegionEvent;
import net.silthus.regions.events.BuyRegionEvent;
import net.silthus.regions.events.BuyRegionFromPlayerEvent;
import net.silthus.regions.limits.LimitCheckResult;
import org.bukkit.Bukkit;

import java.util.EnumSet;
import java.util.Optional;

@Value
@NonFinal
@EqualsAndHashCode(callSuper = true)
public class BuyAction extends RegionAction {

    public BuyAction(@NonNull Region region, @NonNull RegionPlayer regionPlayer) {
        super(region, regionPlayer);
    }

    public BuyAction(@NonNull BuyAction action) {
        super(action);
    }

    public Result check() {

        PriceDetails priceDetails = getRegion().priceDetails(getRegionPlayer());
        LimitCheckResult limitsCheck = getRegionPlayer().checkLimits(getRegion());

        Optional<RegionPlayer> owner = getRegion().owner();
        if (getRegion().status() == Region.Status.OCCUPIED) {
            if (owner.isEmpty()) {
                return new Result(this, priceDetails, limitsCheck, "Das Grundstück " + getRegion().name() + " gehört bereits jemandem steht aber fehlerhaft in der Datenbank. " +
                        "Bitte kontaktiere einen Admin mit der Grundstücks ID: " + getRegion().id(), Result.Status.OTHER);
            } else if (owner.get().equals(getRegionPlayer())) {
                return new Result(this, priceDetails, limitsCheck, "Du besitzt das Grundstück " + getRegion().name() + " bereits.", Result.Status.OWNED_BY_SELF);
            } else {
                return new Result(this, priceDetails, limitsCheck, "Das Grundstück " + getRegion().name() + " gehört bereits " + owner.map(RegionPlayer::name).orElse("jemandem."), Result.Status.OWNED_BY_OTHER);
            }
        }

        if (!limitsCheck.success()) {
            return new Result(this, priceDetails, limitsCheck, "Du hast deine maximale Anzahl an erlaubten Grundstücken erreicht.", Result.Status.LIMITS_REACHED);
        }

        Cost.Result costsCheck = getRegion().costs().stream()
                .map(cost -> cost.check(getRegion(), getRegionPlayer()))
                .reduce(Cost.Result::combine)
                .orElse(new Cost.Result(true, null, priceDetails, Cost.ResultStatus.SUCCESS));

        if (costsCheck.failure()) {
            return new Result(this, priceDetails, limitsCheck, costsCheck.error(), Result.Status.COSTS_NOT_MET);
        }

        return new Result(this, priceDetails, limitsCheck);
    }

    public Result run() {

        Result result = check();

        BuyRegionEvent buyRegionEvent = new BuyRegionEvent(result);
        Bukkit.getPluginManager().callEvent(buyRegionEvent);

        if (buyRegionEvent.isCancelled()) {
            return new Result(result, "Das Kaufen der Region wurde durch ein Plugin verhindert.", Result.Status.EVENT_CANCELLED);
        }

        result = buyRegionEvent.getResult();

        if (result.failure()) {
            return result;
        }

        PriceDetails priceDetails = result.getPriceDetails();

        // lets to a final money check - just to be sure
        Economy economy = RegionsPlugin.instance().getEconomy();
        final RegionPlayer regionPlayer = result.getRegionPlayer();

        if (!economy.has(regionPlayer.getOfflinePlayer(), priceDetails.total())) {
            return new Result(result, "Du hast nicht genügend Geld um das Grundstück zu kaufen.", Result.Status.NOT_ENOUGH_MONEY);
        }


        Optional<Sale> optionalSale = Sale.getActiveSale(result.getRegion());
        if (optionalSale.isPresent()) {
            Sale sale = optionalSale.get();

            if (sale.type() == Sale.Type.DIRECT) {
                BuyRegionFromPlayerEvent event = new BuyRegionFromPlayerEvent(result, sale.seller(), sale.price());
                Bukkit.getPluginManager().callEvent(event);

                if (event.isCancelled()) {
                    return new Result(result, "Das Kaufen der Region von einem anderen Spieler wurde durch ein Plugin verhindert.", Result.Status.EVENT_CANCELLED);
                }

                if (event.getResult().failure()) {
                    return event.getResult();
                }

                economy.depositPlayer(sale.seller().getOfflinePlayer(), sale.price());
                RegionTransaction.of(getRegion(), sale.seller(), RegionTransaction.Action.SELL_TO_PLAYER)
                        .data("buyer_id", regionPlayer.id())
                        .data("buyer", regionPlayer.name())
                        .data("price", sale.price())
                        .save();
            }

            sale.buyer(regionPlayer).save();
        }

        economy.withdrawPlayer(regionPlayer.getOfflinePlayer(), result.getPriceDetails().total());

        Region region = result.getRegion();
        region.owner(regionPlayer)
                .status(Region.Status.OCCUPIED)
                .save();

        RegionTransaction.of(region, regionPlayer, RegionTransaction.Action.BUY)
                .data("price", result.getPriceDetails().total())
                .save();

        region.updateSigns();

        Bukkit.getPluginManager().callEvent(new BoughtRegionEvent(result));

        return result;
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    public static class Result extends BuyAction {

        PriceDetails priceDetails;
        LimitCheckResult limitCheckResult;
        String error;
        EnumSet<Status> statuses;

        private Result(Result result, String error, Status status) {
            super(result);
            this.priceDetails = result.priceDetails;
            this.limitCheckResult = result.limitCheckResult;
            this.error = error;
            this.statuses = EnumSet.of(status);
        }

        public Result(@NonNull BuyAction action, PriceDetails priceDetails) {
            super(action);
            this.priceDetails = priceDetails;
            this.limitCheckResult = new LimitCheckResult();
            this.error = null;
            this.statuses = EnumSet.of(Status.SUCCESS);
        }

        private Result(BuyAction action, PriceDetails priceDetails, LimitCheckResult limitCheckResult) {
            super(action);
            this.priceDetails = priceDetails;
            this.limitCheckResult = limitCheckResult;
            this.error = null;
            this.statuses = EnumSet.of(Status.SUCCESS);
        }

        private Result(BuyAction action, PriceDetails priceDetails, LimitCheckResult limitCheckResult, String error, Status reason) {
            super(action);
            this.priceDetails = priceDetails;
            this.limitCheckResult = limitCheckResult;
            this.error = error;
            this.statuses = EnumSet.of(reason);
        }

        private Result(BuyAction action, PriceDetails priceDetails, LimitCheckResult limitCheckResult, String error, Status reason, Status... additionalReasons) {
            super(action);
            this.priceDetails = priceDetails;
            this.limitCheckResult = limitCheckResult;
            this.error = error;
            this.statuses = EnumSet.of(reason, additionalReasons);
        }

        public boolean success() {

            return statuses.contains(Status.SUCCESS);
        }

        public boolean failure() {

            return !success();
        }

        private double price() {

            return getPriceDetails().total();
        }

        public String formattedPrice() {

            return RegionsPlugin.instance().getEconomy().format(price());
        }

        public enum Status {
            UNKNOWN,
            OWNED_BY_OTHER,
            OWNED_BY_SELF,
            SUCCESS,
            LIMITS_REACHED,
            NOT_ENOUGH_MONEY,
            COSTS_NOT_MET,
            OTHER,
            EVENT_CANCELLED;
        }
    }
}
