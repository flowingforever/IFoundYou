package pro.fazeclan.river.ifoundyou.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pro.fazeclan.river.ifoundyou.IFoundYou;
import pro.fazeclan.river.ifoundyou.role.Faction;
import pro.fazeclan.river.ifoundyou.role.Role;
import pro.fazeclan.river.ifoundyou.util.RoleUtil;
import pro.fazeclan.river.ifoundyou.util.TimeUtil;
import pro.fazeclan.river.jarona.Jarona;
import pro.fazeclan.river.jarona.condition.TimedCondition;
import pro.fazeclan.river.jarona.game.Game;
import pro.fazeclan.river.jarona.nametag.OverridenNametag;
import pro.fazeclan.river.jarona.util.GameUtil;
import pro.fazeclan.river.jarona.util.WorldlessLocation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class FoundYouGame extends Game {
    public FoundYouGame() {
        super(
                IFoundYou.getKey("found_you"),
                true,
                true
        );
    }

    @Override
    public void init(World world, List<Player> players) {
        var plugin = IFoundYou.getInstance();
        var jarona = Jarona.getInstance();

        var roleManager = plugin.getRoleManager();
        var conditionManager = jarona.getConditionManager();
        var nametagManager = jarona.getNametagManager();

        var pluginConfig = IFoundYou.getInstance().getConfig();
        var config = YamlConfiguration.loadConfiguration(new File(world.getWorldFolder(), "map_config.yml"));

        var miniMessage = MiniMessage.miniMessage();

        // okay proper starting neow

        var runnerSpawn = WorldlessLocation.deserialize("spawn", config).toLocation(world);
        var hunterSpawn = WorldlessLocation.deserialize("hunter-spawn", config).toLocation(world);

        // selecting hunters and their roles
        var hunters = new ArrayList<Player>();

        var hunterCount = pluginConfig.getInt("hunter-count", 1);

        var queued = new ArrayList<>(players);
        Collections.shuffle(queued);

        for (int i = 0; i < hunterCount; i++) {
            var hunter = queued.getFirst();
            hunters.add(hunter);
            queued.remove(hunter);
        }

        var limitedHunterRoles = new ArrayList<>(roleManager.getLimitedRoles(Faction.HUNTERS));
        Collections.shuffle(limitedHunterRoles);
        for (Role role : limitedHunterRoles) {
            for (int i = 0; i < role.getMaxPlayers() || i < hunters.size(); i++) {
                try {
                    var hunter = hunters.get(i);
                    RoleUtil.assignRole(hunter, role, hunterSpawn);
                    hunters.remove(hunter);
                } catch (Exception ignored) {}
            }
        }
        if (!hunters.isEmpty()) {
            for (Player hunter : hunters) {
                RoleUtil.assignRole(
                        hunter,
                        roleManager.getRandomUnlimitedRole(Faction.HUNTERS),
                        hunterSpawn
                );
            }
        }

        // selecting runner roles
        var limitedRunnerRoles = new ArrayList<>(roleManager.getLimitedRoles(Faction.RUNNERS));
        Collections.shuffle(limitedRunnerRoles);
        for (Role role : limitedRunnerRoles) {
            for (int i = 0; i < role.getMaxPlayers(); i++) {
                try {
                    var runner = queued.get(i);
                    RoleUtil.assignRole(runner, role, runnerSpawn);
                    queued.remove(runner);
                } catch (Exception ignored) {}
            }
        }
        if (!queued.isEmpty()) {
            for (Player runner : queued) {
                RoleUtil.assignRole(runner, roleManager.getRandomUnlimitedRole(Faction.RUNNERS), runnerSpawn);
            }
        }

        int runnerCount = 0;

        // game prep
        for (Player player : players) {
            var role = RoleUtil.getRoleOrThrow(player);

            Title title = Title.title(Component.empty(), Component.empty());
            switch (role.getFaction()) {
                case RUNNERS -> {
                    runnerCount++;
                    title = Title.title(
                            miniMessage.deserialize("<yellow>You're a <green>Runner!"),
                            miniMessage.deserialize("<green>Avoid being killed by hunters to win!")
                    );
                    nametagManager.createOverride(player, (viewer, target) -> {
                        if (viewer.getWorld().equals(target.getWorld())) {
                            return "<green>" + target.getName() + "</green>";
                        } else {
                            return "";
                        }
                    });
                }
                case HUNTERS -> {
                    title = Title.title(
                            miniMessage.deserialize("<yellow>You're a <red>Hunter!"),
                            miniMessage.deserialize("<red>Catch and kill all runners to win.")
                    );
                    nametagManager.createOverride(player, (viewer, target) -> {
                        if (viewer.getWorld().equals(target.getWorld())) {
                            return "<red>" + target.getName() + "</red>";
                        } else {
                            return "";
                        }
                    });
                    var nametag = (OverridenNametag) nametagManager.get(player);
                    nametag.getDisplay().setSeeThrough(true);
                }
            }
            player.showTitle(title);

        }

        var graceLength = config.getInt("grace-length");
        var gameLength = config.getInt("initial-time") + (config.getInt("time-per-runner") * runnerCount);
        if (hunters.size() > 1) {
            gameLength += config.getInt("time-per-hunter") * (hunterCount - 1);
        }
        world.getPersistentDataContainer().set(
                IFoundYou.getKey("grace_length"),
                PersistentDataType.INTEGER,
                graceLength
        );
        world.getPersistentDataContainer().set(
                IFoundYou.getKey("game_length"),
                PersistentDataType.INTEGER,
                gameLength
        );

        var gameUUID = UUID.fromString(world.getKey().getKey());

        // visualize game length
        var timedCondition = conditionManager
                .getGameConditions(gameUUID)
                .getOrCreate(
                        "game_" + gameUUID,
                        new TimedCondition(
                                TimedCondition.Type.GAME_TICK
                        )
                );

        int finalGameLength = gameLength;
        timedCondition.setHud(condition -> {
            var tc = (TimedCondition) condition;
            long duration;
            if (timedCondition.getDuration() >= finalGameLength) {
                duration = (tc.getDuration() - finalGameLength);
                return "<gold><b>" + TimeUtil.ticksIntoReadableFormat((int) duration) + "</b></gold>";
            } else {
                duration = tc.getDuration();
                return "<red><b>" + TimeUtil.ticksIntoReadableFormat((int) duration) + "</b></red>";
            }
        });

        timedCondition.setHudCondition(_ -> true);
        timedCondition.setDuration(gameLength + graceLength);

        // tab stuffs
        var pdc = world.getPersistentDataContainer();
        pdc.set(Jarona.getKey("tablist_header"), PersistentDataType.STRING,
                "<newline><red>Found You!</red><newline>");

    }

    @Override
    public void tick(World world, List<Player> players) {

        var worldPDC = world.getPersistentDataContainer();
        var minimessage = MiniMessage.miniMessage();

        var graceLength = worldPDC.get(IFoundYou.getKey("grace_length"), PersistentDataType.INTEGER);
        var gameLength = worldPDC.get(IFoundYou.getKey("game_length"), PersistentDataType.INTEGER);

        worldPDC.set(
                IFoundYou.getKey("tick"),
                PersistentDataType.INTEGER,
                getCurrentGameTick(world) + 1
        );

        if (!areRunnersAlive(players) || !areHuntersAlive(players)) {
            GameUtil.endGame(world);
        }

        if (getCurrentGameTick(world) == 100) {
            var config = YamlConfiguration.loadConfiguration(new File(world.getWorldFolder(), "map_config.yml"));

            for (Player player : players) {
                player.showTitle(
                        Title.title(
                                minimessage.deserialize(config.getString("name")),
                                minimessage.deserialize(config.getString("credits"))
                        )
                );
            }
        }

        if (getCurrentGameTick(world) == graceLength) {
            var config = YamlConfiguration.loadConfiguration(new File(world.getWorldFolder(), "map_config.yml"));

            for (Player player : players) {
                player.sendMessage(minimessage.deserialize(
                        "<yellow>Hunters have entered the map, good luck!</yellow>"
                ));

                if (RoleUtil.getFactionElseThrow(player) == Faction.HUNTERS) {
                    player.teleport(WorldlessLocation.deserialize("spawn", config).toLocation(world));
                    player.addPotionEffect(
                            new PotionEffect(
                                    PotionEffectType.GLOWING,
                                    PotionEffect.INFINITE_DURATION,
                                    0, true, false
                            )
                    );
                }
            }
        }

        if (getCurrentGameTick(world) >= (graceLength + gameLength)) {
            GameUtil.endGame(world);
        }

    }

    @Override
    public void end(World world, List<Player> players) {
        var jarona = Jarona.getInstance();
        var gameUUID = UUID.fromString(world.getKey().getKey());
        var miniMessage = MiniMessage.miniMessage();

        boolean runnersWon = areRunnersAlive(players);

        var conditionManager = jarona.getConditionManager();
        conditionManager.getGameConditions(gameUUID).remove("game_" + gameUUID);

        for (Player player : players) {
            RoleUtil.removeRoles(player);

            if (runnersWon) {
                player.showTitle(
                        Title.title(
                                miniMessage.deserialize("<green>Runners"),
                                miniMessage.deserialize("win!")
                        )
                );
            } else {
                player.showTitle(
                        Title.title(
                                miniMessage.deserialize("<red>Hunters"),
                                miniMessage.deserialize("win!")
                        )
                );
            }
        }
    }

    public int getCurrentGameTick(World world) {
        return world.getPersistentDataContainer().getOrDefault(IFoundYou.getKey("tick"), PersistentDataType.INTEGER, 0);
    }

    public boolean isFactionAlive(List<Player> players, Faction faction) {
        return players.stream()
                .filter(player -> !player.getGameMode().isInvulnerable())
                .anyMatch(player -> RoleUtil.getFactionElseThrow(player).equals(faction));
    }

    public boolean areRunnersAlive(List<Player> players) {
        return isFactionAlive(players, Faction.RUNNERS);
    }

    public boolean areHuntersAlive(List<Player> players) {
        return isFactionAlive(players, Faction.HUNTERS);
    }
}
