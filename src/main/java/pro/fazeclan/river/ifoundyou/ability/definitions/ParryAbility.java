package pro.fazeclan.river.ifoundyou.ability.definitions;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import pro.fazeclan.river.ifoundyou.ability.Ability;
import pro.fazeclan.river.ifoundyou.event.AbilityEvent;
import pro.fazeclan.river.ifoundyou.event.FoundGameAddPlayer;
import pro.fazeclan.river.ifoundyou.event.FoundGameRemovePlayer;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.TimedCondition;
import pro.fazeclan.river.jarona.condition.TimedUseCondition;
import pro.fazeclan.river.jarona.util.SchedulingUtil;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ParryAbility extends Ability {

    public ParryAbility() { super("parry"); }

    // State
    private final Map<UUID, Long> ACTIVE_UNTIL = new ConcurrentHashMap<>();  // primed window end

    // Particles
    private final Map<UUID, Closeable> PARTICLE_TASKS = new ConcurrentHashMap<>();

    @EventHandler
    private void handleAbility(AbilityEvent event) {
        if (!event.getExpectedAbility().equalsIgnoreCase(getId())) return;

        var manager = Jarona.getInstance().getConditionManager();
        var player = event.getPlayer();
        int maxUses = getDefaultAbilityProperty("uses", 2);

        var condition = manager.getPlayerConditions(player)
                .getOrCreate(
                        getId() + "_ability",
                        new TimedUseCondition(
                                TimedCondition.Type.GAME_TICK,
                                c -> null,
                                maxUses
                        )
                );

        if (!condition.getAvailable()) return;

        // Already primed?
        long now = System.currentTimeMillis();
        Long until = ACTIVE_UNTIL.get(player.getUniqueId());
        if (until != null && until > now) {
            player.sendMessage(ChatColor.YELLOW + "Parry is already active.");
            return;
        }

        int cooldown = getDefaultAbilityProperty("cooldown", 35);
        int window = getDefaultAbilityProperty("active-window", 3);

        ACTIVE_UNTIL.put(player.getUniqueId(), now + (window * 1000L));

        // HUD updates
        condition.setHud(c -> {
            var tc = (TimedUseCondition) c;
            var duration = (tc.getDuration() / 20) + 1;
            if (tc.getUses() >= tc.getMaxUses()) {
                return "<green>\uD83D\uDEE1 <gray>Depleted.</gray></green> " + buildUses(tc.getMaxUses(), tc.getUses());
            } else if ((tc.getDuration() / 20.0) == 0.0) {
                return "<green>\uD83D\uDEE1 <green>Ready!</green></green> " + buildUses(tc.getMaxUses(), tc.getUses());
            } else {
                return "<green>\uD83D\uDEE1 <red>" + duration +"s</red></green> " + buildUses(tc.getMaxUses(), tc.getUses());
            }
        });
        condition.setDuration((cooldown + window) * 20L);
        condition.increaseUses();

        // Visual & message: primed
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 3 * 20, 0, false, false, true));;
        player.sendMessage(ChatColor.GREEN + "Parry primed! " + ChatColor.GRAY + "(Parry for 3s.)");

        // Sound (everyone)
        player.getWorld().playSound(
                player.getLocation(),
                getDefaultAbilityProperty("sound.window", "minecraft:entity.villager.work_toolsmith"),
                2.0f,
                1.5f
        );

        // Start particle aura while active

        Closeable particleTask = SchedulingUtil.interval(0L, 5L, () -> {
            Long particleUntil = ACTIVE_UNTIL.get(player.getUniqueId());
            if (particleUntil == null || particleUntil <= System.currentTimeMillis()) {
                return; // don’t spawn if no longer active
            }

            player.getWorld().spawnParticle(
                    Particle.ENCHANTED_HIT,
                    player.getLocation().add(0, 1, 0), // around chest area
                    15,
                    0.5, 0.8, 0.5,
                    0.05
            );
        });

        PARTICLE_TASKS.put(player.getUniqueId(), particleTask);

        // Timeout task → punish if not hit
        SchedulingUtil.runLater(3 * 20L, () -> {
            // If the token is still present, no hit occurred → punish and end
            if (ACTIVE_UNTIL.remove(player.getUniqueId()) != null) {
                // stop particles
                Closeable t = PARTICLE_TASKS.remove(player.getUniqueId());
                if (t != null) {
                    try {
                        t.close();
                    } catch (IOException ignored) {}
                }

                player.removePotionEffect(PotionEffectType.GLOWING);

                // Defender stops showing particles
                Closeable task = PARTICLE_TASKS.remove(player.getUniqueId());
                if (task != null) {
                    try {
                        task.close();
                    } catch (IOException ignored) {}
                }

                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 5 * 20, 2, false, true, true)); // Slowness III
                player.playSound(
                        player.getLocation(),
                        getDefaultAbilityProperty("sound.miss", "minecraft:entity.player.big_fall"),
                        1.0f,
                        1.0f
                );

                player.sendMessage(ChatColor.RED + "Block failed! " + ChatColor.GRAY + "Maybe next time...");
            }
        });
    }

    // ===== If hit by a seeker while primed: block & debuff attacker =====
    @EventHandler
    private void handleDamage(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        if (!(victim instanceof Player defender)) return;

        UUID id = defender.getUniqueId();
        Long until = ACTIVE_UNTIL.get(id);
        long now = System.currentTimeMillis();
        if (until == null || until <= now) return; // not active window

        Entity damager = event.getDamager();
        if (!(damager instanceof Player attacker)) return;

        // Consume the window & block damage
        ACTIVE_UNTIL.remove(id);
        event.setCancelled(true);

        // Defender stops showing particles
        Closeable task = PARTICLE_TASKS.remove(id);
        if (task != null) {
            try {
                task.close();
            } catch (IOException ignored) {}
        }

        // Defender stops glowing
        defender.setGlowing(false);
        defender.removePotionEffect(PotionEffectType.GLOWING);

        // Debuff attacker
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 2 * 20, 0, false, false, true));
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 3 * 20, 4, false, false, true)); // Slowness V
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 2 * 20, 4, false, false, true));

        // Sounds update
        defender.playSound(
                defender.getLocation(),
                getDefaultAbilityProperty("sound.hit.defender", "minecraft:block.anvil.land"),
                1.0f,
                2.0f
        );
        attacker.playSound(
                attacker.getLocation(),
                getDefaultAbilityProperty("sound.hit.attacker", "minecraft:block.amethyst_block.break"),
                1.0f,
                0.2f
        );

        // Messages
        defender.sendMessage(ChatColor.AQUA + "Hit parried!");
        attacker.sendMessage(ChatColor.RED + "Your hit was parried!");
    }

    @EventHandler
    private void handleGamePlayerAdd(FoundGameAddPlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var manager = Jarona.getInstance().getConditionManager();
        int maxUses = getDefaultAbilityProperty("uses", 2);
        initializeAbilityUsesCondition(
                event,
                maxUses,
                manager,
                c -> "<green>\uD83D\uDEE1 <green>Ready!</green></green> " + buildUses(maxUses, 0)
        );
    }

    @EventHandler
    private void handleGamePlayerRemove(FoundGameRemovePlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var manager = Jarona.getInstance().getConditionManager();
        manager.getPlayerConditions(event.getPlayer()).remove(getId() + "_ability");
    }

}
