package pro.fazeclan.river.ifoundyou.ability;

import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import pro.fazeclan.river.ifoundyou.IFoundYou;
import pro.fazeclan.river.ifoundyou.event.FoundGameAddPlayer;
import pro.fazeclan.river.ifoundyou.role.Faction;
import pro.fazeclan.river.ifoundyou.util.RoleUtil;
import pro.fazeclan.river.jarona.condition.Condition;
import pro.fazeclan.river.jarona.condition.ConditionManager;
import pro.fazeclan.river.jarona.condition.TimedCondition;
import pro.fazeclan.river.jarona.condition.TimedUseCondition;

import java.io.File;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public class Ability implements Listener {

    @Getter
    private final String id;

    private final File file;
    private final YamlConfiguration config;

    public Ability(String id) {
        var plugin = IFoundYou.getInstance();

        plugin.saveResource("abilities/" + id + ".yml", false);
        this.file = new File(plugin.getDataFolder(), "abilities/" + id + ".yml");
        this.config = YamlConfiguration.loadConfiguration(this.file);

        this.id = id;
    }

    public <T> T getDefaultAbilityProperty(String key, T def) {
        return (T) config.get(key, def);
    }

    public boolean hasAbility(Player player) {
        var role = RoleUtil.getRole(player);
        return role.map(value -> value.getAbilities().contains(getId())).orElse(false);
    }

    public String buildUses(int max, int used) {
        var sb = new StringBuilder();
        for (int i = 0; i < max; i++) {
            if (i < max - used) {
                sb.append("<green>■</green>").append(" ");
            } else {
                sb.append("<red>■</red>").append(" ");
            }
        }
        return sb.toString().trim();
    }

    public void initializeAbilityCondition(FoundGameAddPlayer event, ConditionManager manager, Function<Condition, String> hud) {
        var player = event.getPlayer();
        var condition = manager.getPlayerConditions(player)
                .getOrCreate(
                        getId() + "_ability",
                        new TimedCondition(
                                TimedCondition.Type.GAME_TICK,
                                c -> null,
                                player.getUniqueId()
                        )
                );

        condition.reset();
        condition.setHud(hud);
        condition.setHudCondition(c -> true);
        condition.setPriority(200);
    }

    public void initializeAbilityUsesCondition(FoundGameAddPlayer event, int maxUses, ConditionManager manager, Function<Condition, String> hud) {
        var player = event.getPlayer();
        var condition = manager.getPlayerConditions(player)
                .getOrCreate(
                        getId() + "_ability",
                        new TimedUseCondition(
                                TimedCondition.Type.GAME_TICK,
                                _ -> null,
                                player.getUniqueId(),
                                maxUses
                        )
                );

        condition.reset();
        condition.setHud(hud);
        condition.setHudCondition(_ -> true);
        condition.setPriority(200);
        player.sendMessage("this ability is available: " + condition.getAvailable());
    }

    public boolean isRunner(Player player) {
        var role = RoleUtil.getRole(player);
        return role.filter(value -> value.getFaction() == Faction.RUNNERS).isPresent();
    }

    public boolean isHunter(Player player) {
        var role = RoleUtil.getRole(player);
        return role.filter(value -> value.getFaction() == Faction.HUNTERS).isPresent();
    }
}
