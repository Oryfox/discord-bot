package de.oryfox.discordbot.features;

import de.oryfox.discordbot.commands.CommandProvider;
import de.oryfox.discordbot.model.FeatureConfigurator;
import de.oryfox.discordbot.persistence.PersistenceService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnonymousChatFeature extends ListenerAdapter implements CommandProvider, FeatureConfigurator {

    private final JDA jda;
    private final PersistenceService persistenceService;
    private static final String COMMAND_NAME = "anonymchat";

    @PostConstruct
    public void initAnonymousChat() {
        jda.addEventListener(this);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equalsIgnoreCase(COMMAND_NAME)) {
            if (persistenceService.getConfiguration(event.getGuild().getIdLong()).isAnonymousChatEnabled()) {
                event.reply(":thumbsup:").setEphemeral(true).queue();
                event.getChannel().sendMessage(event.getInteraction().getOption("message").getAsString()).queue();
            } else {
                event.reply("The anonymous chat feature is disabled on this server.").setEphemeral(true).queue();
            }
        }
    }

    @Override
    public List<CommandData> getCommands() {
        return List.of(Commands.slash(COMMAND_NAME, "Send an anonymous chat message to the current channel")
                .addOption(OptionType.STRING, "message", "The message you want to send anonymously", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_SEND))
                .setContexts(InteractionContextType.GUILD)
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL));
    }

    @Override
    public String getFeatureName() {
        return "Anonymous Chat";
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
        if (action.equalsIgnoreCase("disable")) {
            config.setAnonymousChatEnabled(false);
            event.reply("The Anonymous chat feature is now disabled").setEphemeral(true).queue();
        } else if (action.equalsIgnoreCase("enable")) {
            config.setAnonymousChatEnabled(true);
            event.reply("The Anonymous chat feature is now enabled").setEphemeral(true).queue();
        } else {
            event.reply("The executed command contains errors. Nothing changed.").setEphemeral(true).queue();
            return;
        }
        persistenceService.persist(config);
    }
}
