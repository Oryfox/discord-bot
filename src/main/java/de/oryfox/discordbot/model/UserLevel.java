package de.oryfox.discordbot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class UserLevel {
    @Id
    private Long userId;
    private Long xp;

    public UserLevel(Long userId) {
        this.userId = userId;
        this.xp = 0L;
    }
}
