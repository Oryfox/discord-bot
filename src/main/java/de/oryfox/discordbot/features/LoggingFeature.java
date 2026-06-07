package de.oryfox.discordbot.features;

import de.oryfox.discordbot.model.FeatureConfigurator;
import de.oryfox.discordbot.persistence.PersistenceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoggingFeature extends ListenerAdapter implements FeatureConfigurator {

    private final JDA jda;
    private final PersistenceService persistenceService;

    @PostConstruct
    public void initLogging() {
        jda.addEventListener(this);
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        var config = persistenceService.getConfiguration(event.getGuild().getIdLong());
        if (!config.isLoggingEnabled() || config.getLoggingChannel() == null) {
            return;
        }
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

        var channel = jda.getTextChannelById(config.getLoggingChannel());
        if (channel == null) {
            config.setLoggingEnabled(false);
            persistenceService.persist(config);
            event.getGuild()
                    .getOwner()
                    .getDefaultChannel()
                    .asTextChannel()
                    .sendMessageFormat("The logging feature got disabled on %s because the logging channel could not be found. Please set a new channel")
                    .queue();
            return;
        }
        channel.sendMessageComponents(container)
                .useComponentsV2()
                .queue();
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        var config = persistenceService.getConfiguration(event.getGuild().getIdLong());
        if (!config.isLoggingEnabled() || config.getLoggingChannel() == null) {
            return;
        }
        var container = Container.of(
                TextDisplay.of("### Message Deleted"),

                Separator.createInvisible(Separator.Spacing.SMALL),

                TextDisplay.of(String.format("**Channel:** %s", event.getChannel().getJumpUrl())),
                TextDisplay.of(String.format("**Message-ID:** `%s`", event.getMessageId())),

                Separator.createInvisible(Separator.Spacing.SMALL),

                TextDisplay.of(String.format("-# <t:%s:f>", Instant.now().getEpochSecond()))
        );

        var channel = jda.getTextChannelById(config.getLoggingChannel());
        if (channel == null) {
            config.setLoggingEnabled(false);
            persistenceService.persist(config);
            event.getGuild()
                    .getOwner()
                    .getUser()
                    .openPrivateChannel()
                    .complete()
                    .sendMessageFormat("The logging feature got disabled on `%s` because the logging channel could not be found. Please set a new channel and re-enable the feature", event.getGuild().getName())
                    .queue();
            return;
        }
        channel.sendMessageComponents(container)
                .useComponentsV2()
                .queue();
    }


    @Override
    public void onMessageBulkDelete(MessageBulkDeleteEvent event) {
        var config = persistenceService.getConfiguration(event.getGuild().getIdLong());
        if (!config.isLoggingEnabled() || config.getLoggingChannel() == null) {
            return;
        }
        var container = Container.of(
                TextDisplay.of("### Message Deleted"),

                Separator.createInvisible(Separator.Spacing.SMALL),

                TextDisplay.of(String.format("**Channel:** %s", event.getChannel().getJumpUrl())),
                TextDisplay.of(String.format("**Message-IDs:** `%s`", String.join(", ", event.getMessageIds()))),

                Separator.createInvisible(Separator.Spacing.SMALL),

                TextDisplay.of(String.format("-# <t:%s:f>", Instant.now().getEpochSecond()))
        );

        var channel = jda.getTextChannelById(config.getLoggingChannel());
        if (channel == null) {
            config.setLoggingEnabled(false);
            persistenceService.persist(config);
            event.getGuild()
                    .getOwner()
                    .getDefaultChannel()
                    .asTextChannel()
                    .sendMessageFormat("The logging feature got disabled on %s because the logging channel could not be found. Please set a new channel")
                    .queue();
            return;
        }
        channel.sendMessageComponents(container)
                .useComponentsV2()
                .queue();
    }

    @Override
    public String getFeatureName() {
        return "Logging";
    }

    @Override
    public List<Command.Choice> getActions() {
        return List.of(
                new Command.Choice("Disable", "disable"),
                new Command.Choice("Enable", "enable"),
                new Command.Choice("Set Channel", "set-channel")
        );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        var config = persistenceService.getConfiguration(event.getGuild().getIdLong());
        var action = event.getOption("action").getAsString();
        switch (action) {
            case "enable" -> {
                if (config.getLoggingChannel() != null) {
                    config.setLoggingEnabled(true);
                    event.reply("The logging feature is now enabled").setEphemeral(true).queue();
                } else {
                    event.reply("Please set a channel for logging first. See action 'Set Channel'").setEphemeral(true).queue();
                }
            }
            case "disable" -> {
                config.setLoggingEnabled(false);
                event.reply("The logging feature is now disabled").setEphemeral(true).queue();
            }
            case "set-channel" -> {
                var option = event.getOption("channel");
                if (option != null) {
                    var channel = option.getAsChannel();
                    config.setLoggingChannel(channel.getIdLong());
                    event.replyFormat("The logging channel is now set to %s", channel.getJumpUrl()).setEphemeral(true).queue();
                } else {
                    event.replyFormat("The logging channel could not be set. No channel provided.").setEphemeral(true).queue();
                }
            }
            default -> event.replyFormat("Unknown action %s for feature %s", action, getFeatureName()).setEphemeral(true).queue();
        }
        persistenceService.persist(config);
    }
}

