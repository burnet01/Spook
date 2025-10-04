package rip.thecraft.spook.util;

import rip.thecraft.spook.Spook;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Advanced configuration manager with validation, caching, and hot-reload capabilities.
 * Thread-safe for Folia compatibility.
 */
public class ConfigManager {
    
    private final Spook plugin;
    private final ConcurrentHashMap<String, Object> configCache = new ConcurrentHashMap<>();
    private long lastReload = 0;
    private final long CACHE_REFRESH_INTERVAL = 30000; // 30 seconds
    
    // Configuration defaults
    private static final ConfigDefaults DEFAULTS = new ConfigDefaults();
    
    public ConfigManager(@NotNull Spook plugin) {
        this.plugin = plugin;
        validateAndSetDefaults();
    }
    
    /**
     * Validate configuration and set missing defaults
     */
    public boolean validateAndSetDefaults() {
        try {
            FileConfiguration config = plugin.getConfig();
            boolean modified = false;
            
            // Validate and set defaults for all required keys
            for (String key : DEFAULTS.getAllKeys()) {
                if (!config.contains(key)) {
                    Object defaultValue = DEFAULTS.getDefault(key);
                    config.set(key, defaultValue);
                    modified = true;
                    plugin.getLogger().info("Set missing config value: " + key + " = " + defaultValue);
                }
            }
            
            // Ensure servers section exists with at least one example
            if (!config.contains("servers") || config.getConfigurationSection("servers") == null) {
                // Create default lobby server
                config.set("servers.lobby.name", "&a&lMain Lobby");
                config.set("servers.lobby.material", "GRASS");
                config.set("servers.lobby.command", "server lobby");
                config.set("servers.lobby.lore", Arrays.asList(
                    "",
                    "&7• &fMain hub server",
                    "&7• &fSpawn point for all players",
                    "",
                    "&a» Click to join!"
                ));
                modified = true;
                plugin.getLogger().info("Created default servers section with lobby example");
            }
            
            // Validate material names
            modified |= validateMaterials(config);
            
            // Validate numeric values
            modified |= validateNumericValues(config);
            
            // Save config if modified
            if (modified) {
                plugin.saveConfig();
                plugin.getLogger().info("Configuration updated with missing defaults");
            }
            
            // Clear cache after validation
            configCache.clear();
            lastReload = System.currentTimeMillis();
            
            return true;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error validating configuration", e);
            return false;
        }
    }
    
    private boolean validateMaterials(@NotNull FileConfiguration config) {
        boolean modified = false;
        
        String[] materialKeys = {
            "ITEM.SERVER_SELECTOR.MATERIAL",
            "ITEM.ENDER_BUTT.MATERIAL",
            "GLASS_PANE.MATERIAL"
        };
        
        for (String key : materialKeys) {
            String materialName = config.getString(key);
            if (materialName != null) {
                if (!MaterialUtil.isValidMaterial(materialName)) {
                    // Set to default material
                    String defaultMaterial = DEFAULTS.getDefault(key, "STONE").toString();
                    config.set(key, defaultMaterial);
                    modified = true;
                    plugin.getLogger().warning("Invalid material '" + materialName + "' for key '" + key + "', set to default: " + defaultMaterial);
                }
            }
        }
        
        return modified;
    }
    
