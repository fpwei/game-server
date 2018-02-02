package org.fpwei.game.server.game;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.fpwei.game.server.common.CommonRuntimeException;
import org.fpwei.game.server.message.BetRequest;
import org.fpwei.game.server.model.Player;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Slf4j
public abstract class AbstractGameServer implements GameServer {
    protected final List<Player> players = Collections.synchronizedList(new LinkedList<>());

    protected TaskExecutor taskExecutor;

    public AbstractGameServer(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void join(Player player) {
        players.add(player);
    }

    @Override
    public void leave(WebSocketSession session) {
        for (Player player : players) {
            if (player.getSession() == session) {
                players.remove(player);
                break;
            }
        }
    }

    @Override
    public void broadcast(String message) {
        log.debug("Broadcast message: {}", message);

        if (players.size() > 0) {
            players.forEach(player -> {
                try {
                    player.getSession().sendMessage(new TextMessage(message));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    @Override
    public void sendMessage(Player player, String message) {
        if (player.getSession() != null) {
            try {
                player.getSession().sendMessage(new TextMessage(message));
            } catch (IOException e) {
                log.error(ExceptionUtils.getStackTrace(e));
            }
        }
    }

    protected boolean isPlayerExist() {
        return players.size() > 0;
    }

    @Override
    public void bet(Player player, BetRequest request) {
        taskExecutor.execute(() -> {
            try {
                if (bet0(player, request)) {
                    player.getSession().sendMessage(new TextMessage("下注成功"));
                } else {
                    player.getSession().sendMessage(new TextMessage("下注失敗"));
                }
            } catch (CommonRuntimeException e) {
                try {
                    player.getSession().sendMessage(new TextMessage(e.getMessage()));
                } catch (IOException e1) {
                    log.error(ExceptionUtils.getStackTrace(e1));
                }
            } catch (IOException e1) {
                log.error(ExceptionUtils.getStackTrace(e1));
            }
        });
    }

    protected abstract boolean bet0(Player player, BetRequest request);
}
