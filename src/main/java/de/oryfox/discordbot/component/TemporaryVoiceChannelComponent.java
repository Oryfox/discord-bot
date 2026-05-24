package de.oryfox.discordbot.component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.beans.factory.annotation.Value;
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
public class TemporaryVoiceChannelComponent extends ListenerAdapter {

    @Value("${channels.temporary.category:}")
    private String categoryId;

    @Value("${channels.temporary.channel:}")
    private String channelId;

    private VoiceChannel channel;
    private Category category;
    private final JDA jda;
    private List<String> names;
    private final List<Long> createdChannels = new ArrayList<>();

    private static final SecureRandom RANDOM = new SecureRandom();

    @PostConstruct
    public void initTemporaryVoiceChannels() {
        if (channelId == null || channelId.isBlank()) {
            log.error("No channel id set for the temporary voice channels. Feature is disabled.");
            return;
        }
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
        if (categoryId == null || categoryId.isBlank()) {
            log.warn("No category id is set for the temporary voice channels feature. Using root category instead.");
        } else {
            category = jda.getCategoryById(categoryId);
        }
        channel = jda.getVoiceChannelById(channelId);
        if (channel == null) {
            log.error("Temporary Voice Channel with id {} could not be found. Feature is disabled.", channelId);
            return;
        }

        jda.addEventListener(this);
    }

    @Override
    public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getChannelJoined() != null) {
            createAndMove(event);
        }
        if (event.getChannelLeft() != null) {
            dropOldChannel(event);
        }
    }

    private void createAndMove(GuildVoiceUpdateEvent event) {
        if (event.getChannelJoined().getIdLong() != channel.getIdLong()) {
            return;
        }

        var name = names == null || names.isEmpty() ? generateChannelNameFromTime() : generateChannelNameFromList();
        if (category != null) {
            event.getGuild().createVoiceChannel(name, category)
                    .submit()
                    .thenAccept(voiceChannel -> {
                        channel.getGuild().moveVoiceMember(event.getEntity(), voiceChannel).queue();
                        createdChannels.add(voiceChannel.getIdLong());
                    });
        } else {
            event.getGuild().createVoiceChannel(name)
                    .submit()
                    .thenAccept(voiceChannel -> {
                        channel.getGuild().moveVoiceMember(event.getEntity(), voiceChannel).queue();
                        createdChannels.add(voiceChannel.getIdLong());
                    });
        }
    }

    private void dropOldChannel(GuildVoiceUpdateEvent event) {
        var oldChannelId = event.getChannelLeft().getIdLong();
        if (createdChannels.contains(oldChannelId) && event.getChannelLeft().asVoiceChannel().getMembers().isEmpty()) {
            createdChannels.remove(oldChannelId);
            event.getChannelLeft().delete().queue();
        }
    }

    private String generateChannelNameFromTime() {
        return new SimpleDateFormat("HH:mm").format(LocalDateTime.now());
    }

    private String generateChannelNameFromList() {
        return names.get(RANDOM.nextInt(0, names.size()));
    }
}
