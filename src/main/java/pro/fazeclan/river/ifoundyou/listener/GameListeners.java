package pro.fazeclan.river.ifoundyou.listener;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.configuration.file.YamlConfiguration;
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
import org.bukkit.persistence.PersistentDataType;
import pro.fazeclan.river.ifoundyou.IFoundYou;
import pro.fazeclan.river.ifoundyou.util.RoleUtil;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.TimedCondition;
import pro.fazeclan.river.jarona.util.GameUtil;

import java.io.File;
import java.util.UUID;

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
        var world = player.getWorld();
        var config = YamlConfiguration.loadConfiguration(new File(world.getWorldFolder(), "map_config.yml"));
        var manager = Jarona.getInstance().getConditionManager();
        var gameUUID = UUID.fromString(world.getKey().getKey());
        player.setGameMode(GameMode.SPECTATOR);
        world.playSound(
                player.getLocation(),
                "minecraft:block.beacon.deactivate",
                SoundCategory.PLAYERS,
                2f,
                0.5f
        );
        // todo: summon corpse

        // add more time when player dies
        var condition = manager.getGameConditions(gameUUID)
                        .getOrCreate(
                                "game_" + gameUUID,
                                new TimedCondition(
                                        TimedCondition.Type.GAME_TICK
                                )
                        );
        var time = config.getInt("additional-time", 900);
        condition.setDuration(condition.getDuration() + time);

        world.getPersistentDataContainer().set(
                IFoundYou.getKey("game_length"),
                PersistentDataType.INTEGER,
                world.getPersistentDataContainer().getOrDefault(
                        IFoundYou.getKey("game_length"),
                        PersistentDataType.INTEGER,
                        900
                ) + time
        );
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
