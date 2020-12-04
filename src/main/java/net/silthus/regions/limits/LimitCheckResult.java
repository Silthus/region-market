package net.silthus.regions.limits;

import lombok.Value;

import java.util.EnumSet;

@Value
public class LimitCheckResult {

    EnumSet<Type> types;

    public LimitCheckResult() {
        types = EnumSet.of(Type.SUCCESS);
    }

    public LimitCheckResult(Type type) {
        this.types = EnumSet.of(type);
    }

    public LimitCheckResult(Type type, Type... otherTypes) {
        this.types = EnumSet.of(type, otherTypes);
    }

    public LimitCheckResult(EnumSet<Type> types) {
        this.types = types;
    }

    public boolean success() {

        return types.contains(Type.SUCCESS);
    }

    public enum Type {

        TOTAL_LIMIT_REACHED,
        REGIONS_IN_GROUP_LIMIT_REACHED,
        TOTAL_GROUP_LIMIT_REACHED,
        SUCCESS;
    }
}
