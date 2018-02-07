package org.fpwei.game.server.message;

import org.fpwei.game.server.annotation.Message;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Message("bet")
public class BetRequest extends Request{

    private BigDecimal amount;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
