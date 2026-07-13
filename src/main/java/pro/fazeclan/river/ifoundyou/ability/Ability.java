package pro.fazeclan.river.ifoundyou.ability;

import lombok.Getter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import pro.fazeclan.river.ifoundyou.IFoundYou;
import pro.fazeclan.river.ifoundyou.util.RoleUtil;

import java.io.File;

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

}
