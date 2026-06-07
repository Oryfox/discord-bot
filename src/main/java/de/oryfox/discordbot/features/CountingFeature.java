package de.oryfox.discordbot.features;

import de.oryfox.discordbot.model.FeatureConfigurator;
import de.oryfox.discordbot.model.GuildMember;
import de.oryfox.discordbot.persistence.PersistenceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CountingFeature extends ListenerAdapter implements FeatureConfigurator {

    private final JDA jda;
    private final PersistenceService persistenceService;

    private Map<GuildMember, Instant> userMap = new HashMap<>();
    private long lastUserId;

    @PostConstruct
    public void initCounting() {
        jda.addEventListener(this);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        var config = persistenceService.getConfiguration(event.getGuild().getIdLong());
        if (!config.isCountingEnabled() || config.getCountingChannel() == null) {
            return;
        }

        if (event.getChannel().getIdLong() == config.getCountingChannel()) {
            if (event.getAuthor().isBot() && event.getAuthor().getIdLong() != jda.getSelfUser().getIdLong()) {
                event.getMessage().delete().queue();
                return;
            }

            var messageContent = event.getMessage().getContentRaw();
            var user = event.getAuthor();

            if (lastUserId == user.getIdLong()) {
                event.getMessage().delete().queue();
                return;
            }

            if (userMap.containsKey(new GuildMember(event.getMember())) && Instant.now().isBefore(userMap.get(new GuildMember(event.getMember())).plus(12, ChronoUnit.HOURS))) {
                event.getMessage().delete().queue();
                return;
            }

            if (messageContent.matches("\\d+")) {
                var number = Long.parseLong(messageContent);
                if (number == config.getCountingLast() + 1) {
                    config.setCountingLast(number);
                    userMap.put(new GuildMember(event.getMember()), Instant.now());
                    lastUserId = user.getIdLong();
                    event.getMessage().addReaction(Emoji.fromUnicode("U+2705")).queue();
                    persistenceService.persist(config);
                } else {
                    event.getMessage().addReaction(Emoji.fromUnicode("U+274C")).queue();
                    event.getChannel().sendMessage(String.format("<@%s> killed the streak at %d.%n%nLet's start a new try: 1", user.getId(), config.getCountingLast())).queue();
                    userMap = new HashMap<>();
                    config.setCountingLast(1L);
                    persistenceService.persist(config);
                    lastUserId = 0;
                }
            }
        }
    }

    @Override
    public String getFeatureName() {
        return "Counting";
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
                if (config.getCountingChannel() != null) {
                    config.setCountingEnabled(true);
                    config.setCountingLast(1L);
                    event.reply("The counting feature is now enabled").setEphemeral(true).queue();
                } else {
                    event.reply("Please set a channel for counting first. See action 'Set Channel'").setEphemeral(true).queue();
                }
            }
            case "disable" -> {
                config.setCountingEnabled(false);
                event.reply("The counting feature is now disabled").setEphemeral(true).queue();
            }
            case "set-channel" -> {
                var option = event.getOption("channel");
                if (option != null) {
                    var channel = option.getAsChannel();
                    config.setCountingChannel(channel.getIdLong());
                    event.replyFormat("The counting channel is now set to %s", channel.getJumpUrl()).setEphemeral(true).queue();

                    var embed = new EmbedBuilder()
                            .setTitle("Counting")
                            .setDescription("Here you can count together as a community. The rules are simple. No back to back counting. Only one message per member every 12 hours. If someone makes a mistake, everything gets resetted.")
                            .build();
                    channel.asTextChannel().sendMessageEmbeds(embed).queue();
                    channel.asTextChannel().sendMessage("I'll go first: 1").queue();
                } else {
                    event.replyFormat("The counting channel could not be set. No channel provided.").setEphemeral(true).queue();
                }
            }
            default -> event.replyFormat("Unknown action %s for feature %s", action, getFeatureName()).setEphemeral(true).queue();
        }
        persistenceService.persist(config);
    }
}
