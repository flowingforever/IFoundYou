package pro.fazeclan.river.ifoundyou.role;

import org.bukkit.configuration.file.YamlConfiguration;
import pro.fazeclan.river.ifoundyou.IFoundYou;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class RoleManager {

    private final Map<String, Role> registry;

    public RoleManager() {
        this.registry = new ConcurrentHashMap<>();
    }

    public Role getRole(String id) {
        return registry.get(id);
    }

    public Collection<Role> getLimitedRoles() {
        return this.registry.values()
                .stream()
                .filter(Role::isLimited)
                .sorted().toList();
    }

    public Collection<Role> getLimitedRoles(Faction faction) {
        return this.registry.values()
                .stream()
                .filter(Role::isLimited)
                .filter(role -> role.getFaction().equals(faction))
                .sorted().toList();
    }

    public Collection<Role> getUnlimitedRoles() {
        return this.registry.values()
                .stream()
                .filter(role -> !role.isLimited())
                .sorted().toList();
    }

    public Collection<Role> getUnlimitedRoles(Faction faction) {
        return this.registry.values()
                .stream()
                .filter(role -> !role.isLimited())
                .filter(role -> role.getFaction().equals(faction))
                .sorted().toList();
    }

    public Role getRandomUnlimitedRole() {
        var roles = getUnlimitedRoles();
        int index = ThreadLocalRandom.current().nextInt(roles.size());
        return (Role) roles.toArray()[index];
    }

    public Role getRandomUnlimitedRole(Faction faction) {
        var roles = getUnlimitedRoles(faction);
        int index = ThreadLocalRandom.current().nextInt(roles.size());
        return (Role) roles.toArray()[index];
    }

    public Collection<Role> getRoles() {
        return this.registry.values()
                .stream()
                .sorted().toList();
    }

    public Collection<Role> getRoles(Faction faction) {
        return this.registry.values()
                .stream()
                .filter(role -> role.getFaction().equals(faction))
                .sorted().toList();
    }

    public void reloadRegistry() {
        registry.clear();

        var plugin = IFoundYou.getInstance();
        var folder = new File(plugin.getDataFolder(), "roles");
        if (!folder.exists()) {
            plugin.getLogger().warning("Roles folder does not exist, skipping role registry reload.");
            return;
        }
        var files = folder.listFiles();
        if (files == null) {
            plugin.getLogger().warning("Roles folder does not contain any files, skipping role registry reload.");
            return;
        }

        Arrays.stream(files)
                .filter(file -> file.getName().endsWith(".yml"))
                .map(file -> {
                    var config = YamlConfiguration.loadConfiguration(file);
                    return new Role(
                            config.getString("name", "broken"),
                            file.getName().replace(".yml", ""),
                            config.getString("inventory", ""),
                            Faction.valueOf(config.getString("faction", "runner").toUpperCase()),
                            config.getInt("max-players", -1),
                            new HashSet<>(config.getStringList("abilities")),
                            config.getString("description", "")
                    );
                })
                .forEach(role -> registry.put(role.getId(), role));
    }

    public void createRole(
            String name,
            String id,
            String items,
            Faction faction,
            int maxPlayers,
            Set<String> abilities,
            String description
    ) {
        createRole(new Role(name, id, items, faction, maxPlayers, abilities, description));
    }

    public void createRole(Role role) {
        var plugin = IFoundYou.getInstance();
        var folder = new File(plugin.getDataFolder(), "roles");
        if (!folder.exists()) {
            folder.mkdir();
        }
        var file = new File(folder, role.getId() + ".yml");
        if (file.exists()) {
            plugin.getLogger().warning("That role already exists; skipping role creation");
            return;
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().warning("Role file failed to create; skipping role creation");
            return;
        }

        var config = YamlConfiguration.loadConfiguration(file);

        config.set("name", role.getName());
        config.set("inventory", role.getItems());
        config.set("faction", role.getFaction().name().toLowerCase());
        config.set("max-players", role.getMaxPlayers());
        config.set("abilities", role.getAbilities());
        config.set("description", role.getDescription());

        try {
            config.save(file);
            registry.put(role.getId(), role);
        } catch (IOException e) {
            plugin.getLogger().warning("Role config file failed to save; skipping role creation");
        }
    }

    public void removeRole(Role role) {
        removeRole(role.getId());
    }

    public void removeRole(String id) {
        var folder = new File(IFoundYou.getInstance().getDataFolder(), "roles");
        if (!folder.exists()) {
            return;
        }
        var file = new File(folder, id + ".yml");
        if (!file.exists()) {
            return;
        }
        registry.remove(id);
        file.delete();
    }

}
