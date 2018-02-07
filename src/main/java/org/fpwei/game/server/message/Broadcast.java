package org.fpwei.game.server.message;

import org.fpwei.game.server.annotation.Message;

@Message("broadcast")
public class Broadcast {

    private String type;

    private String message;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
