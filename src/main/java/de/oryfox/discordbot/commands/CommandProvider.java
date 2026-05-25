package de.oryfox.discordbot.commands;

import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.List;

public interface CommandProvider {
    List<SlashCommandData> getCommands();
}
