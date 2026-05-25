package de.oryfox.discordbot.component;

import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resources;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.filedisplay.FileDisplay;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

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
        if (event.getAuthor().isBot()) {
            return;
        }

        var container = Container.of(
                TextDisplay.of("### Message Updated"),

                Separator.createInvisible(Separator.Spacing.SMALL),

                TextDisplay.of(String.format("**User:** <@%s>", event.getAuthor().getId())),
                TextDisplay.of(String.format("**Channel:** %s", event.getChannel().getJumpUrl())),
                TextDisplay.of(String.format("**Link to message:** %s", event.getMessage().getJumpUrl())),

                Separator.createInvisible(Separator.Spacing.SMALL),

                TextDisplay.of(String.format("-# <t:%s:f>", Instant.now().getEpochSecond()))
        );

        channel.sendMessageComponents(container)
                .useComponentsV2()
                .queue();
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        var container = Container.of(
                TextDisplay.of("### Message Deleted"),

                Separator.createInvisible(Separator.Spacing.SMALL),

                TextDisplay.of(String.format("**Channel:** %s", event.getChannel().getJumpUrl())),
                TextDisplay.of(String.format("**Message-ID:** `%s`", event.getMessageId())),

                Separator.createInvisible(Separator.Spacing.SMALL),

                TextDisplay.of(String.format("-# <t:%s:f>", Instant.now().getEpochSecond()))
        );

        channel.sendMessageComponents(container)
                .useComponentsV2()
                .queue();
    }

    @Override
    public void onMessageBulkDelete(MessageBulkDeleteEvent event) {
        var container = Container.of(
                TextDisplay.of("### Message Deleted"),

                Separator.createInvisible(Separator.Spacing.SMALL),

                TextDisplay.of(String.format("**Channel:** %s", event.getChannel().getJumpUrl())),
                TextDisplay.of(String.format("**Message-IDs:** `%s`", String.join(", ", event.getMessageIds()))),

                Separator.createInvisible(Separator.Spacing.SMALL),

                TextDisplay.of(String.format("-# <t:%s:f>", Instant.now().getEpochSecond()))
        );

        channel.sendMessageComponents(container)
                .useComponentsV2()
                .queue();
    }
}

