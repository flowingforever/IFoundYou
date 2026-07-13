package pro.fazeclan.river.ifoundyou.ability.definitions;

import org.bukkit.event.EventHandler;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pro.fazeclan.river.ifoundyou.ability.Ability;
import pro.fazeclan.river.ifoundyou.event.AbilityEvent;
import pro.fazeclan.river.ifoundyou.event.FoundGameAddPlayer;
import pro.fazeclan.river.ifoundyou.event.FoundGameRemovePlayer;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.TimedCondition;

public class DisplacementAbility extends Ability {

    public DisplacementAbility() {
        super("displacement");
    }

    @EventHandler
    private void handleAbility(AbilityEvent event) {
        if (!event.getExpectedAbility().equalsIgnoreCase(getId())) return;

        var manager = Jarona.getInstance().getConditionManager();
        var player = event.getPlayer();

        var condition = manager.getPlayerConditions(player).getOrCreate(
                getId() + "_ability",
                new TimedCondition(
                        TimedCondition.Type.GAME_TICK,
                        c -> null
                )
        );

        if (!condition.getAvailable()) {
            return;
        }

        var cooldown = getDefaultAbilityProperty("cooldown", 40);

        condition.setHud(c -> {
            var tc = (TimedCondition) c;
            var duration = (tc.getDuration() / 20) + 1;
            if ((tc.getDuration() / 20.0) == 0.0) {
                return "<aqua>⏩ <green>Ready!</green></aqua>";
            } else {
                return "<aqua>⏩ <red>" + duration + "s</red></aqua>";
            }
        });
        condition.setDuration(cooldown * 20); // set cooldown

        // Apply effects: Speed III for 3s; Weakness IV for 4s
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED,
                getDefaultAbilityProperty("speed.duration", 3) * 20,
                getDefaultAbilityProperty("speed.amplifier", 2),
                false,
                false,
                true
        ));
        player.addPotionEffect(new PotionEffect(
                PotionEffectType.WEAKNESS,
                getDefaultAbilityProperty("weakness.duration", 4) * 20,
                getDefaultAbilityProperty("weakness.amplifier", 3),
                false,
                false,
                true
        ));
    }

    @EventHandler
    private void handleGamePlayerAdd(FoundGameAddPlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var manager = Jarona.getInstance().getConditionManager();
        initializeAbilityCondition(event, manager, c -> "<aqua>⏩ <green>Ready!</green></aqua>");
    }

    @EventHandler
    private void handleGamePlayerRemoval(FoundGameRemovePlayer event) {
        var conditionManager = Jarona.getInstance().getConditionManager();
        conditionManager.getPlayerConditions(event.getPlayer())
                .remove(getId() + "_ability");
    }

}
