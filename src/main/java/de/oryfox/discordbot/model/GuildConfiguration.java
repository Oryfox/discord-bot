package de.oryfox.discordbot.model;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class GuildConfiguration {
    @Id
    private Long guildId;

    private boolean anonymousChatEnabled;

    private boolean countingEnabled;
    private Long countingChannel;
    private Long countingLast;

    private boolean levelingEnabled;

    private boolean loggingEnabled;
    private Long loggingChannel;

    private boolean temporaryVoiceChannelEnabled;
    private Long temporaryVoiceChannelChannel;

    private boolean ticketSystemEnabled;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<Long> temporaryVoiceChannels;

    public GuildConfiguration(Long guildId) {
        this.guildId = guildId;
    }
}
