package de.oryfox.discordbot.features;

import de.oryfox.discordbot.commands.CommandProvider;
import de.oryfox.discordbot.model.FeatureConfigurator;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
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
public class ConfigurationFeature extends ListenerAdapter implements CommandProvider {

    private final JDA jda;
    private final List<FeatureConfigurator> features;

    private static final String COMMAND_NAME = "configure";
    private static final String SUBCOMMAND_FEATURE = "feature";
    private static final String SUBCOMMAND_ACTION = "action";

    @PostConstruct
    public void initConfiguration() {
        jda.addEventListener(this);
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (!event.getName().equalsIgnoreCase(COMMAND_NAME)) {
            return;
        }
        switch (event.getFocusedOption().getName()) {
            case SUBCOMMAND_FEATURE -> event.replyChoices(features.stream().sorted((a, b) -> a.getFeatureName().compareToIgnoreCase(b.getFeatureName())).map(f -> new Command.Choice(f.getFeatureName(), f.getFeatureName())).toList()).queue();
            case SUBCOMMAND_ACTION -> {
                for (var feature : features) {
                    if (feature.getFeatureName().equalsIgnoreCase(event.getOption(SUBCOMMAND_FEATURE).getAsString())) {
                        event.replyChoices(feature.autoCompleteAction(event.getFocusedOption().getValue())).queue();
                        return;
                    }
                }
            }
            default -> log.warn("Unknown command option requested auto complete: {}", event.getFocusedOption().getName());
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (!event.getName().equalsIgnoreCase(COMMAND_NAME)) {
            return;
        }

        for (var feature : features) {
            if (feature.getFeatureName().equalsIgnoreCase(event.getOption(SUBCOMMAND_FEATURE).getAsString())) {
                feature.execute(event);
                return;
            }
        }
    }

    @Override
    public List<CommandData> getCommands() {
        return List.of(
                Commands.slash(COMMAND_NAME, "Configuration of the bot.")
                        .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                        .setContexts(InteractionContextType.GUILD)
                        .addOption(OptionType.STRING, SUBCOMMAND_FEATURE, "The feature to configure", true, true)
                        .addOption(OptionType.STRING, SUBCOMMAND_ACTION, "The action to execute", true, true)
                        .addOption(OptionType.CHANNEL, "channel", "A channel option for configuration", false, false)
        );
    }
}
