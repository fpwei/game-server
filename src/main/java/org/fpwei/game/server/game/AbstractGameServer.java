package org.fpwei.game.server.game;

import org.fpwei.game.server.model.Player;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.websocket.Session;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractGameServer implements GameServer {
    protected final List<Player> players = Collections.synchronizedList(new LinkedList<>());


    public void join(Player player) {
        players.add(player);
    }

    public void leave(WebSocketSession session) {
        for (Player player : players) {
            if (player.getSession() == session) {
                players.remove(player);
                break;
            }
        }
    }

    public abstract void start();


    public void broadcast(String message) {
        if (players.size() > 0) {
            players.forEach(player -> {
                try {
                    player.getSession().sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.out.println("Broadcast : " + message);
            });
        }
    }

    protected boolean isPlayerExist() {
        return players.size() > 0;
    }
}
