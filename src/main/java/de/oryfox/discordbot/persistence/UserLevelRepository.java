package de.oryfox.discordbot.persistence;

import de.oryfox.discordbot.model.GuildMember;
import de.oryfox.discordbot.model.UserLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserLevelRepository extends JpaRepository<UserLevel, GuildMember> {
}
