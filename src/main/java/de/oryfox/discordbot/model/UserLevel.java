package de.oryfox.discordbot.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class UserLevel {
    @EmbeddedId
    private GuildMember guildMember;
    private Long xp;

    public UserLevel(Long userId, Long guildId) {
        this.guildMember = new GuildMember(userId, guildId);
        this.xp = 0L;
    }
}
