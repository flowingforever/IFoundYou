package pro.fazeclan.river.ifoundyou.role;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

@Getter
public class Role implements Comparable<Role> {

    private final String name;
    private final String id;
    private final String items;
    private final Faction faction;
    private final int maxPlayers;
    private final Set<String> abilities;
    private final String description;

    public Role(
            String name,
            String id,
            String items,
            Faction faction,
            int maxPlayers,
            Set<String> abilities,
            String description
    ) {
        this.name = name;
        this.id = id;
        this.items = items;
        this.faction = faction;
        this.maxPlayers = maxPlayers;
        this.abilities = abilities;
        this.description = description;
    }

    public Role(
            String name,
            String id,
            String items,
            Faction faction,
            int maxPlayers,
            Set<String> abilities
    ) {
        this.name = name;
        this.id = id;
        this.items = items;
        this.faction = faction;
        this.maxPlayers = maxPlayers;
        this.abilities = abilities;
        this.description = "";
    }

    public Role(
            String name,
            String id,
            String items,
            Faction faction,
            int maxPlayers
    ) {
        this.name = name;
        this.id = id;
        this.items = items;
        this.faction = faction;
        this.maxPlayers = maxPlayers;
        this.abilities = new HashSet<>();
        this.description = "";
    }

    public Role(
            String name,
            String id,
            String items,
            Faction faction
    ) {
        this.name = name;
        this.id = id;
        this.items = items;
        this.faction = faction;
        this.maxPlayers = -1;
        this.abilities = new HashSet<>();
        this.description = "";
    }

    public boolean isLimited() {
        return maxPlayers > -1;
    }

    @Override
    public int compareTo(@NotNull Role o) {
        return Integer.compare(o.maxPlayers, this.maxPlayers);
    }
}
