package rip.thecraft.spook.server;

import rip.thecraft.spook.Spook;
import rip.thecraft.spook.util.*;
import rip.thecraft.spook.util.FastItemUtil;
import rip.thecraft.spook.util.MaterialUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;

/**
 * Simple and working ServerSelector - easy to configure and use.
 * Compatible with both Folia and Paper.
 */
public class ServerSelector implements Listener {

    private static final String GUI_TITLE = CC.translate("&8» &6&lServer Selection &8«");
    private final Map<String, String> serverCommands = new HashMap<>();
    
    // Rate limiting - prevent GUI spam (500ms cooldown)
    private final Map<UUID, Long> lastInteraction = new HashMap<>();
    private static final long COOLDOWN_MS = 500L;
    
    // CACHED GUI - Created once, reused forever
    private Inventory cachedGUI = null;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(@NotNull PlayerInteractEvent event) {
        // ULTRA FAST: Single line check with immediate return
        if (!FastItemUtil.isServerSelector(event.getItem())) return;
        
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        
        // Rate limiting - prevent spam abuse
        Long lastTime = lastInteraction.get(playerId);
        if (lastTime != null && (currentTime - lastTime) < COOLDOWN_MS) {
            event.setCancelled(true);
            return;
        }
        
        lastInteraction.put(playerId, currentTime);
        event.setCancelled(true);
        
        // Direct calls - no try-catch overhead in hot path
        openServerSelector(player);
        playClickSound(player);
    }
    
    private void openServerSelector(@NotNull Player player) {
        // Use cached GUI - create only if needed
        if (cachedGUI == null) {
            cachedGUI = createServerGUI();
        }
        
        // Delay inventory opening by 1 tick to avoid advancement trigger overhead
        SchedulerUtil.runTaskLaterForEntity(Spook.getInstance(), player, () -> {
            player.openInventory(cachedGUI);
        }, 1L);
    }
    
    private Inventory createServerGUI() {
        ConfigurationSection serversConfig = Spook.getInstance().getConfig().getConfigurationSection("servers");
        
        if (serversConfig == null) {
            Spook.getInstance().getLogger().warning("No servers configured in config.yml! Please add servers under 'servers:' section");
            Spook.getInstance().getLogger().info("Config keys found: " + Spook.getInstance().getConfig().getKeys(true));
            return Bukkit.createInventory(null, 27, GUI_TITLE);
        }
        
        Set<String> serverNames = serversConfig.getKeys(false);
        int guiSize = Math.max(27, ((serverNames.size() / 9) + 1) * 9); // Round up to nearest multiple of 9
        guiSize = Math.min(guiSize, 54); // Max 6 rows
        
        Inventory gui = Bukkit.createInventory(null, guiSize, GUI_TITLE);
        
        // Fill with glass panes (cross-version compatible)
        ItemStack glassPane = new ItemBuilder(MaterialUtil.getGlassPane())
                .setName(" ")
                .create();
        
        for (int i = 0; i < gui.getSize(); i++) {
            gui.setItem(i, glassPane);
        }
        
        // Add server items
        serverCommands.clear();
        int slot = 10; // Start from slot 10 for better positioning
        
        for (String serverKey : serverNames) {
            ConfigurationSection serverSection = serversConfig.getConfigurationSection(serverKey);
            if (serverSection == null) continue;
            
            String displayName = CC.translate(serverSection.getString("name", "&a" + serverKey));
            String materialName = serverSection.getString("material", "GRASS_BLOCK");
            List<String> lore = serverSection.getStringList("lore");
            String command = serverSection.getString("command", "server " + serverKey);
            
            // Process lore
            List<String> processedLore = new ArrayList<>();
            for (String line : lore) {
                processedLore.add(CC.translate(line));
            }
            
            try {
                Material material = MaterialUtil.getMaterial(materialName);
                
                ItemStack serverItem = new ItemBuilder(material)
                        .setName(displayName)
                        .setLore(processedLore)
                        .create();
                
                if (slot < gui.getSize()) {
                    gui.setItem(slot, serverItem);
                    serverCommands.put(displayName, command);
                    
                    slot++;
                    // Skip glass pane slots for better layout
                    if (slot % 9 == 8) slot += 2; // Skip to next row
                }
                
            } catch (IllegalArgumentException e) {
                Spook.getInstance().getLogger().warning("Invalid material: " + materialName + " for server: " + serverKey);
            }
        }
        
        return gui;
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        
        if (!GUI_TITLE.equals(event.getView().getTitle())) {
            return;
        }
        
        event.setCancelled(true);
        
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return;
        }
        
        String displayName = meta.getDisplayName();
        String command = serverCommands.get(displayName);
        
