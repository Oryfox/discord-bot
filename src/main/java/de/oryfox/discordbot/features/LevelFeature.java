package de.oryfox.discordbot.features;

import de.oryfox.discordbot.commands.CommandProvider;
import de.oryfox.discordbot.model.FeatureConfigurator;
import de.oryfox.discordbot.model.GuildMember;
import de.oryfox.discordbot.model.UserLevel;
import de.oryfox.discordbot.persistence.PersistenceService;
import de.oryfox.discordbot.persistence.UserLevelRepository;
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
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LevelFeature extends ListenerAdapter implements CommandProvider, FeatureConfigurator {

    private final JDA jda;
    private final PersistenceService persistenceService;
    private final UserLevelRepository userLevelRepository;

    private static final String COMMAND_NAME = "level";

    @PostConstruct
    public void initLeveling() {
        jda.addEventListener(this);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        var config = persistenceService.getConfiguration(event.getGuild().getIdLong());
        if (!config.isLevelingEnabled()) {
            return;
        }
        var user = event.getAuthor();
        if (user.isBot()) {
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

        var userLevel = userLevelRepository.findById(new GuildMember(event.getMember())).orElseGet(() -> new UserLevel(user.getIdLong(), event.getGuild().getIdLong()));
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
        var config = persistenceService.getConfiguration(event.getGuild().getIdLong());
        if (event.getName().equalsIgnoreCase(COMMAND_NAME)) {
            if (!config.isLevelingEnabled()) {
                event.reply("The leveling feature is not enabled on this server.").setEphemeral(true).queue();
                return;
            }
            var userLevel = userLevelRepository.findById(new GuildMember(event.getMember())).orElseGet(() -> new UserLevel(event.getUser().getIdLong(), event.getGuild().getIdLong()));
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
    public List<CommandData> getCommands() {
        return List.of(Commands.slash(COMMAND_NAME, "Shows your current Level and Experience Points").setContexts(InteractionContextType.GUILD));
    }

    @Override
    public String getFeatureName() {
        return "Leveling";
    }

    @Override
    public List<Command.Choice> getActions() {
        return List.of(
                new Command.Choice("Disable", "disable"),
                new Command.Choice("Enable", "enable")
        );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        var config = persistenceService.getConfiguration(event.getGuild().getIdLong());
        var action = event.getOption("action").getAsString();
        switch (action) {
            case "enable" -> {
                config.setLevelingEnabled(true);
                event.reply("The leveling feature is now enabled").setEphemeral(true).queue();
            }
            case "disable" -> {
                config.setLevelingEnabled(false);
                event.reply("The leveling feature is now disabled").setEphemeral(true).queue();
            }
            default -> event.replyFormat("Unknown action %s for feature %s", action, getFeatureName()).setEphemeral(true).queue();
        }
        persistenceService.persist(config);
    }
}