    private boolean validateNumericValues(@NotNull FileConfiguration config) {
        boolean modified = false;
        
        // Validate slots
        int[] slots = {
            config.getInt("ITEM.SERVER_SELECTOR.SLOT", 4),
            config.getInt("ITEM.ENDER_BUTT.SLOT", 3)
        };
        
        String[] slotKeys = {
            "ITEM.SERVER_SELECTOR.SLOT",
            "ITEM.ENDER_BUTT.SLOT"
        };
        
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] < 0 || slots[i] > 53) {
                int defaultSlot = (Integer) DEFAULTS.getDefault(slotKeys[i], 4);
                config.set(slotKeys[i], defaultSlot);
                modified = true;
                plugin.getLogger().warning("Invalid slot " + slots[i] + " for key '" + slotKeys[i] + "', set to default: " + defaultSlot);
            }
        }
        
        return modified;
    }
    
    /**
     * Get a cached configuration value
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCachedValue(@NotNull String key, @NotNull Class<T> type) {
        // Check if cache needs refresh
        if (System.currentTimeMillis() - lastReload > CACHE_REFRESH_INTERVAL) {
            refreshCache();
        }
        
        Object value = configCache.get(key);
        if (value != null && type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }
        
        // Get from config and cache it
        FileConfiguration config = plugin.getConfig();
        Object configValue = config.get(key);
        
        if (configValue != null) {
            configCache.put(key, configValue);
            if (type.isAssignableFrom(configValue.getClass())) {
                return (T) configValue;
            }
        }
        
        // Return default if available
        return (T) DEFAULTS.getDefault(key);
    }
    
    /**
     * Get a string value with color translation
     */
    @NotNull
    public String getTranslatedString(@NotNull String key, @NotNull String defaultValue) {
        String value = getCachedValue(key, String.class);
        return CC.translate(value != null ? value : defaultValue);
    }
    
    /**
     * Get an integer value with bounds checking
     */
    public int getBoundedInt(@NotNull String key, int defaultValue, int min, int max) {
        Integer value = getCachedValue(key, Integer.class);
        int result = value != null ? value : defaultValue;
        return Math.max(min, Math.min(max, result));
    }
    
    /**
     * Get a material with fallback to default
     */
    @NotNull
    public Material getMaterial(@NotNull String key, @NotNull Material defaultMaterial) {
        String materialName = getCachedValue(key, String.class);
        if (materialName != null) {
            return MaterialUtil.getMaterial(materialName, defaultMaterial);
        }
        return defaultMaterial;
    }
    
    /**
     * Reload configuration and clear caches
     */
    public boolean reloadConfiguration() {
        try {
            plugin.reloadConfig();
            
            // Clear all caches
            clearAllCaches();
            
            // Validate configuration
            boolean valid = validateAndSetDefaults();
            
            plugin.getLogger().info("Configuration reloaded successfully");
            return valid;
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error reloading configuration", e);
            return false;
        }
    }
    
    /**
     * Clear all caches throughout the plugin
     */
    public void clearAllCaches() {
        configCache.clear();
        
        // Clear ItemBuilder cache
        ItemBuilder.clearCache();
        
        // Clear caches from other components if they exist
        lastReload = System.currentTimeMillis();
        
        plugin.getLogger().info("All plugin caches cleared");
    }
    
    private void refreshCache() {
        configCache.clear();
        lastReload = System.currentTimeMillis();
    }
    
    /**
     * Get cache statistics for monitoring
     */
    @NotNull
    public String getCacheStats() {
        return String.format("ConfigCache: %d entries, ItemCache: %d entries", 
            configCache.size(), ItemBuilder.getCacheSize());
    }
    
    /**
     * Configuration defaults storage
     */
    private static class ConfigDefaults {
        private final ConcurrentHashMap<String, Object> defaults = new ConcurrentHashMap<>();
        
        ConfigDefaults() {
            // Item configurations
            defaults.put("ITEM.SERVER_SELECTOR.MATERIAL", "BOOK");
            defaults.put("ITEM.SERVER_SELECTOR.SLOT", 4);
            defaults.put("ITEM.SERVER_SELECTOR.NAME", "&6Servers");
            
            defaults.put("ITEM.ENDER_BUTT.MATERIAL", "ENDER_PEARL");
            defaults.put("ITEM.ENDER_BUTT.SLOT", 3);
            defaults.put("ITEM.ENDER_BUTT.NAME", "&bEnderButt");
            defaults.put("ITEM.ENDER_BUTT.SOUND", "ENTITY_ENDERMAN_TELEPORT");
            
            // Glass pane configuration
            defaults.put("GLASS_PANE.MATERIAL", "STAINED_GLASS_PANE");
            defaults.put("GLASS_PANE.NAME", " ");
            defaults.put("GLASS_PANE.AMOUNT", 1);
            defaults.put("GLASS_PANE.VALUE", 0);
            
            // Simple servers configuration (examples)
            defaults.put("servers.lobby.name", "&a&lMain Lobby");
            defaults.put("servers.lobby.material", "GRASS");
            defaults.put("servers.lobby.command", "server lobby");
            defaults.put("servers.lobby.lore", Arrays.asList(
                "",
                "&7• &fMain hub server",
                "&7• &fSpawn point for all players",
                "",
                "&a» Click to join!"
            ));
            
            // Settings
            defaults.put("SETTINGS.ALWAYS_SUNNY", true);
        }
        
        @NotNull
        String[] getAllKeys() {
            return defaults.keySet().toArray(new String[0]);
        }
        
        @Nullable
        Object getDefault(@NotNull String key) {
            return defaults.get(key);
        }
        
        @NotNull
        Object getDefault(@NotNull String key, @NotNull Object fallback) {
            return defaults.getOrDefault(key, fallback);
        }
    }
}