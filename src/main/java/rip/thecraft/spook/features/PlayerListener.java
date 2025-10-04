package rip.thecraft.spook.features;

import rip.thecraft.spook.Spook;
import rip.thecraft.spook.util.*;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.*;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Optimized PlayerListener with caching, proper error handling, and performance improvements.
 * Thread-safe for Folia compatibility.
 */
public class PlayerListener implements Listener {

    // Cache frequently used items to reduce object creation
    private final ConcurrentHashMap<String, ItemStack> itemCache = new ConcurrentHashMap<>();
    private Location spawnLocation;
    private long lastConfigUpdate = 0;
    private final long CONFIG_CACHE_TIME = 30000; // 30 seconds

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        try {
            // Clear inventory
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            
            // Set gamemode and health
            if (player.getGameMode() != GameMode.CREATIVE) {
                player.setGameMode(GameMode.ADVENTURE);
            }
            player.setHealth(player.getMaxHealth());
            player.setFoodLevel(20);
            player.setSaturation(20f);
            
            // Teleport to spawn using scheduler for thread safety
            SchedulerUtil.runTaskForEntity(Spook.getInstance(), player, () -> {
                Location spawn = getSpawnLocation();
                if (spawn != null) {
                    player.teleport(spawn);
                } else {
                    Spook.getInstance().getLogger().warning("No spawn location set! Use /setspawn command.");
                }
            });

            // Give hub items
            giveHubItems(player);
            
            // Clear chat (optimized - single title instead of 100 messages)
            clearPlayerChat(player);
            
        } catch (Exception e) {
            Spook.getInstance().getLogger().log(Level.WARNING, "Error handling player join for " + player.getName(), e);
        }
    }
    
    private void giveHubItems(@NotNull Player player) {
        try {
            FileConfiguration config = getConfig();
            
            // Server selector
            ItemStack serverSelector = getCachedItem("server_selector", () -> {
                Material material = Material.valueOf(config.getString("ITEM.SERVER_SELECTOR.MATERIAL", "BOOK").toUpperCase());
                return new ItemBuilder(material)
                        .setName(config.getString("ITEM.SERVER_SELECTOR.NAME", "&6Servers"))
                        .create(true); // Use cache
            });
            
            int selectorSlot = config.getInt("ITEM.SERVER_SELECTOR.SLOT", 4);
            player.getInventory().setItem(selectorSlot, serverSelector);

            // Ender butt
            ItemStack enderButt = getCachedItem("ender_butt", () -> {
                Material material = Material.valueOf(config.getString("ITEM.ENDER_BUTT.MATERIAL", "ENDER_PEARL").toUpperCase());
                return new ItemBuilder(material)
                        .setName(config.getString("ITEM.ENDER_BUTT.NAME", "&bEnderButt"))
                        .create(true); // Use cache
            });
            
            int enderButtSlot = config.getInt("ITEM.ENDER_BUTT.SLOT", 3);
            player.getInventory().setItem(enderButtSlot, enderButt);
            
        } catch (Exception e) {
            Spook.getInstance().getLogger().log(Level.WARNING, "Error giving hub items to " + player.getName(), e);
        }
    }
    
    private ItemStack getCachedItem(String key, java.util.function.Supplier<ItemStack> supplier) {
        return itemCache.computeIfAbsent(key, k -> supplier.get()).clone();
    }
    
    private void clearPlayerChat(@NotNull Player player) {
        // More efficient than sending 100 empty messages
        SchedulerUtil.runTaskLater(Spook.getInstance(), () -> {
            try {
                player.sendTitle("", "", 0, 1, 0);
                // Send a few empty messages for older clients
                for (int i = 0; i < 5; i++) {
                    player.sendMessage("");
                }
            } catch (Exception e) {
                // Fallback for very old versions
                for (int i = 0; i < 20; i++) {
                    player.sendMessage("");
                }
            }
        }, 1L);
    }
    
    private Location getSpawnLocation() {
        long currentTime = System.currentTimeMillis();
        if (spawnLocation == null || (currentTime - lastConfigUpdate) > CONFIG_CACHE_TIME) {
            FileConfiguration config = getConfig();
            String spawnString = config.getString("Spawn.location");
            spawnLocation = LocationUtil.parseToLocation(spawnString);
            lastConfigUpdate = currentTime;
        }
        return spawnLocation;
    }
    
    private FileConfiguration getConfig() {
        return Spook.getInstance().getConfig();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onWeather(@NotNull WeatherChangeEvent event) {
        if (event.toWeatherState()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onFood(@NotNull FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDamage(@NotNull EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntitySpawn(@NotNull EntitySpawnEvent event) {
        // Prevent all mob spawning in hub (except players and items)
        EntityType type = event.getEntityType();
        
        // Allow only essential entities
        if (type != EntityType.PLAYER &&
            type != EntityType.ARMOR_STAND &&
            type != EntityType.ITEM_FRAME) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onClick(@NotNull InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (player.getGameMode() != GameMode.CREATIVE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPickup(@NotNull EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (player.getGameMode() != GameMode.CREATIVE) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onDrop(@NotNull PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(@NotNull BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("hub.command.place") || player.getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("hub.command.break") || player.getGameMode() != GameMode.CREATIVE) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityExplode(@NotNull EntityExplodeEvent event) {
        event.setCancelled(true);
    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void onBlockExplode(@NotNull BlockExplodeEvent event) {
        event.setCancelled(true);
    }
    
    /**
     * Clear cached items (useful for config reloads)
     */
    public void clearCache() {
        itemCache.clear();
        spawnLocation = null;
        lastConfigUpdate = 0;
    }
    
    /**
     * Get cache size for monitoring
     */
    public int getCacheSize() {
        return itemCache.size();
    }
}
