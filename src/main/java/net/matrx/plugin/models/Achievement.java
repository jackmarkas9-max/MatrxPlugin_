package net.matrx.plugin.models;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

public class Achievement {
    private final String id;
    private final String displayName;
    private final String description;
    private final Material icon;
    private final int xpReward;

    public Achievement(String id, String displayName, String description, Material icon, int xpReward) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.xpReward = xpReward;
    }

    public String getId() { return id; }
    public String getDisplayName() { return ChatColor.translateAlternateColorCodes('&', displayName); }
    public String getDescription() { return ChatColor.translateAlternateColorCodes('&', description); }
    public Material getIcon() { return icon; }
    public int getXpReward() { return xpReward; }

    public ItemStack createIcon(boolean unlocked) {
        ItemStack item = new ItemStack(unlocked ? icon : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName((unlocked ? "&a" : "&7") + displayName);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + description);
        lore.add("");
        lore.add(unlocked ? "&a✔ Unlocked!" : "&cNot yet unlocked");
        lore.add("&e+" + xpReward + " XP reward");
        meta.setLore(lore.stream().map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
        item.setItemMeta(meta);
        return item;
    }

    public static final Map<String, Achievement> ALL = new LinkedHashMap<>();

    static {
        register("first_steps", "&bFirst Steps", "Complete your first parkour course", Material.IRON_BOOTS, 50);
        register("speed_demon", "&6Speed Demon", "Complete a course under par time", Material.BLAZE_POWDER, 100);
        register("combo_king", "&cCombo King", "Reach a 10-checkpoint combo", Material.NETHERITE_SWORD, 200);
        register("combo_god", "&dCombo God", "Reach a 25-checkpoint combo", Material.NETHER_STAR, 500);
        register("star_gazer", "&aStar Gazer", "Find your first hidden star", Material.END_CRYSTAL, 75);
        register("star_collector", "&2Star Collector", "Find 10 hidden stars", Material.SEA_LANTERN, 300);
        register("perfectionist", "&fPerfectionist", "Complete a course with max combo", Material.QUARTZ, 150);
        register("explorer", "&eExplorer", "Complete 5 different courses", Material.MAP, 100);
        register("veteran", "&5Veteran", "Complete 50 courses total", Material.DIAMOND_CHESTPLATE, 500);
        register("marathon", "&9Marathon", "Complete 100 courses total", Material.ELYTRA, 1000);
        register("speed_runner", "&eSpeed Runner", "Complete a course in under 10 seconds", Material.GOLDEN_BOOTS, 250);
        register("dedication", "&dDedication", "Play for 10 hours total", Material.CLOCK, 400);
        register("diamond_hands", "&bDiamond Hands", "Reach Diamond rank", Material.DIAMOND, 200);
        register("legendary", "&5Legendary", "Reach Legend rank", Material.DRAGON_HEAD, 1000);
        register("impossible", "&4Impossible", "Complete an Impossible tier course", Material.BEDROCK, 1000);
    }

    private static void register(String id, String name, String desc, Material icon, int xp) {
        ALL.put(id, new Achievement(id, name, desc, icon, xp));
    }

    public static Achievement get(String id) {
        return ALL.get(id);
    }

    public static Collection<Achievement> values() {
        return ALL.values();
    }
}
