package pro.fazeclan.river.ifoundyou.ability.definitions;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TerrifyAbility extends Ability {

    public TerrifyAbility() { super("terrify"); }

    private final Map<UUID, Closeable> terrifyTasks = new HashMap<>();

    @EventHandler
    private void handleActivate(AbilityEvent event) {
        if (!event.getExpectedAbility().equalsIgnoreCase(getId())) return;

        var player = event.getPlayer();
        var manager = Jarona.getInstance().getConditionManager();

        var condition = manager.getPlayerConditions(player)
                .getOrCreate(
                        getId() + "_ability",
                        new TimedCondition(
                                TimedCondition.Type.GAME_TICK,
                                c  -> null
                        )
                );

        if (!condition.getAvailable()) return;

        int cooldown = getDefaultAbilityProperty("cooldown", 75);

        condition.setHud(c -> {
            var tc = (TimedCondition) c;
            var duration = (tc.getDuration() / 20) + 1;
            if ((tc.getDuration() / 20.0) == 0.0) {
                return "<green>☽ <green>Ready!</green></green>";
            } else {
                return "<green>☽ <red>" + duration +"s</red></green>";
            }
        });
        condition.setDuration(cooldown * 20L);

        // Reveal all hiders (glow 3s), darkness to everyone 5s, speed I to user 10s
        final int GLOW_TICKS = getDefaultAbilityProperty("glow", 3) * 20;
        final int DARK_ALL_TICKS = getDefaultAbilityProperty("darkness", 5) * 20;
        final int SPEED_TICKS = getDefaultAbilityProperty("speed", 10) * 20;

        for (Player victim : player.getWorld().getPlayers()) {
            // Darkness to everyone (including user)
            victim.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, DARK_ALL_TICKS, 0, false, false, true));

            // Glow only hiders
            if (isRunner(victim)) {
                victim.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, GLOW_TICKS, 0, false, false, true));
            }
        }

        // Speed I to the user
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, SPEED_TICKS, 0, false, false, true));
    }

    @EventHandler
    private void handleGameAddPlayer(FoundGameAddPlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var player = event.getPlayer();
        var manager = Jarona.getInstance().getConditionManager();

        terrifyTasks.put(player.getUniqueId(), SchedulingUtil.interval(20L, 20L, () -> tickAura(player)));

        initializeAbilityCondition(event, manager, c -> "<green>☽ <green>Ready!</green></green>");
    }

    @EventHandler
    private void handleGameRemovePlayer(FoundGameRemovePlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var player = event.getPlayer();
        Jarona.getInstance().getConditionManager()
                .getPlayerConditions(player)
                .remove(getId() + "_ability");
        try {
            terrifyTasks.remove(player.getUniqueId()).close();
        } catch (IOException ignored) {}
    }

    private void tickAura(Player user) {
        // Apply Darkness 2s to nearby hiders within 5 blocks
        var aura_radius_sq = Math.pow(getDefaultAbilityProperty("aura-radius", 5.0), 2);
        var aura_darkness_ticks = getDefaultAbilityProperty("aura-darkness", 2) * 20;

        if (!hasAbility(user)) return;

        var seekerLoc = user.getLocation();

        for (Player targetPlayer : user.getWorld().getPlayers()) {
            Player target = targetPlayer.getPlayer();
            if (!isRunner(target)) continue;

            if (seekerLoc.distanceSquared(target.getLocation()) <= aura_radius_sq) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, aura_darkness_ticks, 0, false, true, true));
            }
        }
    }

}
