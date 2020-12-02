package net.silthus.regions.util;

import com.google.common.base.Strings;

public final class Enums {

    public static <T extends Enum<?>> T searchEnum(Class<T> enumeration,
                                                   String search) {

        if (Strings.isNullOrEmpty(search)) {
            return null;
        }

        for (T each : enumeration.getEnumConstants()) {
            if (each.name().compareToIgnoreCase(search) == 0) {
                return each;
            }
        }
        return null;
    }
}
