package org.fpwei.game.server.game;

import org.fpwei.game.server.model.Player;
import org.springframework.web.socket.WebSocketSession;

import javax.websocket.Session;

public interface GameServer {
    void join(Player player);

    void leave(WebSocketSession session);

    void start();

    void broadcast(String message);
}
