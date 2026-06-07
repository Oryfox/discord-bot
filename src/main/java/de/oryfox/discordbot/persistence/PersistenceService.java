package de.oryfox.discordbot.persistence;

import de.oryfox.discordbot.model.GuildConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PersistenceService {

    private final GuildConfigurationRepository configurationRepository;

    private final Map<Long, GuildConfiguration> cache = new HashMap<>();

    public GuildConfiguration getConfiguration(Long guildId) {
        if (cache.containsKey(guildId)) {
            return cache.get(guildId);
        } else {
            var config = configurationRepository.findById(guildId);
            if (config.isPresent()) {
                cache.put(guildId, config.get());
                return config.get();
            } else {
                return new GuildConfiguration(guildId);
            }
        }
    }

    public void persist(GuildConfiguration guildConfiguration) {
        cache.put(guildConfiguration.getGuildId(), guildConfiguration);
        configurationRepository.save(guildConfiguration);
    }
}
