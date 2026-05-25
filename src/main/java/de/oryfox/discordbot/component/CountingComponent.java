package de.oryfox.discordbot.component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CountingComponent extends ListenerAdapter {

    @Value("${channels.counting:}")
    private String channelId;

    private final JDA jda;

    private Map<Long, Instant> userMap;
    private long lastNumber = 1;
    private long lastUserId;

    @PostConstruct
    public void initCounting() {
        if (channelId == null || channelId.isBlank()) {
            log.error("No counting channel provided. Feature is disabled.");
            return;
        }

        userMap = new HashMap<>();

        var channel = jda.getTextChannelById(channelId);
        if (channel == null) {
            log.error("Channel with provided id {} not found", channelId);
        }

        var embed = new EmbedBuilder()
                .setTitle("Counting")
                .setDescription("Here you can count together as a community. The rules are simple. No back to back counting. Only one message per member every 12 hours. If someone makes a mistake, everything gets resetted.")
                .build();
        channel.sendMessageEmbeds(embed).queue();
        channel.sendMessage("I'll go first: 1").queue();

        jda.addEventListener(this);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getChannel().getId().equalsIgnoreCase(channelId)) {
            if (event.getAuthor().isBot()) {
                event.getMessage().delete().queue();
                return;
            }

            var messageContent = event.getMessage().getContentRaw();
            var user = event.getAuthor();

            if (lastUserId == user.getIdLong()) {
                event.getMessage().delete().queue();
                return;
            }

            if (userMap.containsKey(user.getIdLong()) && Instant.now().isBefore(userMap.get(user.getIdLong()).plus(12, ChronoUnit.HOURS))) {
                event.getMessage().delete().queue();
                return;
            }

            if (messageContent.matches("\\d+")) {
                var number = Long.parseLong(messageContent);
                if (number == lastNumber + 1) {
                    lastNumber = number;
                    userMap.put(user.getIdLong(), Instant.now());
                    lastUserId = user.getIdLong();
                    event.getMessage().addReaction(Emoji.fromUnicode("U+2705")).queue();
                } else {
                    event.getMessage().addReaction(Emoji.fromUnicode("U+274C")).queue();
                    event.getChannel().sendMessage(String.format("<@%s> killed the streak at %d.%n%nLet's start a new try: 1", user.getId(), lastNumber)).queue();
                    userMap = new HashMap<>();
                    lastNumber = 1;
                    lastUserId = 0;
                }
            }
        }
    }
}