        if (command != null) {
            player.closeInventory();
            playSuccessSound(player);
            
            // Small delay to ensure inventory is fully closed before connecting
            SchedulerUtil.runTaskLaterForEntity(Spook.getInstance(), player, () -> {
                connectToServer(player, command);
            }, 2L); // 2 ticks = 0.1 seconds
        }
    }
    
    private void connectToServer(@NotNull Player player, @NotNull String command) {
        try {
            player.sendMessage(CC.translate("&aConnecting to server..."));
            
            // Replace {player} placeholder
            String finalCommand = command.replace("{player}", player.getName());
            
            // Execute command as the player in the correct region context
            SchedulerUtil.runTaskForEntity(Spook.getInstance(), player, () -> {
                try {
                    // PLAYER executes the command (not console!)
                    player.performCommand(finalCommand);
                    
                    Spook.getInstance().getLogger().info("Player " + player.getName() + " executed: /" + finalCommand);
                    
                } catch (Exception e) {
                    Spook.getInstance().getLogger().log(Level.WARNING, "Error executing server command: " + command, e);
                    player.sendMessage(CC.translate("&cFailed to connect to server!"));
                }
            });
            
        } catch (Exception e) {
            Spook.getInstance().getLogger().log(Level.WARNING, "Error connecting player to server", e);
            player.sendMessage(CC.translate("&cFailed to connect to server!"));
        }
    }
    
    private void playClickSound(@NotNull Player player) {
        try {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
        } catch (Exception e) {
            // Fallback for older versions
            try {
                player.playSound(player.getLocation(), Sound.valueOf("CLICK"), 0.5f, 1.0f);
            } catch (Exception ignored) {
                // No sound if both fail
            }
        }
    }
    
    private void playSuccessSound(@NotNull Player player) {
        try {
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
        } catch (Exception e) {
            // Fallback for older versions
            try {
                player.playSound(player.getLocation(), Sound.valueOf("ORB_PICKUP"), 0.7f, 1.2f);
            } catch (Exception ignored) {
                // No sound if both fail
            }
        }
    }
    
    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        // Clean up rate limiting data to prevent memory leaks
        lastInteraction.remove(event.getPlayer().getUniqueId());
    }
    
    /**
     * Send ultra-fast chat-based server list (eliminates GUI overhead completely)
     */
    private void sendServerList(@NotNull Player player) {
        ConfigurationSection serversConfig = Spook.getInstance().getConfig().getConfigurationSection("servers");
        
        if (serversConfig == null) {
            player.sendMessage(CC.translate("&cNo servers configured!"));
            return;
        }
        
        player.sendMessage(CC.translate("&8&l» &6&lServer Selection &8&l«"));
        player.sendMessage("");
        
        Set<String> serverNames = serversConfig.getKeys(false);
        for (String serverKey : serverNames) {
            ConfigurationSection serverSection = serversConfig.getConfigurationSection(serverKey);
            if (serverSection == null) continue;
            
            String displayName = CC.translate(serverSection.getString("name", "&a" + serverKey));
            String command = serverSection.getString("command", "server " + serverKey);
            
            // Store command for click handling
            serverCommands.put(displayName, command);
            
            // Send clickable message
            player.sendMessage(CC.translate("&8» &f" + displayName + " &8- &aClick to join"));
        }
        
        player.sendMessage("");
        player.sendMessage(CC.translate("&7Type server name to connect, or use the GUI with /servers"));
    }
    
    /**
     * Pre-initialize server commands for maximum performance
     * Call this during plugin startup
     */
    public void initializeGUICache() {
        // Just initialize the server commands, no GUI needed
        ConfigurationSection serversConfig = Spook.getInstance().getConfig().getConfigurationSection("servers");
        if (serversConfig != null) {
            Set<String> serverNames = serversConfig.getKeys(false);
            for (String serverKey : serverNames) {
                ConfigurationSection serverSection = serversConfig.getConfigurationSection(serverKey);
                if (serverSection != null) {
                    String displayName = CC.translate(serverSection.getString("name", "&a" + serverKey));
                    String command = serverSection.getString("command", "server " + serverKey);
                    serverCommands.put(displayName, command);
                }
            }
        }
        Spook.getInstance().getLogger().info("ServerSelector initialized with " + serverCommands.size() + " servers (chat-based)");
    }
    
    /**
     * Refresh GUI cache when config changes
     */
    public void refreshGUICache() {
        cachedGUI = null; // Clear cache to force recreation
        cachedGUI = createServerGUI();
        Spook.getInstance().getLogger().info("ServerSelector GUI cache refreshed");
    }
}
