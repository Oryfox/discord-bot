package de.oryfox.discordbot.model;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;

import java.util.List;

public interface FeatureConfigurator {
    String getFeatureName();
    List<Command.Choice> getActions();
    void execute(SlashCommandInteractionEvent event);
    default List<Command.Choice> autoCompleteAction(String currentValue) {
        return getActions().stream()
                .filter(action -> action.getName().startsWith(currentValue))
                .toList();
    }
}
