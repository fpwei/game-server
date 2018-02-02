package org.fpwei.game.server.common;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.fpwei.game.server.game.GameServer;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class MessageHandler implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    public void handleMessage(String str) {
        JsonParser parser = new JsonParser();
        JsonObject obj = parser.parse(str).getAsJsonObject();
        obj.keySet().;
        obj.get

        String gameId = "";

        GameServer gameServer = (GameServer) applicationContext.getBean(gameId);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
