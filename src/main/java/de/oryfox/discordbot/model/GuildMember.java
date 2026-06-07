package de.oryfox.discordbot.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.dv8tion.jda.api.entities.Member;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GuildMember {
    private Long userId;
    private Long guildId;

    public GuildMember(Member member) {
        this.userId = member.getUser().getIdLong();
        this.guildId = member.getGuild().getIdLong();
    }
}
