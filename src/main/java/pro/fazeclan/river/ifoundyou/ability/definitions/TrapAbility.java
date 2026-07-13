package pro.fazeclan.river.ifoundyou.ability.definitions;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pro.fazeclan.river.ifoundyou.ability.Ability;
import pro.fazeclan.river.ifoundyou.event.AbilityEvent;
import pro.fazeclan.river.ifoundyou.event.FoundGameAddPlayer;
import pro.fazeclan.river.ifoundyou.event.FoundGameRemovePlayer;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.TimedCondition;
import pro.fazeclan.river.jarona.util.SchedulingUtil;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class TrapAbility extends Ability {

    public TrapAbility() { super("trap"); }

    private final List<Trap> TRAPS          = new CopyOnWriteArrayList<>();
    private volatile boolean scannersStarted = false;

    // Ensure proximity/cleanup scanners run once
    private void ensureScanners() {
        var radiusSquared = Math.pow(getDefaultAbilityProperty("radius", 3.25), 2);
        if (scannersStarted) return;
        synchronized (TrapAbility.class) {
            if (scannersStarted) return;
            scannersStarted = true;

            // Proximity scanner: apply effects while inside radius
            SchedulingUtil.interval(10L, 10L, () -> {
                if (TRAPS.isEmpty()) return;

                for (Trap trap : TRAPS) {
                    World w = trap.location.getWorld();
                    if (w == null) continue;

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        if (p.getWorld() != w) continue;
                        if (p.getLocation().distanceSquared(trap.location) > radiusSquared) continue;

                        if (isRunner(p)) {
                            p.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 2 * 20, 0, false, false, true));
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 2 * 20, 2, false, false, true));
                        } else if (isHunter(p)) {
                            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 3 * 20, 0, false, false, true));
                        }
                    }
                }
            });

            // Expiration & seeker-absence
            SchedulingUtil.interval(20L, 20L, () -> {
                long now = System.currentTimeMillis();

                // remove expired
                for (Trap t : TRAPS) {
                    if (now >= t.expiresAt) {
                        t.destroy();
                        TRAPS.remove(t);
                    }
                }

                // if no seekers remain, clear all traps
                if (noSeekersOnline() && !TRAPS.isEmpty()) {
                    for (Trap t : TRAPS) t.destroy();
                    TRAPS.clear();
                }
            });
        }
    }

    @EventHandler
    private void onActivate(AbilityEvent event) {
        if (!event.getExpectedAbility().equalsIgnoreCase(getId())) return;

        ensureScanners();

        var manager = Jarona.getInstance().getConditionManager();
        final Player seeker = event.getPlayer();

        final UUID id = seeker.getUniqueId();

        // Cooldown gate
        var condition = manager.getPlayerConditions(seeker).getOrCreate(
                getId() + "_ability",
                new TimedCondition(
                        TimedCondition.Type.GAME_TICK,
                        c -> null,
                        id
                )
        );

        if (!condition.getAvailable()) {
            return;
        }

        var cooldown = getDefaultAbilityProperty("cooldown", 60);
        var lifetimeSeconds = getDefaultAbilityProperty("lifetime", 130);

        condition.setHud(c -> {
            var tc = (TimedCondition) c;
            var duration = (tc.getDuration() / 20) + 1;
            if ((tc.getDuration() / 20.0) == 0.0) {
                return "<gold>⚓ <green>Ready!</green></gold>";
            } else {
                return "<gold>⚓ <red>" + duration + "s</red></gold>";
            }
        });
        condition.setDuration(cooldown * 20);

        // Place at block center, just above surface
        Location base = seeker.getLocation().toCenterLocation();
        base.setY(seeker.getLocation().getBlockY() + 0.01);

        Trap trap = new Trap(base, id, System.currentTimeMillis() + lifetimeSeconds * 1000L);
        TRAPS.add(trap);
        trap.startBeam();

        seeker.sendMessage(ChatColor.GOLD + "Trap placed. " + ChatColor.GRAY + "(Lasts " + lifetimeSeconds + "s)");
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent e) {
        var player = e.getPlayer();
        Jarona.getInstance().getConditionManager().getPlayerConditions(player).remove(getId() + "_ability");
    }

    @EventHandler
    private void handleGameJoin(FoundGameAddPlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        initializeAbilityCondition(
                event,
                Jarona.getInstance().getConditionManager(),
                c -> "<yellow>⚓ <green>Ready!</green></yellow>"
        );
    }

    @EventHandler
    private void handleGameLeave(FoundGameRemovePlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var player = event.getPlayer();
        // clear cooldowns and traps after the round ends
        Jarona.getInstance().getConditionManager().getPlayerConditions(player).remove(getId() + "_ability");
        TRAPS.removeIf(trap -> trap.owner.equals(player.getUniqueId()));
    }

    private boolean noSeekersOnline() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isHunter(p)) return false;
        }
        return true;
    }

    // Trap
    private class Trap {
        final Location location;
        final UUID owner;
        final long expiresAt;
        private Closeable beamTask;

        Trap(Location location, UUID owner, long expiresAt) {
            this.location = location.clone();
            this.owner = owner;
            this.expiresAt = expiresAt;
        }

        void startBeam() {
            Particle.DustOptions dust = new Particle.DustOptions(
                    Color.fromRGB(
                            getDefaultAbilityProperty("particle.color.red", 255),
                            getDefaultAbilityProperty("particle.color.green", 255),
                            getDefaultAbilityProperty("particle.color.blue", 255)
                    ),
                    getDefaultAbilityProperty("particle.size", 1.2).floatValue()
            );
            beamTask = SchedulingUtil.interval(0L, 5L, () -> { // redraw every 0.25s
                World w = location.getWorld();
                if (w == null) return;

                // Base point just above block surface
                Location base = location.clone().add(0, 0.10, 0);

                // Draw a straight orange line upward (~3.5 blocks high)
                final double height = 3.5;
                final double step   = 0.25;

                for (double dy = 0; dy <= height; dy += step) {
                    Location pLoc = base.clone().add(0, dy, 0);
                    w.spawnParticle(Particle.DUST, pLoc, 4, 0.02, 0.02, 0.02, 0, dust);
                }
            });
        }

        void destroy() {
            if (beamTask != null) {
                try {
                    beamTask.close();
                } catch (IOException ignored) {}
                beamTask = null;
            }
        }
    }

}
