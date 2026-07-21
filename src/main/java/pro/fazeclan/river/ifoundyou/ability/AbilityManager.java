package pro.fazeclan.river.ifoundyou.ability;

import org.bukkit.event.HandlerList;
import pro.fazeclan.river.ifoundyou.IFoundYou;
import pro.fazeclan.river.ifoundyou.ability.definitions.*;

import java.util.HashMap;
import java.util.Map;

public class AbilityManager {

    private final Map<String, Ability> registry = new HashMap<>();

    public void registerAbilities() {
        register(new AdrenalineAbility());
        register(new BrewerAbility());
        register(new ConcealAbility());
        register(new DisplacementAbility());
        register(new EcholocateAbility());
        register(new EnragedShriekAbility());
        register(new NoisemakerAbility());
        register(new ParryAbility());
        register(new RollAbility());
        register(new StalkAbility());
        register(new TerrifyAbility());
        register(new TrapAbility());
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
