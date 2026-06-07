package de.oryfox.discordbot.features;

import de.oryfox.discordbot.model.FeatureConfigurator;
import de.oryfox.discordbot.model.GuildConfiguration;
import de.oryfox.discordbot.persistence.PersistenceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import org.springframework.stereotype.Component;

import java.io.*;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TemporaryVoiceChannelFeature extends ListenerAdapter implements FeatureConfigurator {

    private final JDA jda;
    private final PersistenceService persistenceService;
    private List<String> names;

    private static final SecureRandom RANDOM = new SecureRandom();

    @PostConstruct
    public void initTemporaryVoiceChannels() {
        var file = new File("channel-names.txt");
        if (file.exists()) {
            try (var reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                names = new ArrayList<>(reader.readAllLines());
            } catch (IOException e) {
                log.warn("An error occured while reading the channel-names.txt", e);
            }
        } else {
            try {
                if (file.createNewFile()) {
                    log.warn("Created a channel-names.txt file, which you can fill line by line with available names");
                }
            } catch (IOException e) {
                log.error("Could not create channel-names.txt file.", e);
            }
        }

        jda.addEventListener(this);
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        var config = persistenceService.getConfiguration(event.getGuild().getIdLong());
        if (!config.isTemporaryVoiceChannelEnabled() || config.getTemporaryVoiceChannelChannel() == null) {
            return;
        }
        if (event.getChannelJoined() != null) {
            createAndMove(event, config);
        }
        if (event.getChannelLeft() != null) {
            dropOldChannel(event, config);
        }
    }

    private void createAndMove(GuildVoiceUpdateEvent event, GuildConfiguration config) {
        if (event.getChannelJoined().getIdLong() != config.getTemporaryVoiceChannelChannel()) {
            return;
        }

        var name = names == null || names.isEmpty() ? generateChannelNameFromTime() : generateChannelNameFromList();
        var voiceChannel = event.getGuild().createVoiceChannel(name, event.getChannelJoined().getParentCategory()).complete();
        event.getChannelJoined().getGuild().moveVoiceMember(event.getEntity(), voiceChannel).queue();
        if (config.getTemporaryVoiceChannels() == null) {
            config.setTemporaryVoiceChannels(new ArrayList<>());
        }
        config.getTemporaryVoiceChannels().add(voiceChannel.getIdLong());
        persistenceService.persist(config);
    }

    private void dropOldChannel(GuildVoiceUpdateEvent event, GuildConfiguration config) {
        var oldChannelId = event.getChannelLeft().getIdLong();
        if (config.getTemporaryVoiceChannels() == null) {
            return;
        }
        if (config.getTemporaryVoiceChannels().contains(oldChannelId) && event.getChannelLeft().asVoiceChannel().getMembers().isEmpty()) {
            config.getTemporaryVoiceChannels().remove(oldChannelId);
            event.getChannelLeft().delete().queue();
            persistenceService.persist(config);
        }
    }

    private String generateChannelNameFromTime() {
        return new SimpleDateFormat("HH:mm").format(LocalDateTime.now());
    }

    private String generateChannelNameFromList() {
        return names.get(RANDOM.nextInt(0, names.size()));
    }

    @Override
    public String getFeatureName() {
        return "Temporary Voice Chat";
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
                if (config.getTemporaryVoiceChannelChannel() != null) {
                    config.setTemporaryVoiceChannelEnabled(true);
                    event.reply("The temporary voice chat feature is now enabled").setEphemeral(true).queue();
                } else {
                    event.reply("Please set a channel for temporary voice chat first. See action 'Set Channel'").setEphemeral(true).queue();
                }
            }
            case "disable" -> {
                config.setTemporaryVoiceChannelEnabled(false);
                event.reply("The temporary voice chat feature is now disabled").setEphemeral(true).queue();
            }
            case "set-channel" -> {
                var option = event.getOption("channel");
                if (option != null) {
                    var channel = option.getAsChannel();
                    config.setTemporaryVoiceChannelChannel(channel.getIdLong());
                    event.replyFormat("The temporary voice chat channel is now set to %s", channel.getJumpUrl()).setEphemeral(true).queue();
                } else {
                    event.replyFormat("The temporary voice chat channel could not be set. No channel provided.").setEphemeral(true).queue();
                }
            }
            default -> event.replyFormat("Unknown action %s for feature %s", action, getFeatureName()).setEphemeral(true).queue();
        }
        persistenceService.persist(config);
    }
}
