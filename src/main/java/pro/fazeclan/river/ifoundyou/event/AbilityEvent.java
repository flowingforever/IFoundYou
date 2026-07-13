package pro.fazeclan.river.ifoundyou.event;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class AbilityEvent extends PlayerEvent {

    @Getter
    private final String expectedAbility;

    public AbilityEvent(@NotNull Player player, String expectedAbility) {
        super(player);
        this.expectedAbility = expectedAbility;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    private static HandlerList HANDLERS = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
