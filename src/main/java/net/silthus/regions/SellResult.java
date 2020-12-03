package net.silthus.regions;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class SellResult extends SellAction {

    double price;

    public SellResult(SellAction sellAction, double price) {
        super(sellAction);
        this.price = price;
    }
}
