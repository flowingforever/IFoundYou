package pro.fazeclan.river.ifoundyou.listener;

import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import pro.fazeclan.river.ifoundyou.IFoundYou;
import pro.fazeclan.river.ifoundyou.event.AbilityEvent;
import pro.fazeclan.river.ifoundyou.util.RoleUtil;

public class AbilityListeners implements Listener {

    @EventHandler
    public void handleSwapHands(PlayerSwapHandItemsEvent event) {
        var player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (player.isSneaking()) {
            return;
        }
        var role = RoleUtil.getRole(player);
        if (role.isEmpty()) {
            return;
        }
        for (String abilityId : role.get().getAbilities()) {
            IFoundYou.getInstance().getServer().getPluginManager().callEvent(new AbilityEvent(
                    player,
                    abilityId
            ));
            if (!event.isCancelled()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void handleCrouchingSwapHands(PlayerSwapHandItemsEvent event) {
        var player = event.getPlayer();
        if (player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        if (!player.isSneaking()) {
            return;
        }
        var role = RoleUtil.getRole(player);
        if (role.isEmpty()) {
            return;
        }
        for (String abilityId : role.get().getAbilities()) {
            IFoundYou.getInstance().getServer().getPluginManager().callEvent(new AbilityEvent(
                    player,
                    abilityId
            ));
            if (!event.isCancelled()) {
                event.setCancelled(true);
            }
        }
    }

}
