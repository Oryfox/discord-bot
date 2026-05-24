package de.oryfox.discordbot.component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoggingComponent extends ListenerAdapter {

    @Value("${channels.logging:}")
    private String channelId;

    private TextChannel channel;

    private final JDA jda;

    @PostConstruct
    public void initLogging() {
        if (channelId == null || channelId.isBlank()) {
            log.error("No logging channel provided. Feature is disabled.");
            return;
        }

        channel = jda.getTextChannelById(channelId);

        jda.addEventListener(this);
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        var embed = new EmbedBuilder()
                .setTitle("Message Updated")
                .setDescription(String.format("<@%s> updated the message %s in channel %s", event.getAuthor().getId(), event.getMessage().getJumpUrl(), event.getChannel().getJumpUrl()))
                .setColor(Color.ORANGE)
                .build();

        channel.sendMessageEmbeds(embed).queue();
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        var embed = new EmbedBuilder()
                .setTitle("Message Deleted")
                .setDescription(String.format("Message with id `%d` deleted in %s", event.getMessageIdLong(), event.getChannel().getJumpUrl()))
                .setColor(Color.RED)
                .build();

        channel.sendMessageEmbeds(embed).queue();
    }

    @Override
    public void onMessageBulkDelete(MessageBulkDeleteEvent event) {
        var embed = new EmbedBuilder()
                .setTitle("Message Deleted")
                .setDescription(String.format("Messages with ids `%s` deleted in %s", String.join(", ", event.getMessageIds()), event.getChannel().getJumpUrl()))
                .setColor(Color.RED)
                .build();

        channel.sendMessageEmbeds(embed).queue();
    }
}

