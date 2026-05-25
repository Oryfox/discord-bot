package de.oryfox.discordbot.commands;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommandRegistry {

    private final JDA jda;
    private final List<CommandProvider> commandProviders;

    @PostConstruct
    public void registerCommands() {
        var listOfCommands = commandProviders.stream().flatMap(c -> c.getCommands().stream()).toList();
        log.info("Found {} command providers and registering a total of {} commands", commandProviders.size(), listOfCommands.size());
        jda.updateCommands().addCommands(listOfCommands).queue();
    }
}
