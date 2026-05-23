package de.oryfox.discordbot.config;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumSet;

@Configuration
@Slf4j
public class JdaConfig {

    @Value("${discord.token:}")
    private String discordToken;

    @Bean(destroyMethod = "awaitShutdown")
    @SneakyThrows
    public JDA jda() {
        if (discordToken == null || discordToken.isBlank()) {
            log.error("Please provide a Discord Bot token.");
            return null;
        }
        return JDABuilder
                .create(discordToken, EnumSet.allOf(GatewayIntent.class))
                .build()
                .awaitReady();
    }
}
