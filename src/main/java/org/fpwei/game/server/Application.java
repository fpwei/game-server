package org.fpwei.game.server;

import org.fpwei.game.server.game.Baccarat;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {

    @Bean
    public Baccarat baccarat() {
        return new Baccarat();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
