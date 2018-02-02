package org.fpwei.game.server.game;

import org.fpwei.game.server.message.BetRequest;
import org.fpwei.game.server.model.Player;
import org.springframework.web.socket.WebSocketSession;

public interface GameServer {
    void join(Player player);

    void leave(WebSocketSession session);

    void start();

    void sendMessage(Player player, String message);

    void broadcast(String message);

    void bet(Player player, BetRequest request);
}
