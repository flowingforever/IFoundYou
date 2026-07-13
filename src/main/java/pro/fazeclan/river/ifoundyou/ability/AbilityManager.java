package pro.fazeclan.river.ifoundyou.ability;

import org.bukkit.event.HandlerList;
import pro.fazeclan.river.ifoundyou.IFoundYou;

import java.util.HashMap;
import java.util.Map;

public class AbilityManager {

    private final Map<String, Ability> registry = new HashMap<>();

    public void registerAbilities() {
        // abil ities
    }

    public void reloadRegistry() {
        for (Map.Entry<String, Ability> entry : registry.entrySet()) {
            HandlerList.unregisterAll(entry.getValue());
        }
        registry.clear();
        registerAbilities();
    }

    public <T extends Ability> T register(T ability) {
        registry.put(ability.getId(), ability);
        IFoundYou.getInstance()
                .getServer()
                .getPluginManager()
                .registerEvents(ability, IFoundYou.getInstance());
        return ability;
    }

    public void unregister(Ability ability) {
        registry.remove(ability.getId());
        HandlerList.unregisterAll(ability);
    }

}
