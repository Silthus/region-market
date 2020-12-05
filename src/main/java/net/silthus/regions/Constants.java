package net.silthus.regions;

public final class Constants {

    public static final String SIGN_TAG = "[region]";

    public static final String PRICE_MODIFIER_PREFIX = "rcregions.price-multiplier.";
    public static final String LIMITS_PREFIX = "rcregions.limits.";
    public static final String LIMITS_OVERRIDE_PREFIX = "rcregions.player-limits.";

    public static final String PERMISSION_SIGN_CREATE = "rcregions.region.sign.create";
    public static final String PERMISSION_SIGN_DESTROY = "rcregions.region.sign.destroy";
    public static final String PERMISSION_SIGN_BUY = "rcregions.region.sign.buy";

    public static final String BUY_REGION_COMMAND = "/rcr buy %s";
    public static final String SELL_REGION_FOR_OTHERS_PERMISSION = "rcregions.region.sell.others";

    public static final class Permissions {

        public static final String BUY_REGION_FOR_OTHERS = "rcregions.region.buy.others";
        public static final String REGION_LIST_FOR_OTHERS = "rcregions.region.list.others";
    }

}
