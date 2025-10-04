package rip.thecraft.spook.util;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Optimized ItemBuilder with proper null checks, caching, and thread safety.
 * Supports both Paper and Folia environments.
 */
public class ItemBuilder {
    
    private static final Logger LOGGER = Logger.getLogger("ItemBuilder");
    private static final ConcurrentHashMap<String, ItemStack> ITEM_CACHE = new ConcurrentHashMap<>();
    
    private final ItemStack itemStack;
    private ItemMeta cachedMeta;

    public ItemBuilder(@NotNull Material material, int amount) {
        if (material == Material.AIR) {
            throw new IllegalArgumentException("Cannot create ItemBuilder with AIR material");
        }
        this.itemStack = new ItemStack(material, Math.max(1, Math.min(amount, 64)));
        this.cachedMeta = itemStack.getItemMeta();
    }

    public ItemBuilder(@NotNull Material material) {
        this(material, 1);
    }

    @Deprecated
    public ItemBuilder(@NotNull Material material, int amount, short data) {
        this(material, amount);
        // Data values are deprecated in newer versions
        if (cachedMeta != null) {
            try {
                cachedMeta.setCustomModelData((int) data);
            } catch (Exception e) {
                // Silently ignore for compatibility
            }
        }
    }

    @NotNull
    public ItemBuilder setName(@Nullable String name) {
        if (cachedMeta != null) {
            cachedMeta.setDisplayName(name != null ? CC.translate(name) : null);
        }
        return this;
    }

    @NotNull
    public ItemBuilder setLore(@Nullable List<String> lore) {
        if (cachedMeta != null && lore != null) {
            List<String> translatedLore = lore.stream()
                    .map(line -> line != null ? CC.translate(line) : "")
                    .toList();
            cachedMeta.setLore(translatedLore);
        }
        return this;
    }
    
    @NotNull
    public ItemBuilder setLore(@NotNull String... lore) {
        return setLore(Arrays.asList(lore));
    }

    @NotNull
    public ItemBuilder addEnchant(@NotNull Enchantment enchantment, int level) {
        if (cachedMeta != null) {
            cachedMeta.addEnchant(enchantment, Math.max(1, level), true);
        }
        return this;
    }
    
    @NotNull
    public ItemBuilder addItemFlags(@NotNull ItemFlag... flags) {
        if (cachedMeta != null && flags.length > 0) {
            cachedMeta.addItemFlags(flags);
        }
        return this;
    }
    
    @NotNull
    public ItemBuilder hideAllFlags() {
        return addItemFlags(ItemFlag.values());
    }
    
    @NotNull
    public ItemBuilder setUnbreakable(boolean unbreakable) {
        if (cachedMeta != null) {
            cachedMeta.setUnbreakable(unbreakable);
        }
        return this;
    }
    
    @NotNull
    public ItemBuilder setCustomModelData(int data) {
        if (cachedMeta != null) {
            cachedMeta.setCustomModelData(data);
        }
        return this;
    }

    @NotNull
    public ItemBuilder setColor(@NotNull Color color) {
        if (cachedMeta instanceof LeatherArmorMeta leatherMeta) {
            leatherMeta.setColor(color);
        } else {
            LOGGER.warning("Attempted to set color on non-leather armor item: " + itemStack.getType());
        }
        return this;
    }
    
    @NotNull
    public ItemBuilder setAmount(int amount) {
        itemStack.setAmount(Math.max(1, Math.min(amount, itemStack.getMaxStackSize())));
        return this;
    }

    @NotNull
    public ItemStack create() {
        return create(false);
    }
    
    @NotNull
    public ItemStack create(boolean useCache) {
        // Apply cached meta if available
        if (cachedMeta != null) {
            itemStack.setItemMeta(cachedMeta);
        }
        
        if (useCache) {
            String cacheKey = generateCacheKey();
            return ITEM_CACHE.computeIfAbsent(cacheKey, k -> itemStack.clone());
        }
        
        return itemStack.clone();
    }
    
    @NotNull
    private String generateCacheKey() {
        StringBuilder key = new StringBuilder();
        key.append(itemStack.getType().name());
        key.append("_").append(itemStack.getAmount());
        
        if (cachedMeta != null) {
            if (cachedMeta.hasDisplayName()) {
                key.append("_name:").append(cachedMeta.getDisplayName());
            }
            if (cachedMeta.hasLore()) {
                key.append("_lore:").append(cachedMeta.getLore().hashCode());
            }
            if (cachedMeta.hasEnchants()) {
                key.append("_enchants:").append(cachedMeta.getEnchants().hashCode());
            }
        }
        
        return key.toString();
    }
    
    /**
     * Clear the item cache (useful for memory management)
     */
    public static void clearCache() {
        ITEM_CACHE.clear();
    }
    
    /**
     * Get cache size for monitoring
     */
    public static int getCacheSize() {
        return ITEM_CACHE.size();
    }
    
    /**
     * Create a quick item with just material and name
     */
    @NotNull
    public static ItemStack quickItem(@NotNull Material material, @Nullable String name) {
        return new ItemBuilder(material).setName(name).create();
    }
    
    /**
     * Create a quick item with material, name, and lore
     */
    @NotNull
    public static ItemStack quickItem(@NotNull Material material, @Nullable String name, @NotNull String... lore) {
        return new ItemBuilder(material).setName(name).setLore(lore).create();
    }
}
