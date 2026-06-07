package de.oryfox.discordbot.persistence;

import de.oryfox.discordbot.model.GuildConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuildConfigurationRepository extends JpaRepository<GuildConfiguration, Long> {
}
