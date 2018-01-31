package org.fpwei.game.server.model;

import org.fpwei.game.server.entity.User;
import org.springframework.web.socket.WebSocketSession;

public class Player {

    private User user;

    private WebSocketSession Session;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public WebSocketSession getSession() {
        return Session;
    }

    public void setSession(WebSocketSession session) {
        Session = session;
    }
}
