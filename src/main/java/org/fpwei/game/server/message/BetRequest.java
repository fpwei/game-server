package org.fpwei.game.server.message;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;


public class BetRequest {

    private String gameId;

    private String gameType;

    private BigDecimal amount;

    private Map<String, String> attributeMap = new HashMap<>();

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getGameType() {
        return gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getAttribute(String key) {
        return attributeMap.get(key);
    }

    public void setAttribute(String key, String value) {
        this.attributeMap = new HashMap<>();
        addAttribute(key, value);
    }

    public void addAttribute(String key, String value) {
        attributeMap.put(key, value);
    }
}
