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

public class ConcealAbility extends Ability {

    public ConcealAbility() {
        super("conceal");
    }

    @EventHandler
    private void handleAbility(AbilityEvent event) {
        if (!event.getExpectedAbility().equalsIgnoreCase(getId())) return;

        var conditionManager = Jarona.getInstance().getConditionManager();
        var player = event.getPlayer();

        // uses cap
        int maxUses = getDefaultAbilityProperty("uses", 2);

        // cooldown
        var condition = conditionManager.getPlayerConditions(player).getOrCreate(
                getId() + "_ability",
                new TimedUseCondition(
                        TimedCondition.Type.GAME_TICK,
                        c -> null,
                        player.getUniqueId(),
                        maxUses
                )
        );

        if (!condition.getAvailable()) {
            return;
        }

        // update ui
        var cooldown = getDefaultAbilityProperty("cooldown", 30);

        condition.setDuration(cooldown * 20L);
        condition.increaseUses();

        condition.setHud(c -> {
            var tc = (TimedUseCondition) c;
            var duration = (tc.getDuration() / 20) + 1;
            if (tc.getUses() >= tc.getMaxUses()) {
                return "<dark_gray>░ <gray>Depleted.</gray></dark_gray> " + buildUses(tc.getMaxUses(), tc.getUses());
            } else if ((tc.getDuration() / 20.0) == 0.0) {
                return "<dark_gray>░ <green>Ready!</green></dark_gray> " + buildUses(tc.getMaxUses(), tc.getUses());
            } else {
                return "<dark_gray>░ <red>" + duration +"s</red></dark_gray> " + buildUses(tc.getMaxUses(), tc.getUses());
            }
        });

        // Apply Invisibility for 5 seconds.

        var duration = getDefaultAbilityProperty("duration", 8);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration * 20, 0, false, false, true));

        player.sendMessage(ChatColor.GREEN + "Conceal activated! " + ChatColor.GRAY + "(Invisibility, " + duration + "s) "
                + ChatColor.DARK_AQUA + "[" + (condition.getMaxUses() - condition.getUses()) + " left]");
    }

    @EventHandler
    private void handleGamePlayerAdd(FoundGameAddPlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) return;

        var conditionManager = Jarona.getInstance().getConditionManager();
        int maxUses = getDefaultAbilityProperty("uses", 2);
        initializeAbilityUsesCondition(
                event,
                maxUses,
                conditionManager,
                c -> "<dark_gray>░ <green>Ready!</green></dark_gray> " + buildUses(maxUses, 0)
        );
    }

    @EventHandler
    private void handleGamePlayerRemoval(FoundGameRemovePlayer event) {
        var conditionManager = Jarona.getInstance().getConditionManager();
        conditionManager.getPlayerConditions(event.getPlayer())
                .remove(getId() + "_ability");
    }

}
