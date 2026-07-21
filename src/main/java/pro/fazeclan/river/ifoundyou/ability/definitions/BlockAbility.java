package pro.fazeclan.river.ifoundyou.ability.definitions;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
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

public class BlockAbility extends Ability {

    public BlockAbility() { super("block"); }

    // Config
    private static final long ACTIVE_WINDOW_MS = 3_000L;

    // Sounds
    private static final Sound SND_ACTIVATE_ALL = Sound.ENTITY_VILLAGER_WORK_TOOLSMITH; // everyone, pitch 1.5
    private static final Sound SND_HIT_DEFENDER = Sound.BLOCK_ANVIL_LAND;               // defender only, pitch 2
    private static final Sound SND_HIT_ATTACKER = Sound.BLOCK_AMETHYST_BLOCK_BREAK;     // attacker only, pitch 0.2
    private static final Sound SND_TIMEOUT_DEF   = Sound.ENTITY_PLAYER_BIG_FALL;        // defender only

    // State
    private static final Map<UUID, Long> ACTIVE_UNTIL = new ConcurrentHashMap<>();  // primed window end

    // Particles
    private static final Map<UUID, Closeable> PARTICLE_TASKS = new ConcurrentHashMap<>();

    // Activate: consume a charge, prime for 3s, start 35s cooldown
    @EventHandler
    private void handleAbility(AbilityEvent event) {
        if (!event.getExpectedAbility().equalsIgnoreCase(getId())) return;
        var conditionManager = Jarona.getInstance().getConditionManager();

        final Player defender = event.getPlayer();
        final UUID id = defender.getUniqueId();

        // Already primed?
        long now = System.currentTimeMillis();
        Long until = ACTIVE_UNTIL.get(id);
        if (until != null && until > now) {
            defender.sendMessage(ChatColor.YELLOW + "Block is already active.");
            return;
        }

        // Consume a charge and prime window
        ACTIVE_UNTIL.put(id, now + ACTIVE_WINDOW_MS);

        var cooldown = getDefaultAbilityProperty("cooldown", 35) * 20L;
        var maxUses = getDefaultAbilityProperty("uses", 2);

        var condition = conditionManager.getPlayerConditions(defender)
                .getOrCreate(
                        getId() + "_ability",
                        new TimedUseCondition(
                                TimedCondition.Type.GAME_TICK,
                                c -> null,
                                id,
                                maxUses
                        )
                );

        if (!condition.getAvailable()) return;

        condition.setDuration(cooldown);
        condition.increaseUses();

        condition.setHud(c -> {
            var tc = (TimedUseCondition) c;
            var duration = (tc.getDuration() / 20) + 1;
            if (tc.getUses() >= tc.getMaxUses()) {
                return "<yellow>\uD83D\uDEE1 <gray>Depleted.</gray></yellow> " + buildUses(tc.getMaxUses(), tc.getUses());
            } else if (ACTIVE_UNTIL.containsKey(id)) {
                return "<yellow>\uD83D\uDEE1 <yellow>In Use!</yellow></yellow> " + buildUses(tc.getMaxUses(), tc.getUses());
            } else if ((tc.getDuration() / 20.0) == 0.0) {
                return "<yellow>\uD83D\uDEE1 <green>Ready!</green></yellow> " + buildUses(tc.getMaxUses(), tc.getUses());
            } else {
                return "<yellow>\uD83D\uDEE1 <red>" + duration +"s</red></yellow> " + buildUses(tc.getMaxUses(), tc.getUses());
            }
        });

        // Visual & message: primed
        defender.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 3 * 20, 0, false, false, true));;
        defender.sendMessage(ChatColor.GREEN + "Block primed! " + ChatColor.GRAY + "(Parry for 3s.)");

        // Sound (everyone)
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), SND_ACTIVATE_ALL, 2.0f, 1.5f);
        }

        // Start particle aura while active
        var particleTask = SchedulingUtil.interval(0L, 5L, () -> {
            Long particleUntil = ACTIVE_UNTIL.get(id);
            if (particleUntil == null || particleUntil <= System.currentTimeMillis()) {
                return; // don’t spawn if no longer active
            }

            defender.getWorld().spawnParticle(
                    Particle.ENCHANTED_HIT,
                    defender.getLocation().add(0, 1, 0), // around chest area
                    15,
                    0.5, 0.8, 0.5,
                    0.05
            );
        });

        PARTICLE_TASKS.put(id, particleTask);

        // Timeout task → punish if not hit
        SchedulingUtil.runLater(3 * 20L, () -> {
            // If the token is still present, no hit occurred → punish and end
            if (ACTIVE_UNTIL.remove(id) != null) {
                defender.removePotionEffect(PotionEffectType.GLOWING);

                // Defender stops showing particles
                var task = PARTICLE_TASKS.remove(id);
                if (task != null) try { task.close(); } catch (IOException ignored) {}

                defender.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 5 * 20, 2, false, true, true)); // Slowness III
                defender.playSound(defender.getLocation(), SND_TIMEOUT_DEF, 1.0f, 1.0f);

                defender.sendMessage(ChatColor.RED + "Block failed! " + ChatColor.GRAY + "Maybe next time...");
            }
        });
    }

    // ===== If hit by a seeker while primed: block & debuff attacker =====
    @EventHandler
    private void onDamage(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        if (!(victim instanceof Player defender)) return;

        UUID id = defender.getUniqueId();
        Long until = ACTIVE_UNTIL.get(id);
        long now = System.currentTimeMillis();
        if (until == null || until <= now) return; // not active window

        Entity damager = event.getDamager();
        if (!(damager instanceof Player attacker)) return;
        if (!isHunter(attacker)) return;

        // Consume the window & block damage
        ACTIVE_UNTIL.remove(id);
        event.setCancelled(true);

        // Defender stops showing particles
        var task = PARTICLE_TASKS.remove(id);
        if (task != null) try { task.close(); } catch (IOException ignored) {}

        // Defender stops glowing
        defender.setGlowing(false);
        defender.removePotionEffect(PotionEffectType.GLOWING);

        // Debuff attacker
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 2 * 20, 0, false, true, true));
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 3 * 20, 4, false, true, true)); // Slowness V
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 2 * 20, 4, false, true, true));

        // Sounds update
        defender.playSound(defender.getLocation(), SND_HIT_DEFENDER, 1.0f, 2.0f);
        attacker.playSound(attacker.getLocation(), SND_HIT_ATTACKER, 1.0f, 0.2f);

        // Messages
        defender.sendMessage(ChatColor.AQUA + "Hit parried!");
        attacker.sendMessage(ChatColor.RED + "Your hit was parried!");
    }

    @EventHandler
    private void onQuit(PlayerQuitEvent e) {
        UUID id = e.getPlayer().getUniqueId();

        // Defender stops showing particles
        var task = PARTICLE_TASKS.remove(id);
        if (task != null) try { task.close(); } catch (IOException ignored) {}
    }

    @EventHandler
    private void handleGameAddPlayer(FoundGameAddPlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) {
            return;
        }

        var conditionManager = Jarona.getInstance().getConditionManager();
        int maxUses = getDefaultAbilityProperty("uses", 2);
        initializeAbilityUsesCondition(
                event,
                maxUses,
                conditionManager,
                c -> "<yellow>\uD83D\uDEE1 <green>Ready!</green></yellow> " + buildUses(maxUses, 0)
        );
    }

    @EventHandler
    private void handleGameRemovePlayer(FoundGameRemovePlayer event) {
        var conditionManager = Jarona.getInstance().getConditionManager();
        conditionManager.getPlayerConditions(event.getPlayer())
                .remove(getId() + "_ability");
    }

}
