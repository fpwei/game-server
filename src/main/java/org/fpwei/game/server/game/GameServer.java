package org.fpwei.game.server.game;

import org.fpwei.game.server.message.Request;
import org.fpwei.game.server.model.Player;
import org.springframework.web.socket.WebSocketSession;

public interface GameServer {
    void join(Player player);

    void leave(Player player);

    void start();

    void sendMessage(Player player, Object object);

    void broadcast(String message);

    void process(Player player, Request request);
}
