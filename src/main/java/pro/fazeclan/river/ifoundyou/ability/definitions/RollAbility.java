package pro.fazeclan.river.ifoundyou.ability.definitions;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pro.fazeclan.river.ifoundyou.ability.Ability;
import pro.fazeclan.river.ifoundyou.event.AbilityEvent;
import pro.fazeclan.river.ifoundyou.event.FoundGameAddPlayer;
import pro.fazeclan.river.ifoundyou.event.FoundGameRemovePlayer;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.TimedCondition;
import pro.fazeclan.river.jarona.condition.TimedUseCondition;

import java.util.concurrent.ThreadLocalRandom;

public class RollAbility extends Ability {

    public RollAbility() {
        super("roll");
    }

    @EventHandler
    private void handleAbility(AbilityEvent event) {
        if (!event.getExpectedAbility().equalsIgnoreCase(getId())) return;

        var player = event.getPlayer();
        var manager = Jarona.getInstance().getConditionManager();

        var condition = manager.getPlayerConditions(player)
                .getOrCreate(
                        getId() + "_ability",
                        new TimedUseCondition(
                                TimedCondition.Type.GAME_TICK,
                                c -> null,
                                getDefaultAbilityProperty("uses", 5)
                        )
                );

        if (!condition.getAvailable()) return;

        int cooldown = getDefaultAbilityProperty("cooldown", 45);

        condition.setDuration(cooldown * 20L);
        condition.increaseUses();

        // update cooldown ui
        condition.setHud(c -> {
            var tc = (TimedUseCondition) c;
            var duration = (tc.getDuration() / 20) + 1;
            if (tc.getUses() >= tc.getMaxUses()) {
                return "<dark_purple>\uD83E\uDDEA <gray>Depleted.</gray></dark_purple> " + buildUses(tc.getMaxUses(), tc.getUses());
            } else if ((tc.getDuration() / 20.0) == 0.0) {
                return "<dark_purple>\uD83E\uDDEA <green>Ready!</green></dark_purple> " + buildUses(tc.getMaxUses(), tc.getUses());
            } else {
                return "<dark_purple>\uD83E\uDDEA <red>" + duration + "s</red></dark_purple> " + buildUses(tc.getMaxUses(), tc.getUses());
            }
        });

        // Roll the bones (25% each)
        int pick = ThreadLocalRandom.current().nextInt(6);
        switch (pick) {
            case 0 -> { // Poison II (10s)
                player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 10 * 20, 0, false, false, true));
                player.sendMessage(ChatColor.DARK_GREEN + "You rolled: Poison I (10s).");
            }
            case 1 -> { // Weakness I (10s)
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 10 * 20, 0, false, false, true));
                player.sendMessage(ChatColor.GRAY + "You rolled: Weakness I (10s).");
            }
            case 2 -> { // Darkness (60s)
                player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60 * 20, 0, false, false, true));
                player.sendMessage(ChatColor.DARK_GRAY + "You rolled: Darkness (60s).");
            }
            case 3 -> { // Resistance III (10s)
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 8 * 20, 2, false, false, true));
                player.sendMessage(ChatColor.AQUA + "You rolled: Resistance III (8s).");
            }
            case 4 -> { // Absorption III (30s)
                player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 30 * 20, 2, false, false, true));
                player.sendMessage(ChatColor.GOLD + "You rolled: Absorption III (30s).");
            }
            case 5 -> { // Strength I (10s)
                player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 20, 0, false, false, true));
                player.sendMessage(ChatColor.YELLOW + "You rolled: Strength I (20s).");
            }
        }
    }

    @EventHandler
    private void handlePlayerAddition(FoundGameAddPlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var manager = Jarona.getInstance().getConditionManager();

        int maxUses = getDefaultAbilityProperty("uses", 5);
        initializeAbilityUsesCondition(
                event,
                maxUses,
                manager,
                c -> "<dark_purple>\uD83E\uDDEA <green>Ready!</green></dark_purple> " + buildUses(maxUses, 0)
        );
    }

    @EventHandler
    private void handlePlayerRemoval(FoundGameRemovePlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        Jarona.getInstance().getConditionManager()
                .getPlayerConditions(event.getPlayer())
                .remove(getId() + "_ability");
    }

}
