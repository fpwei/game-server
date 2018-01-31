package org.fpwei.game.server.game;

import org.fpwei.game.server.model.Player;

public interface GameServer {
    void join(Player player);

    void leave(Player player);

    void start();

    void broadcast(String message);
}
