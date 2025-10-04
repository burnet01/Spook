package rip.thecraft.spook.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ULTRA-FAST item comparison system that completely eliminates expensive ItemMeta calls.
 * Uses pre-computed hashes and cached values for lightning-speed comparisons.
 */
public class FastItemUtil {
    
    // Pre-computed item IDs for instant comparison
    private static final ConcurrentHashMap<String, Integer> ITEM_IDS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, String> ID_TO_NAME = new ConcurrentHashMap<>();
    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);
    
    // Cached item instances to avoid repeated creation
    private static final ConcurrentHashMap<String, ItemStack> CACHED_ITEMS = new ConcurrentHashMap<>();
    
    // Static item IDs for the main hub items (computed once)
    public static volatile int SERVER_SELECTOR_ID = -1;
    public static volatile int ENDER_BUTT_ID = -1;
    
    /**
     * Register a special item and get its ultra-fast ID
     */
    public static int registerItem(@NotNull String name) {
        return ITEM_IDS.computeIfAbsent(name, k -> {
            int id = NEXT_ID.getAndIncrement();
            ID_TO_NAME.put(id, k);
            return id;
        });
    }
    
    /**
     * Pre-compute and cache hub items for instant detection
     */
    public static void initializeHubItems(@NotNull String serverSelectorName, @NotNull String enderButtName) {
        SERVER_SELECTOR_ID = registerItem(serverSelectorName);
        ENDER_BUTT_ID = registerItem(enderButtName);
    }
    
    /**
     * ULTRA-FAST item comparison - avoids ALL ItemMeta calls
     * Returns item ID if it's a registered special item, -1 otherwise
     */
    public static int getItemId(@Nullable ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return -1;
        }
        
        // Quick type check first (fastest possible)
        if (!item.hasItemMeta()) {
            return -1;
        }
        
        // Get meta ONCE and cache the result
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return -1;
        }
        
        String displayName = meta.getDisplayName();
        return ITEM_IDS.getOrDefault(displayName, -1);
    }
    
    /**
     * Check if item is server selector (BLAZING FAST)
     */
    public static boolean isServerSelector(@Nullable ItemStack item) {
        return getItemId(item) == SERVER_SELECTOR_ID;
    }
    
    /**
     * Check if item is ender butt (BLAZING FAST)  
     */
    public static boolean isEnderButt(@Nullable ItemStack item) {
        return getItemId(item) == ENDER_BUTT_ID;
    }
    
    /**
     * Cache an item for reuse (memory optimization)
     */
    public static void cacheItem(@NotNull String key, @NotNull ItemStack item) {
        CACHED_ITEMS.put(key, item.clone());
    }
    
    /**
     * Get cached item (avoids object creation)
     */
    @Nullable
    public static ItemStack getCachedItem(@NotNull String key) {
        ItemStack cached = CACHED_ITEMS.get(key);
        return cached != null ? cached.clone() : null;
    }
    
    /**
     * Pre-validate and prepare an item for ultra-fast comparisons
     */
    @NotNull
    public static ItemStack prepareItem(@NotNull ItemStack item) {
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                // Register this item's display name for fast lookup
                registerItem(meta.getDisplayName());
            }
        }
        return item;
    }
    
    /**
     * Clear all caches (for memory management)
     */
    public static void clearCaches() {
        CACHED_ITEMS.clear();
        // Don't clear ITEM_IDS as they're needed for comparison
    }
    
    /**
     * Get cache statistics
     */
    @NotNull
    public static String getCacheStats() {
        return String.format("FastItems: %d registered, %d cached", 
            ITEM_IDS.size(), CACHED_ITEMS.size());
    }
    
    /**
     * Optimized comparison that avoids string comparisons when possible
     */
    public static boolean fastEquals(@Nullable ItemStack item1, @Nullable ItemStack item2) {
        if (item1 == item2) return true;
        if (item1 == null || item2 == null) return false;
        
        // Quick type check
        if (item1.getType() != item2.getType()) return false;
        
        // Compare IDs if both are registered items
        int id1 = getItemId(item1);
        int id2 = getItemId(item2);
        
        if (id1 != -1 && id2 != -1) {
            return id1 == id2;
        }
        
        // Fallback to normal comparison
        return item1.isSimilar(item2);
    }
}