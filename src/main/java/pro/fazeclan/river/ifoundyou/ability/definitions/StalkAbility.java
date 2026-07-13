package pro.fazeclan.river.ifoundyou.ability.definitions;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pro.fazeclan.river.ifoundyou.ability.Ability;
import pro.fazeclan.river.ifoundyou.event.FoundGameAddPlayer;
import pro.fazeclan.river.ifoundyou.event.FoundGameRemovePlayer;
import pro.fazeclan.river.jarona.util.SchedulingUtil;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StalkAbility extends Ability {

    public StalkAbility() { super("stalk"); }

    private final Map<UUID, Long> lastStepAt = new ConcurrentHashMap<>();
    private final Map<UUID, Closeable> gameTasks = new ConcurrentHashMap<>();

    @EventHandler
    private void handleGameAddPlayer(FoundGameAddPlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var player = event.getPlayer();
        gameTasks.put(player.getUniqueId(), SchedulingUtil.interval(20L, 20L, () -> {
            setSeekerGlowAccordingToSneak(player);
        }));
    }

    @EventHandler
    private void handleGameRemovePlayer(FoundGameRemovePlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        try {
            gameTasks.remove(event.getPlayer().getUniqueId()).close();
        } catch (IOException ignored) {}
    }

    @EventHandler
    private void handleJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        if (hasAbility(player)) {
            setSeekerGlowAccordingToSneak(player);
        }
    }

    @EventHandler
    private void handleQuit(PlayerQuitEvent event) {
        lastStepAt.remove(event.getPlayer().getUniqueId());
    }

    // Footsteps: when a Hider moves, draw prints visible only to seekers with this ability
    @EventHandler
    private void onPlayerMove(PlayerMoveEvent event) {
        if (!event.hasChangedPosition()) return;

        Player mover = event.getPlayer();
        if (!isRunner(mover)) return;
        if (mover.getGameMode().isInvulnerable()) return;

        // ignore tiny moves
        var min_move_dist_sq = Math.pow(getDefaultAbilityProperty("minimum-movement", 0.2), 2);
        if (event.getFrom().distanceSquared(event.getTo()) < min_move_dist_sq) return;

        long step_cooldown = (long) (getDefaultAbilityProperty("cooldown", 0.2) * 1000L);

        long now = System.currentTimeMillis();
        UUID id = mover.getUniqueId();
        long last = lastStepAt.getOrDefault(id, 0L);
        if (now - last < step_cooldown) return;
        lastStepAt.put(id, now);

        Location stepLoc = mover.getLocation().clone().add(0, 0.05, 0);
        World w = stepLoc.getWorld();
        if (w == null) return;

        var foot_dust = new Particle.DustOptions(Color.fromRGB(
                getDefaultAbilityProperty("particle.red", 220),
                getDefaultAbilityProperty("particle.green", 220),
                getDefaultAbilityProperty("particle.blue", 220)
        ), getDefaultAbilityProperty("particle.size", 1.0).floatValue());
        var view_range = Math.pow(getDefaultAbilityProperty("view-range", 24.0), 2);

        // Show to seekers that have this ability, and are reasonably close
        for (Player viewer : w.getPlayers()) {
            if (viewer == mover) continue;
            if (!hasAbility(viewer)) continue;
            if (viewer.getLocation().distanceSquared(stepLoc) > view_range) continue;
            if (viewer.getGameMode().isInvulnerable()) continue;

            // Footprint particles?
            viewer.spawnParticle(Particle.DUST, stepLoc.clone().add(0.15, 0, 0.1), 3, 0.02, 0.0, 0.02, 0, foot_dust);
            viewer.spawnParticle(Particle.DUST, stepLoc.clone().add(-0.15, 0, -0.1), 3, 0.02, 0.0, 0.02, 0, foot_dust);
        }
    }

    private void setSeekerGlowAccordingToSneak(Player seeker) {
        if (!hasAbility(seeker)) return;
        if (seeker.getGameMode().equals(GameMode.SPECTATOR)) {
            seeker.removePotionEffect(PotionEffectType.GLOWING);
            return;
        }

        if (seeker.isSneaking()) {
            seeker.removePotionEffect(PotionEffectType.GLOWING);
        } else {
            seeker.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, PotionEffect.INFINITE_DURATION, 0, false, false, true));
        }
    }

}
