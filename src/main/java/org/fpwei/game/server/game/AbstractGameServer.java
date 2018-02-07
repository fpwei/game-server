package org.fpwei.game.server.game;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.fpwei.game.server.common.CommonRuntimeException;
import org.fpwei.game.server.message.Broadcast;
import org.fpwei.game.server.message.JoinGameRequest;
import org.fpwei.game.server.message.Request;
import org.fpwei.game.server.model.Player;
import org.fpwei.game.server.utils.JsonUtils;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.socket.TextMessage;

import java.io.IOException;
import java.math.RoundingMode;
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

        sendUserInfo(player);
    }

    @Override
    public void leave(Player player) {
        players.removeIf(p -> p.equals(player));
    }

    @Override
    public void broadcast(String message) {
        log.debug("Broadcast message: {}", message);

        Broadcast broadcast = new Broadcast();
        broadcast.setType("broadcast");
        broadcast.setMessage(message);
        final String msg = JsonUtils.toJson(broadcast);

        if (players.size() > 0) {
            players.forEach(player -> {
                try {
                    player.getSession().sendMessage(new TextMessage(msg));
                } catch (IOException | IllegalStateException e) {
                    log.error(ExceptionUtils.getStackTrace(e));
                }
            });
        }
    }

    @Override
    public void sendMessage(Player player, Object object) {
        if (player.getSession() != null) {
            try {
                player.getSession().sendMessage(new TextMessage(JsonUtils.toJson(object)));
            } catch (IOException e) {
                log.error(ExceptionUtils.getStackTrace(e));
            }
        }
    }

    protected void sendMessage(Player player, String string) {
        Broadcast broadcast = new Broadcast();
        broadcast.setType("response");
        broadcast.setMessage(string);

        sendMessage(player, broadcast);
    }


    protected boolean isPlayerExist() {
        return players.size() > 0;
    }

    @Override
    public void process(Player player, Request request) {
        if (request instanceof JoinGameRequest) {
            this.join(player);
        }

        taskExecutor.execute(() -> {
            try {
                if (process0(player, request)) {
                    sendMessage(player, "success");
                } else {
                    sendMessage(player, "failed");
                    sendUserInfo(player);
                }
            } catch (CommonRuntimeException e) {
                sendMessage(player, e.getMessage());
                sendUserInfo(player);
            }
        });
    }

    protected abstract boolean process0(Player player, Request request);

    protected void sendUserInfo(Player player) {
        Broadcast broadcast = new Broadcast();
        broadcast.setType("user");
        broadcast.setMessage(player.getUser().getName() + "," + player.getUser().getAmount().setScale(2, RoundingMode.DOWN));

        sendMessage(player, broadcast);
    }
}
