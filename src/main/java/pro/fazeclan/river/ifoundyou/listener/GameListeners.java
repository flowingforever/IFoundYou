package pro.fazeclan.river.ifoundyou.listener;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import pro.fazeclan.river.ifoundyou.util.RoleUtil;
import pro.fazeclan.river.jarona.util.GameUtil;

public class GameListeners implements Listener {

    @EventHandler
    public void handleEntityDamageEvent(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        if (!player.getWorld().getKey().namespace().equals("ifoundyou")) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void handlePlayerDropItem(PlayerDropItemEvent event) {
        if (!event.getPlayer().getWorld().getKey().namespace().equals("ifoundyou")) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void handlePlayerInteractFarmland(PlayerInteractEvent event) {
        var player = event.getPlayer();
        if (event.getAction() != Action.PHYSICAL) {
            return;
        }
        var block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        if (block.getType() != Material.FARMLAND) {
            return;
        }
        if (!player.getWorld().getKey().namespace().equals("ifoundyou")) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void handleFoodLevel(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!player.getWorld().getKey().namespace().equals("ifoundyou")) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void handlePlayerInteractChiseledBookshelf(PlayerInteractEvent event) {
        var player = event.getPlayer();
        if (!player.getWorld().getKey().namespace().equals("ifoundyou")) {
            return;
        }
        var block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        if (block.getType() != Material.CHISELED_BOOKSHELF) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void handlePlayerInteractPot(PlayerInteractEvent event) {
        var player = event.getPlayer();
        if (!player.getWorld().getKey().namespace().equals("ifoundyou")) {
            return;
        }
        var block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        if (block.getType() != Material.DECORATED_POT) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void handlePlayerDamage(EntityDamageEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!player.getWorld().getKey().namespace().equals("ifoundyou")) {
            return;
        }
        if (event.getFinalDamage() < player.getHealth()) {
            return;
        }
        player.setGameMode(GameMode.SPECTATOR);
        player.getWorld().playSound(
                player.getLocation(),
                "minecraft:block.beacon.deactivate",
                SoundCategory.PLAYERS,
                2f,
                0.5f
        );
        // todo: summon corpse
        event.setCancelled(true);
    }

    @EventHandler
    public void handlePlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        if (player.getWorld().getKey().namespace().equals("ifoundyou")) {
            return;
        }
        RoleUtil.removeRoles(player);
        GameUtil.resetPlayer(player, GameMode.ADVENTURE);
    }

}
