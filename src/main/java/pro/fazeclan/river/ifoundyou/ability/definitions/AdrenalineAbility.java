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

public class AdrenalineAbility extends Ability {

    public AdrenalineAbility() { super("adrenaline"); }

    @EventHandler
    private void handleAbility(AbilityEvent event) {
        if (!event.getExpectedAbility().equalsIgnoreCase(getId())) return;
        var conditionManager = Jarona.getInstance().getConditionManager();

        var cooldown = getDefaultAbilityProperty("cooldown", 30) * 20L;
        var player = event.getPlayer();

        var maxUses = getDefaultAbilityProperty("uses", 2);

        var condition = conditionManager.getPlayerConditions(player)
                .getOrCreate(
                        getId() + "_ability",
                        new TimedUseCondition(
                                TimedCondition.Type.GAME_TICK,
                                c -> null,
                                player.getUniqueId(),
                                maxUses
                        )
                );

        if (!condition.getAvailable()) return;

        condition.setDuration(cooldown);

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.SPEED,
                5 * 20,
                1,
                false,
                false,
                true
        ));
        condition.increaseUses();

        condition.setHud(c -> {
            var tc = (TimedUseCondition) c;
            var duration = (tc.getDuration() / 20) + 1;
            if (tc.getUses() >= tc.getMaxUses()) {
                return "<yellow>⚡ <gray>Depleted.</gray></yellow> " + buildUses(tc.getMaxUses(), tc.getUses());
            } else if ((tc.getDuration() / 20.0) == 0.0) {
                return "<yellow>⚡ <green>Ready!</green></yellow> " + buildUses(tc.getMaxUses(), tc.getUses());
            } else {
                return "<yellow>⚡ <red>" + duration +"s</red></yellow> " + buildUses(tc.getMaxUses(), tc.getUses());
            }
        });

        player.sendMessage(ChatColor.GREEN + "Adrenaline activated! " + ChatColor.GRAY + "(Speed II, 5s) "
                + ChatColor.DARK_AQUA + "[" + (condition.getMaxUses() - condition.getUses()) + " left]");

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
                c -> "<yellow>⚡ <green>Ready!</green></yellow> " + buildUses(maxUses, 0)
        );
    }

    @EventHandler
    private void handleGameRemovePlayer(FoundGameRemovePlayer event) {
        var conditionManager = Jarona.getInstance().getConditionManager();
        conditionManager.getPlayerConditions(event.getPlayer())
                .remove(getId() + "_ability");
    }

}
