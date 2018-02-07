package org.fpwei.game.server.game;

import org.fpwei.game.server.model.Player;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;

@Component
@Scope(SCOPE_SINGLETON)
public class Lobby {

    private final CopyOnWriteArrayList<Player> players = new CopyOnWriteArrayList<>();

    @Autowired
    private List<GameServer> servers;


    public void enter(Player player) {
        players.add(player);
    }

    public void leave(Player player) {
        players.remove(player);

        servers.parallelStream().forEach(server -> server.leave(player));
    }

    public void leave(WebSocketSession session) {
        Player player = new Player();
        player.setSession(session);

        leave(player);
    }

    public Player getPlayer(WebSocketSession session) {
        return players.parallelStream()
                .filter(player -> player.getSession().equals(session))
                .findAny()
                .orElse(null);
    }

}
