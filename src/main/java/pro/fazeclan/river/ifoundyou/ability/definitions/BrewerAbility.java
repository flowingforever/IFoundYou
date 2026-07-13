package pro.fazeclan.river.ifoundyou.ability.definitions;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import pro.fazeclan.river.ifoundyou.ability.Ability;
import pro.fazeclan.river.ifoundyou.event.FoundGameAddPlayer;
import pro.fazeclan.river.ifoundyou.event.FoundGameRemovePlayer;
import pro.fazeclan.river.jarona.util.SchedulingUtil;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class BrewerAbility extends Ability {

    // player uuid to task map
    private final Map<UUID, Closeable> brewTasks = new HashMap<>();

    public BrewerAbility() {
        super("brewer");
    }

    @EventHandler
    private void handleGameAddPlayer(FoundGameAddPlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) {
            return;
        }
        var player = event.getPlayer();
        int interval = getDefaultAbilityProperty("interval", 45);
        brewTasks.put(player.getUniqueId(), SchedulingUtil.interval(20, interval * 20L, () -> {
            brewFor(player);
        }));
    }

    @EventHandler
    private void handleGameRemovePlayer(FoundGameRemovePlayer event) {
        if (!event.getRole().getAbilities().contains(getId())) {
            return;
        }
        try {
            brewTasks.remove(event.getPlayer().getUniqueId()).close();
        } catch (IOException ignored) {}
    }

    private void brewFor(Player p) {
        ItemStack potion = randomPotion();
        giveOrDrop(p, potion);
        p.sendMessage(ChatColor.GREEN + "You conjured a new potion!");
    }

    // ---- POTION RECIPES ----
    private ItemStack randomPotion() {
        List<ItemStack> recipes = new ArrayList<>(5);
        recipes.add(potionOfRevitalization());
        recipes.add(vileConcoction());
        recipes.add(oneWingedAngel());
        recipes.add(deathsDance());
        recipes.add(nearPerfectedAmbrosia());

        return recipes.get(ThreadLocalRandom.current().nextInt(recipes.size()));
    }

    // Potion of Revitalization (splash): Instant Health + Regen II (0:05)
    private ItemStack potionOfRevitalization() {
        ItemStack item = new ItemStack(Material.SPLASH_POTION, 1);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Potion of Revitalization");
        // INSTANT_HEALTH fires on drink/throw
        meta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 0, false, false, true), true);
        meta.addCustomEffect(new PotionEffect(PotionEffectType.REGENERATION, 5 * 20, 1, false, false, true), true); // II = amp 1
        meta.setColor(Color.FUCHSIA);
        item.setItemMeta(meta);
        return item;
    }

    // Vile Concoction: Weakness III (0:03), Nausea (0:03), Mining Fatigue III (0:03)
    private ItemStack vileConcoction() {
        ItemStack item = new ItemStack(Material.LINGERING_POTION, 1);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_GREEN + "Vile Concoction");
        meta.addCustomEffect(new PotionEffect(PotionEffectType.WEAKNESS, 3 * 20, 2, false, false, true), true);       // III
        meta.addCustomEffect(new PotionEffect(PotionEffectType.NAUSEA, 3 * 20, 0, false, false, true), true);      // Nausea
        meta.addCustomEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 3 * 20, 2, false, false, true), true);   // Mining Fatigue III
        meta.setColor(Color.OLIVE);
        item.setItemMeta(meta);
        return item;
    }

    // One-Winged Angel: Slow Falling II (0:04), Speed II (0:06)
    private ItemStack oneWingedAngel() {
        ItemStack item = new ItemStack(Material.POTION, 1);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "One-Winged Angel");
        meta.addCustomEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 4 * 20, 1, false, false, true), true); // II
        meta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 6 * 20, 1, false, false, true), true);        // II
        meta.setColor(Color.TEAL);
        item.setItemMeta(meta);
        return item;
    }

    // Death's Dance: Wither II (0:07), Speed III (0:10)
    private ItemStack deathsDance() {
        ItemStack item = new ItemStack(Material.POTION, 1);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setDisplayName(ChatColor.DARK_PURPLE + "Death's Dance");
        meta.addCustomEffect(new PotionEffect(PotionEffectType.WITHER, 7 * 20, 1, false, false, true), true); // II
        meta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 10 * 20, 2, false, false, true), true); // III
        meta.setColor(Color.PURPLE);
        item.setItemMeta(meta);
        return item;
    }

    // Near-Perfected Ambrosia: Instant Health II, Regeneration II (0:10), Nausea (0:10)
    private ItemStack nearPerfectedAmbrosia() {
        ItemStack item = new ItemStack(Material.POTION, 1);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Near-Perfected Ambrosia");
        meta.addCustomEffect(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 1, false, false, true), true); // II
        meta.addCustomEffect(new PotionEffect(PotionEffectType.REGENERATION, 10 * 20, 1, false, false, true), true); // II
        meta.addCustomEffect(new PotionEffect(PotionEffectType.NAUSEA, 10 * 20, 0, false, false, true), true);    // Nausea
        meta.setColor(Color.ORANGE);
        item.setItemMeta(meta);
        return item;
    }

    // ---- helpers ----
    private void giveOrDrop(Player p, ItemStack item) {
        PlayerInventory inv = p.getInventory();
        var leftover = inv.addItem(item);
        if (!leftover.isEmpty()) {
            World w = p.getWorld();
            if (w != null) w.dropItemNaturally(p.getLocation(), item);
        }
    }

}
