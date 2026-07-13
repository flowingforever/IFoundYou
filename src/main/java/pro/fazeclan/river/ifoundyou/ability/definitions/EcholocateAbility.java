package pro.fazeclan.river.ifoundyou.ability.definitions;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pro.fazeclan.river.ifoundyou.ability.Ability;
import pro.fazeclan.river.ifoundyou.event.FoundGameAddPlayer;
import pro.fazeclan.river.ifoundyou.event.FoundGameRemovePlayer;
import pro.fazeclan.river.jarona.util.SchedulingUtil;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EcholocateAbility extends Ability {

    // player to task map
    private final Map<UUID, Closeable> echoTasks = new HashMap<>();

    private final Map<UUID, Long> LAST_PING_AT = new ConcurrentHashMap<>();

    public EcholocateAbility() {
        super("echolocate");
    }

    @EventHandler
    private void handleGameStart(FoundGameAddPlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var player = event.getPlayer();
        double r2 = Math.pow(getDefaultAbilityProperty("radius", 25.0), 2);

        echoTasks.put(player.getUniqueId(), SchedulingUtil.interval(10L, 10L, () -> {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 0, false, false, true));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 1, false, false, true));
            player.removePotionEffect(PotionEffectType.GLOWING);
            player.setGlowing(false);

            Location hl = player.getLocation();
            for (Player victim : player.getWorld().getPlayers()) {
                if (victim.getGameMode().isInvulnerable()) continue;
                if (victim == player) continue;
                if (hl.distanceSquared(victim.getLocation()) > r2) continue;
                victim.addPotionEffect(new PotionEffect(
                        PotionEffectType.DARKNESS,
                        40, 0, false, false, true
                ));
            }
        }));
    }

    @EventHandler
    private void handlePlayerRemoval(FoundGameRemovePlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        try {
            echoTasks.remove(event.getPlayer().getUniqueId()).close();
        } catch (IOException ignored) {}
    }

    @EventHandler
    private void handlePlayerMovement(PlayerMoveEvent event) {
        var mover = event.getPlayer();
        if (mover.isSneaking()) return;
        if (mover.getGameMode().isInvulnerable()) return;
        if (event.getFrom().distanceSquared(event.getTo()) < 0.01) return; // tiny movement

        // Only allow Hiders to trigger echolocation
        if (!isRunner(mover)) return;

        // Muffled by wool/carpet
        if (isSoftBlockUnder(mover)) return;

        double r2 = Math.pow(getDefaultAbilityProperty("radius", 25.0), 2);
        long cooldown = getDefaultAbilityProperty("cooldown", 2) * 1000L;

        // NEW: Per-mover cooldown (2s)
        long now = System.currentTimeMillis();
        long last = LAST_PING_AT.getOrDefault(mover.getUniqueId(), 0L);
        if (now - last < cooldown) return;  // still on cooldown
        LAST_PING_AT.put(mover.getUniqueId(), now);

        for (Player holder : Bukkit.getOnlinePlayers()) {
            if (holder.getGameMode().equals(GameMode.SPECTATOR)) continue;
            if (!hasAbility(holder)) continue;
            if (holder.getWorld() != mover.getWorld()) continue;
            if (holder.getLocation().distanceSquared(mover.getLocation()) > r2) continue;

            // Sculk clicking at mover’s location
            mover.getWorld().playSound(mover.getLocation(), Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 1.0f);

            // Vibration particle to the holder
            sendEcholocationPing(holder, mover);
        }
    }

    private void sendEcholocationPing(Player holder, Player mover) {
        Location start = holder.getLocation().add(0, 1.0, 0);
        Location end   = mover.getLocation().add(0, 1.0, 0);

        int travelTicks = Math.min(40, Math.max(10, (int) (start.distance(end) * 2)));

        Vibration vib = new Vibration(
                start,
                new Vibration.Destination.EntityDestination(mover),
                travelTicks
        );

        holder.spawnParticle(Particle.VIBRATION, start, 1, vib);
    }

    private boolean isSoftBlockUnder(Player p) {
        Material m = p.getLocation().subtract(0, 0.1, 0).getBlock().getType();
        return isWool(m) || isCarpet(m);
    }

    private boolean isWool(Material m) {
        return switch (m) {
            case WHITE_WOOL, ORANGE_WOOL, MAGENTA_WOOL, LIGHT_BLUE_WOOL, YELLOW_WOOL, LIME_WOOL,
                 PINK_WOOL, GRAY_WOOL, LIGHT_GRAY_WOOL, CYAN_WOOL, PURPLE_WOOL, BLUE_WOOL,
                 BROWN_WOOL, GREEN_WOOL, RED_WOOL, BLACK_WOOL -> true;
            default -> false;
        };
    }

    private boolean isCarpet(Material m) {
        return switch (m) {
            case WHITE_CARPET, ORANGE_CARPET, MAGENTA_CARPET, LIGHT_BLUE_CARPET, YELLOW_CARPET, LIME_CARPET,
                 PINK_CARPET, GRAY_CARPET, LIGHT_GRAY_CARPET, CYAN_CARPET, PURPLE_CARPET, BLUE_CARPET,
                 BROWN_CARPET, GREEN_CARPET, RED_CARPET, BLACK_CARPET -> true;
            default -> false;
        };
    }

}
