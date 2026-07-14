package pro.fazeclan.river.ifoundyou.ability.definitions;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pro.fazeclan.river.ifoundyou.ability.Ability;
import pro.fazeclan.river.ifoundyou.event.AbilityEvent;
import pro.fazeclan.river.ifoundyou.event.FoundGameAddPlayer;
import pro.fazeclan.river.ifoundyou.event.FoundGameRemovePlayer;
import pro.fazeclan.river.ifoundyou.util.RoleUtil;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.TimedCondition;
import pro.fazeclan.river.jarona.util.SchedulingUtil;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NoisemakerAbility extends Ability {

    public NoisemakerAbility() { super("noise"); }

    private final Map<UUID, Closeable> rings = new HashMap<>();

    @EventHandler
    private void handleAbility(AbilityEvent event) {
        if (!event.getExpectedAbility().equalsIgnoreCase(getId())) return;

        var manager = Jarona.getInstance().getConditionManager();
        var player = event.getPlayer();

        var condition = manager.getPlayerConditions(player)
                .getOrCreate(
                        getId() + "_ability",
                        new TimedCondition(
                                TimedCondition.Type.GAME_TICK,
                                c -> null
                        )
                );

        if (!condition.getAvailable()) return;

        int cooldown = getDefaultAbilityProperty("cooldown", 45);

        condition.setHud(c -> {
            var tc = (TimedCondition) c;
            var duration = (tc.getDuration() / 20) + 1;
            if ((tc.getDuration() / 20.0) == 0.0) {
                return "<gold>\uD83D\uDD14 <green>Ready!</green></gold>";
            } else {
                return "<gold>\uD83D\uDD14 <red>" + duration + "s</red></gold>";
            }
        });

        condition.setDuration(cooldown * 20L);

        // ability
        Location loc = player.getLocation();
        World world = loc.getWorld();
        if (world != null) {

            world.playSound(loc, Sound.ITEM_GOAT_HORN_SOUND_5, 10.0f, 1.0f);
        }

        // Blindness 5s to seekers within 10 blocks
        final double radius = 7.5;
        for (Player victim : player.getWorld().getPlayers()) {
            if (victim == player) continue;
            if (victim.getGameMode().isInvulnerable()) continue;
            if (RoleUtil.getFactionElseThrow(player) == RoleUtil.getFactionElseThrow(victim)) continue;
            if (victim.getLocation().distanceSquared(loc) > radius * radius) continue;

            victim.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, getDefaultAbilityProperty("blindness-duration", 5) * 20, 0, false, false, true));
        }

        // Speed I for 8s, Regen I for 10s to the user
        var duration = getDefaultAbilityProperty("speed-regen-duration", 8);

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration * 20, 0, false, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration * 20, 0, false, false, true));

        player.sendMessage(ChatColor.GREEN + "Noise activated! " + ChatColor.GRAY + "(Speed I & Regen I, " + duration + "s) ");
    }

    @EventHandler
    private void handlePlayerAdd(FoundGameAddPlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var player = event.getPlayer();
        rings.put(player.getUniqueId(), SchedulingUtil.interval(20, getDefaultAbilityProperty("period", 10) * 20, () -> ringBell(player)));

        var manager = Jarona.getInstance().getConditionManager();
        initializeAbilityCondition(event, manager, c -> "<gold>\uD83D\uDD14 <green>Ready!</green></gold>");
    }

    @EventHandler
    private void handlePlayerRemoval(FoundGameRemovePlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var player = event.getPlayer();
        var manager = Jarona.getInstance().getConditionManager();
        manager.getPlayerConditions(player).remove(getId() + "_ability");
        try {
            rings.remove(player.getUniqueId()).close();
        } catch (IOException ignored) {}
    }

    private void ringBell(Player p) {
        if (!hasAbility(p)) return;
        if (!(p.getGameMode() == GameMode.ADVENTURE)) return;
        World w = p.getWorld();
        // if (w == null) return;

        w.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 2f, 1.0f);
    }

}
