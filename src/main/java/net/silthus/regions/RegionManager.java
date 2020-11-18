package net.silthus.regions;

import lombok.extern.java.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Log(topic = "sRegionMarket")
public final class RegionManager {

    private final Map<String, Cost.Registration<?>> costTypes = new HashMap<>();

    public <TCost extends Cost> RegionManager register(Class<TCost> costClass, Supplier<TCost> supplier) {

        if (!costClass.isAnnotationPresent(CostType.class)) {
            log.severe(costClass.getCanonicalName() + " is missing the @CostType annotation. Cannot register it.");
            return this;
        }

        String type = costClass.getAnnotation(CostType.class).value().toLowerCase();

        if (costTypes.containsKey(type)) {
            log.warning("Cannot register cost type " + costClass.getCanonicalName()
                    + "! A duplicate cost with the identifier " + type + " already exists: "
                    + costTypes.get(type).costClass().getCanonicalName());
            return this;
        }

        costTypes.put(type, new Cost.Registration<>(type, costClass, supplier));
        log.info("registered cost type \"" + type + "\": " + costClass.getCanonicalName());
        return this;
    }
}
