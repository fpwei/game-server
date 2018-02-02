package org.fpwei.game.server.controller;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.fpwei.game.server.entity.User;
import org.fpwei.game.server.game.Baccarat;
import org.fpwei.game.server.model.Player;
import org.fpwei.game.server.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Controller
public class WebSocketHandler extends TextWebSocketHandler {

    public static final CopyOnWriteArrayList<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    private static final int LOGIN_GAME = 0;

    private static final Gson gson = new Gson();

    @Autowired
    private Baccarat baccarat;

    @Autowired
    private LoginService loginService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.debug("Receive message [{}] from session: {}", message.getPayload(), session.getId());
        String text = message.getPayload();

        if ("join".equals(text)) {
            Player player = new Player();
            player.setSession(session);
            baccarat.join(player);
        }

//        String[] arr = text.split("!@#"); //MessageType!@#GameId,LoginKey,GameType
//        String messageType = arr[0];
//
//        if (StringUtils.isNumeric(messageType)) {
//            session.sendMessage(new TextMessage("ERROR"));
//            return;
//        }
//
//        switch (Integer.valueOf(messageType)) {
//            case LOGIN_GAME:
//                User user = loginService.login(arr[1].split(","));
//                if (user == null) {
//                    session.sendMessage(new TextMessage("login failed"));
//                }else{
//                    session.sendMessage(new TextMessage(String.format("6!@#%s,%s", user.getBalance().toString(), user.getName())));
//                    Player player = new Player();
//                    player.setUser(user);
//                    player.setSession(session);
//                    baccarat.join(player);
//                }
//
//                break;
//            default://沒有設定到的一律視為遊戲封包 封包ID+遊戲類別+誰+中文內容
////                baccarat.setProcess(this.getPosKey(), text);
//
//                break;
//
//        }
//    }

    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        baccarat.leave(session);
    }



}
