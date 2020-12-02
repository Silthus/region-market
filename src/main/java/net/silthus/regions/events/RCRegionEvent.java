package net.silthus.regions.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class RCRegionEvent extends Event {

    private static final HandlerList handlerList = new HandlerList();
}
