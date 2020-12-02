package net.silthus.regions.events;

import lombok.Data;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Data
public abstract class RCRegionEvent extends Event {

    private static final HandlerList handlerList = new HandlerList();
}
