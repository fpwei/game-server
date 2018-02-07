package org.fpwei.game.server.common;

import org.fpwei.game.server.annotation.Message;
import org.fpwei.game.server.game.Baccarat;
import org.fpwei.game.server.game.GameServer;
import org.fpwei.game.server.game.Lobby;
import org.fpwei.game.server.message.LoginRequest;
import org.fpwei.game.server.message.Request;
import org.fpwei.game.server.model.Player;
import org.fpwei.game.server.service.UserService;
import org.fpwei.game.server.utils.JsonUtils;
import org.reflections.Reflections;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;

@Component
public class MessageHandler implements ApplicationContextAware {

    @Autowired
    private UserService userService;

    @Autowired
    private Lobby lobby;

    private ApplicationContext applicationContext;

    private Reflections ref = new Reflections("org.fpwei.game.server.message");

    public void handleMessage(WebSocketSession session, String msg) {
        Object request = parseMessage(msg);

        if (request instanceof LoginRequest) {
            Player player = new Player();
            player.setSession(session);
            player.setUser(userService.login((LoginRequest) request));

            lobby.enter(player);
        } else if (request instanceof Request) {
            Player player = lobby.getPlayer(session);
            if (player != null) {
                dispatchMessage(player, (Request) request);
            } else {
                throw new CommonRuntimeException("Player not exist");
            }
        }
    }

    public void dispatchMessage(Player player, Request request) {
        GameServer server = getGameServer(request.getGameType(), request.getGameId());
        server.process(player, request);
    }

    private GameServer getGameServer(String gameType, String gameId) {
        //TODO search from DB or others way
        return applicationContext.getBean(Baccarat.class);
    }


    private Object parseMessage(String text) {
        String messageType = JsonUtils.getRootKey(text);

        Optional<Class<?>> clazz = ref.getTypesAnnotatedWith(Message.class).parallelStream()
                .filter(c -> c.getAnnotation(Message.class).value().equals(messageType))
                .findAny();

        if (clazz.isPresent()) {
            return JsonUtils.fromJson(JsonUtils.getJson(messageType, text), clazz.get());
        } else {
            throw new CommonRuntimeException("Illegal request format");
        }
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
