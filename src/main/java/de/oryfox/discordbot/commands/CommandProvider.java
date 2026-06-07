package de.oryfox.discordbot.commands;

import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.util.List;

public interface CommandProvider {
    List<CommandData> getCommands();
}
