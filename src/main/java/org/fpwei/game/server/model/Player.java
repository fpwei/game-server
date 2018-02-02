package org.fpwei.game.server.model;

import org.fpwei.game.server.entity.User;
import org.springframework.web.socket.WebSocketSession;

import java.math.BigDecimal;
import java.util.Objects;

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

    public BigDecimal getBalance(){
        return user.getAmount();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return Objects.equals(Session, player.Session);
    }

    @Override
    public int hashCode() {

        return Objects.hash(Session);
    }
}
