package com.luludev.autoswing;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class AutoSwingPlugin extends JavaPlugin implements Listener {

    private final Map<UUID, BukkitTask> autoSwingTasks = new HashMap<>();

    // ค่า config
    private int swingIntervalTicks;
    private boolean requireSneak;
    private String msgEnabled;
    private String msgDisabled;
    private String msgWrongWeapon;

    @Override
    public void onEnable() {
        saveDefaultConfig();  // จะสร้าง /plugins/AutoSwing/config.yml อัตโนมัติ
        loadConfigValues();
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("AutoSwing enabled!");
    }

    @Override
    public void onDisable() {
        for (BukkitTask task : autoSwingTasks.values()) {
            task.cancel();
        }
        autoSwingTasks.clear();
        getLogger().info("AutoSwing disabled!");
    }

    private void loadConfigValues() {
        // ถ้าไม่มี config.yml จริง ๆ getConfig() ก็ยังใช้ได้ จะเป็น config ว่าง ๆ
        swingIntervalTicks = getConfig().getInt("swing-interval-ticks", 4);
        requireSneak = getConfig().getBoolean("require-sneak", true);

        msgEnabled = color(getConfig().getString("messages.enabled",
                "&a[AutoSwing] &fเปิดโหมดสวิงอัตโนมัติแล้ว!"));
        msgDisabled = color(getConfig().getString("messages.disabled",
                "&c[AutoSwing] &fปิดโหมดสวิงอัตโนมัติแล้ว!"));
        msgWrongWeapon = color(getConfig().getString("messages.wrong-weapon",
                "&c[AutoSwing] &fต้องถือดาบหรือขวานเท่านั้น!"));
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();

        // คลิกขวาเท่านั้น
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();

        // ถ้า config บังคับให้ต้องย่อตัว
        if (requireSneak && !player.isSneaking()) {
            return;
        }

        // ต้องถือดาบหรือขวานเท่านั้น
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isAllowedWeapon(item)) {
            if (!msgWrongWeapon.isEmpty()) {
                player.sendMessage(msgWrongWeapon);
            }
            return;
        }

        UUID uuid = player.getUniqueId();

        if (autoSwingTasks.containsKey(uuid)) {
            stopAutoSwing(player);
        } else {
            startAutoSwing(player);
        }
    }

    private boolean isAllowedWeapon(ItemStack item) {
        if (item == null) return false;

        Material type = item.getType();
        switch (type) {
            // ดาบ
            case WOODEN_SWORD:
            case STONE_SWORD:
            case IRON_SWORD:
            case GOLDEN_SWORD:
            case DIAMOND_SWORD:
            case NETHERITE_SWORD:
                return true;

            // ขวาน
            case WOODEN_AXE:
            case STONE_AXE:
            case IRON_AXE:
            case GOLDEN_AXE:
            case DIAMOND_AXE:
            case NETHERITE_AXE:
                return true;

            default:
                return false;
        }
    }

    private void startAutoSwing(Player player) {
        UUID uuid = player.getUniqueId();

        stopAutoSwing(player); // กัน task เก่า

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    stopAutoSwing(player);
                    return;
                }

                player.swingMainHand();
            }
        }.runTaskTimer(this, 0L, swingIntervalTicks);

        autoSwingTasks.put(uuid, task);
        player.sendMessage(msgEnabled);
    }

    private void stopAutoSwing(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitTask task = autoSwingTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        player.sendMessage(msgDisabled);
    }
}
