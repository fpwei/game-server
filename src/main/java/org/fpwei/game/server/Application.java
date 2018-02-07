package org.fpwei.game.server;

import org.fpwei.game.server.game.Baccarat;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;

import java.util.concurrent.Executor;

@SpringBootApplication
public class Application  extends SpringBootServletInitializer {
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(Application.class);
    }

    @Bean
    public Baccarat baccarat(@Qualifier("betExecutor") TaskExecutor taskExecutor) {
        return new Baccarat(taskExecutor);
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
