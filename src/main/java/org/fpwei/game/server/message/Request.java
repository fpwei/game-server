package org.fpwei.game.server.message;

import java.util.HashMap;
import java.util.Map;

public abstract class Request {

    private String gameId;

    private String gameType;

    private Map<String, String> attribute = new HashMap<>();

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

    public Map<String, String> getAttribute() {
        return attribute;
    }

    public String getAttribute(String key) {
        return attribute.get(key);
    }

    public void setAttribute(Map<String, String> attribute) {
        this.attribute = attribute;
    }
}
