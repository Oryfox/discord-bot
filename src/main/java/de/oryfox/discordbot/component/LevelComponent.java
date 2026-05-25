package de.oryfox.discordbot.component;

import de.oryfox.discordbot.commands.CommandProvider;
import de.oryfox.discordbot.model.UserLevel;
import de.oryfox.discordbot.repository.UserLevelRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LevelComponent extends ListenerAdapter implements CommandProvider {

    private final JDA jda;
    private final UserLevelRepository userLevelRepository;

    @Value("${channels.level.ignored:}")
    private String ignoredChannelIds;

    private final List<String> ignoredChannel = new ArrayList<>();

    private static final String COMMAND_NAME = "level";

    @PostConstruct
    public void initLeveling() {
        if (ignoredChannelIds != null && !ignoredChannelIds.isBlank()) {
            ignoredChannel.addAll(Arrays.asList(ignoredChannelIds.split(",\\s?")));
        }
        jda.addEventListener(this);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        var user = event.getAuthor();
        if (user.isBot()) {
            return;
        }
        if (ignoredChannel.stream().anyMatch(c -> c.equalsIgnoreCase(event.getChannel().getId()))) {
            return;
        }

        int gainedXp;
        var message = event.getMessage();
        try {
            if (!message.getAttachments().isEmpty() && message.getAttachments().stream().anyMatch(a -> a.getContentType().matches("image/.+"))) {
                gainedXp = 8;
            } else if (message.getContentRaw().length() > 10) {
                gainedXp = 5;
            } else {
                gainedXp = 2;
            }
        } catch (Exception _) {
            gainedXp = 2;
        }

        var userLevel = userLevelRepository.findById(user.getIdLong()).orElseGet(() -> new UserLevel(user.getIdLong()));
        var oldLevel = calculateLevel(userLevel.getXp());
        userLevel.setXp(userLevel.getXp() + gainedXp);
        userLevelRepository.save(userLevel);

        var newLevel = calculateLevel(userLevel.getXp());
        if (oldLevel != newLevel) {
            var embed = new EmbedBuilder()
                    .setTitle("Level Up!")
                    .setDescription(String.format("<@%s> reached Level %d!", user.getId(), newLevel))
                    .setColor(Color.GREEN)
                    .build();
            event.getChannel().sendMessageEmbeds(embed).queue();
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equalsIgnoreCase(COMMAND_NAME)) {
            var userLevel = userLevelRepository.findById(event.getUser().getIdLong()).orElseGet(() -> new UserLevel(event.getUser().getIdLong()));
            var container = Container.of(
                    TextDisplay.of("### Level"),
                    Separator.createInvisible(Separator.Spacing.SMALL),
                    TextDisplay.of(String.format("**Experience Points**: %d", userLevel.getXp())),
                    TextDisplay.of(String.format("**Level**: %d", calculateLevel(userLevel.getXp()))),
                    TextDisplay.of(String.format("**Required points for next level**: %d", requiredPointsForNextLevel(userLevel.getXp())))
            );
            event.replyComponents(container)
                    .useComponentsV2()
                    .setEphemeral(true)
                    .queue();
        }
    }

    private int calculateLevel(Long exp) {
        return (int) Math.sqrt(exp / 100.0);
    }

    private long requiredPointsForNextLevel(Long exp) {
        return (long) (100 * Math.pow(calculateLevel(exp) + 1.0, 2)) - exp;
    }

    @Override
    public List<SlashCommandData> getCommands() {
        return List.of(Commands.slash(COMMAND_NAME, "Shows your current Level and Experience Points"));
    }
}
