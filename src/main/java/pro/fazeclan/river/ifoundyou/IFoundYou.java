package pro.fazeclan.river.ifoundyou;

import de.tr7zw.nbtapi.NBT;
import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import pro.fazeclan.river.ifoundyou.ability.AbilityManager;
import pro.fazeclan.river.ifoundyou.game.FoundYouGame;
import pro.fazeclan.river.ifoundyou.listener.GameListeners;
import pro.fazeclan.river.ifoundyou.role.RoleManager;
import pro.fazeclan.river.jarona.Jarona;

public final class IFoundYou extends JavaPlugin {

    @Getter
    RoleManager roleManager;

    @Getter
    AbilityManager abilityManager;

    @Override
    public void onLoad() {
        this.roleManager = new RoleManager();
        this.abilityManager = new AbilityManager();
    }

    @Override
    public void onEnable() {
        if (!NBT.preloadApi()) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        var manager = Jarona.getInstance().getGameManager();
        manager.register(new FoundYouGame());

        saveDefaultConfig();

        this.roleManager.reloadRegistry();
        this.abilityManager.registerAbilities();

        getServer().getPluginManager().registerEvents(new GameListeners(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static IFoundYou getInstance() {
        return JavaPlugin.getPlugin(IFoundYou.class);
    }

    public static NamespacedKey getKey(String value) {
        return new NamespacedKey(getInstance(), value);
    }

}
