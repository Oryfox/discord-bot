package de.oryfox.discordbot.component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AnonymousChatComponent extends ListenerAdapter {

    private final JDA jda;

    private static final String COMMAND_NAME = "anonymchat";

    @PostConstruct
    public void initAnonymousChat() {
        jda.addEventListener(this);

        var command = Commands.slash(COMMAND_NAME, "Send an anonymous chat message to the current channel")
                .addOption(OptionType.STRING, "message", "The message you want to send anonymously", true)
                .setContexts(InteractionContextType.GUILD)
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL);

        jda.updateCommands().addCommands(command).queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equalsIgnoreCase(COMMAND_NAME)) {
            event.reply(":thumbsup:").setEphemeral(true).queue();
            event.getChannel().sendMessage(event.getInteraction().getOption("message").getAsString()).queue();
        }
    }
}
