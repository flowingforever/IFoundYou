package pro.fazeclan.river.ifoundyou.event;

import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import pro.fazeclan.river.ifoundyou.role.Role;

public class FoundGameRemovePlayer extends PlayerEvent {

    @Getter
    private final Role role;

    public FoundGameRemovePlayer(@NotNull Player player, Role role) {
        super(player);
        this.role = role;
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
