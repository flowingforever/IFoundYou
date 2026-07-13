package pro.fazeclan.river.ifoundyou.ability.definitions;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
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

import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

public class EnragedShriekAbility extends Ability {

    public EnragedShriekAbility() {
        super("shriek");
    }

    private final Map<UUID, Closeable> CHARGES = new HashMap<>();

    @EventHandler
    private void handleAbility(AbilityEvent event) {
        if (!event.getExpectedAbility().equalsIgnoreCase(getId())) return;

        var minimessage = MiniMessage.miniMessage();
        var player = event.getPlayer();

        if (CHARGES.containsKey(player.getUniqueId())) {
            player.sendMessage(minimessage.deserialize("<red>Enraged Shriek is already charging!</red>"));
            return;
        }

        var manager = Jarona.getInstance().getConditionManager();

        var condition = manager.getPlayerConditions(player)
                .getOrCreate(
                        getId() + "_ability",
                        new TimedCondition(
                                TimedCondition.Type.GAME_TICK,
                                c -> null
                        )
                );

        if (!condition.getAvailable()) {
            return;
        }

        int cooldown = getDefaultAbilityProperty("cooldown", 35);

        condition.setHud(c -> {
            var tc = (TimedCondition) c;
            var duration = (tc.getDuration() / 20) + 1;
            if ((tc.getDuration() / 20.0) == 0.0) {
                return "<aqua>☄ <green>Ready!</green></aqua>";
            } else {
                return "<aqua>☄ <red>" + duration + "s</red></aqua>";
            }
        });

        var charge = getDefaultAbilityProperty("charge", 3);

        // Make all hiders glow for 3s
        for (Player hider : player.getWorld().getPlayers()) {
            if (isRunner(hider)) return;

            hider.showTitle(Title.title(
                    miniMessage().deserialize(""),
                    miniMessage().deserialize("<red>BOOM INCOMING!")
            ));
            hider.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, charge * 20, 0, false, false, true));
        }

        // Title + progress: cyan
        sendChargeFrame(player, 0);

        player.playSound(player.getLocation(), Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 3.0f, 1.0f);

        // Charge task: update squares once per second, then fire
        final int[] tick = {0};
        var task = SchedulingUtil.interval(20, 20, () -> {
            tick[0]++;

            if (tick[0] < charge) {
                sendChargeFrame(player, tick[0]); // 1 and 2
                return;
            }

            // Final frame (3/3), then fire
            sendChargeFrame(player, charge);
            try {
                CHARGES.remove(player.getUniqueId()).close();
            } catch (IOException ignored) {}

            // Fire the sonic shriek
            fireShriek(player);

            // Start cooldown now
            condition.setDuration(cooldown * 20L);
        });

        CHARGES.put(player.getUniqueId(), task);
    }

    private void fireShriek(Player user) {
        Location eye = user.getEyeLocation();
        Vector dir = eye.getDirection().normalize();

        World w = user.getWorld();

        // Visual: a short sonic trail with particles
        w.playSound(eye, Sound.ENTITY_WARDEN_SONIC_BOOM, 2.0f, 1.0f);

        double max = getDefaultAbilityProperty("range", 15);
        for (double d = 0; d <= max; d += getDefaultAbilityProperty("step", 0.5)) {
            Location point = eye.clone().add(dir.clone().multiply(d));

            // Particles along the path
            w.spawnParticle(Particle.SONIC_BOOM, point, 1, 0, 0, 0, 0);
            w.spawnParticle(Particle.WAX_OFF, point, 2, 0.05, 0.05, 0.05, 0.0);

            // Check for a hider hit near this point
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (target == user) continue;
                if (target.getWorld() != w) continue;
                if (!isRunner(target)) continue;

                if (point.distanceSquared(target.getLocation().add(0, 1.0, 0)) <= getDefaultAbilityProperty("hit-radius", 1.0)) {
                    // Apply 7 hearts (14 HP) and Weakness II for 10s
                    target.damage(14.0, user);
                    target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 10 * 20, 1, false, false, true));

                    // Impact flare
                    w.spawnParticle(Particle.SONIC_BOOM, target.getLocation().add(0, 1.0, 0), 10, 0.2, 0.2, 0.2, 0.01);
                    w.playSound(target.getLocation(), Sound.ENTITY_SKELETON_HORSE_DEATH, 1.0f, 0.6f);
                    return; // stop after first hit
                }
            }
        }
    }

    private void sendChargeFrame(Player user, int filled) {
        // 0..3; build square progress using cyan blocks
        String[] boxes = new String[] {"⬛", "⬛", "⬛"};
        for (int i = 0; i < filled && i < 3; i++) boxes[i] = "🟦";

        String bar = ChatColor.DARK_GRAY + "[ "
                + ChatColor.DARK_AQUA + boxes[0] + " "
                + ChatColor.DARK_AQUA + boxes[1] + " "
                + ChatColor.DARK_AQUA + boxes[2]
                + ChatColor.DARK_GRAY + " ]";

        user.sendTitle(ChatColor.AQUA + "", bar, 0, 25, 5);
    }

    @EventHandler
    private void handleGamePlayerAdd(FoundGameAddPlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var manager = Jarona.getInstance().getConditionManager();
        initializeAbilityCondition(event, manager, c -> "<aqua>☄ <green>Ready!</green></aqua>");
    }

    @EventHandler
    private void handleGamePlayerRemoval(FoundGameRemovePlayer event) {
        var manager = Jarona.getInstance().getConditionManager();
        manager.getPlayerConditions(event.getPlayer()).remove(getId() + "_ability");
    }

}
