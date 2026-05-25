package de.oryfox.discordbot.component;

import de.oryfox.discordbot.commands.CommandProvider;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnonymousChatComponent extends ListenerAdapter implements CommandProvider {

    private final JDA jda;

    private static final String COMMAND_NAME = "anonymchat";

    @PostConstruct
    public void initAnonymousChat() {
        jda.addEventListener(this);
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equalsIgnoreCase(COMMAND_NAME)) {
            event.reply(":thumbsup:").setEphemeral(true).queue();
            event.getChannel().sendMessage(event.getInteraction().getOption("message").getAsString()).queue();
        }
    }

    @Override
    public List<SlashCommandData> getCommands() {
        return List.of(Commands.slash(COMMAND_NAME, "Send an anonymous chat message to the current channel")
                .addOption(OptionType.STRING, "message", "The message you want to send anonymously", true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_SEND))
                .setContexts(InteractionContextType.GUILD)
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL));
    }
}
