package rip.thecraft.spook.util;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Material compatibility utility for supporting multiple Minecraft versions.
 * Handles the differences between legacy (1.12-) and modern (1.13+) materials.
 */
public class MaterialUtil {
    
    private static final Map<String, String> LEGACY_MATERIALS = new HashMap<>();
    
    static {
        // Modern -> Legacy material mappings
        LEGACY_MATERIALS.put("GRAY_STAINED_GLASS_PANE", "STAINED_GLASS_PANE");
        LEGACY_MATERIALS.put("WHITE_STAINED_GLASS_PANE", "STAINED_GLASS_PANE");
        LEGACY_MATERIALS.put("BLACK_STAINED_GLASS_PANE", "STAINED_GLASS_PANE");
        LEGACY_MATERIALS.put("RED_STAINED_GLASS_PANE", "STAINED_GLASS_PANE");
        LEGACY_MATERIALS.put("GREEN_STAINED_GLASS_PANE", "STAINED_GLASS_PANE");
        LEGACY_MATERIALS.put("BLUE_STAINED_GLASS_PANE", "STAINED_GLASS_PANE");
        LEGACY_MATERIALS.put("YELLOW_STAINED_GLASS_PANE", "STAINED_GLASS_PANE");
        LEGACY_MATERIALS.put("ORANGE_STAINED_GLASS_PANE", "STAINED_GLASS_PANE");
        LEGACY_MATERIALS.put("PURPLE_STAINED_GLASS_PANE", "STAINED_GLASS_PANE");
        
        LEGACY_MATERIALS.put("OAK_PLANKS", "WOOD");
        LEGACY_MATERIALS.put("BIRCH_PLANKS", "WOOD");
        LEGACY_MATERIALS.put("SPRUCE_PLANKS", "WOOD");
        LEGACY_MATERIALS.put("JUNGLE_PLANKS", "WOOD");
        LEGACY_MATERIALS.put("ACACIA_PLANKS", "WOOD");
        LEGACY_MATERIALS.put("DARK_OAK_PLANKS", "WOOD");
        
        LEGACY_MATERIALS.put("GOLDEN_PICKAXE", "GOLD_PICKAXE");
        LEGACY_MATERIALS.put("GOLDEN_AXE", "GOLD_AXE");
        LEGACY_MATERIALS.put("GOLDEN_SWORD", "GOLD_SWORD");
        LEGACY_MATERIALS.put("GOLDEN_SHOVEL", "GOLD_SPADE");
        LEGACY_MATERIALS.put("GOLDEN_HOE", "GOLD_HOE");
        
        LEGACY_MATERIALS.put("GRASS_BLOCK", "GRASS");
        LEGACY_MATERIALS.put("DIRT", "DIRT");
        LEGACY_MATERIALS.put("COBBLESTONE", "COBBLESTONE");
        
        // Add more mappings as needed
    }
    
    /**
     * Get a material that works across different Minecraft versions
     */
    @NotNull
    public static Material getMaterial(@NotNull String materialName) {
        return getMaterial(materialName, Material.STONE);
    }
    
    /**
     * Get a material with a fallback default
     */
    @NotNull
    public static Material getMaterial(@NotNull String materialName, @NotNull Material fallback) {
        try {
            // First try the exact material name
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Try legacy mapping
            String legacyName = LEGACY_MATERIALS.get(materialName.toUpperCase());
            if (legacyName != null) {
                try {
                    return Material.valueOf(legacyName);
                } catch (IllegalArgumentException e2) {
                    // Still doesn't work, return fallback
                    return fallback;
                }
            }
            // No mapping found, return fallback
            return fallback;
        }
    }
    
    /**
     * Get a safe glass pane material that works on any version
     */
    @NotNull
    public static Material getGlassPane() {
        // Try modern first, then legacy, then ultimate fallback
        Material material = getMaterial("GRAY_STAINED_GLASS_PANE");
        if (material != Material.STONE) { // If we got something other than fallback
            return material;
        }
        
        // Try legacy stained glass pane
        material = getMaterial("STAINED_GLASS_PANE");
        if (material != Material.STONE) {
            return material;
        }
        
        // Ultimate fallback for very old versions
        return Material.GLASS;
    }
    
    /**
     * Get a safe wood material that works on any version
     */
    @NotNull
    public static Material getWoodMaterial() {
        return getMaterial("OAK_PLANKS", Material.OAK_WOOD);
    }
    
    /**
     * Check if a material name is valid for the current version
     */
    public static boolean isValidMaterial(@NotNull String materialName) {
        try {
            Material.valueOf(materialName.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            String legacyName = LEGACY_MATERIALS.get(materialName.toUpperCase());
            if (legacyName != null) {
                try {
                    Material.valueOf(legacyName);
                    return true;
                } catch (IllegalArgumentException e2) {
                    return false;
                }
            }
            return false;
        }
    }
    
    /**
     * Get the actual material name that exists on this server version
     */
    @NotNull
    public static String getValidMaterialName(@NotNull String materialName) {
        try {
            Material.valueOf(materialName.toUpperCase());
            return materialName.toUpperCase();
        } catch (IllegalArgumentException e) {
            String legacyName = LEGACY_MATERIALS.get(materialName.toUpperCase());
            if (legacyName != null) {
                try {
                    Material.valueOf(legacyName);
                    return legacyName;
                } catch (IllegalArgumentException e2) {
                    return "STONE"; // Ultimate fallback
                }
            }
            return "STONE"; // Ultimate fallback
        }
    }
    
    /**
     * Get server version info for debugging
     */
    @NotNull
    public static String getVersionInfo() {
        try {
            // Try to detect version by checking for modern materials
            Material.valueOf("GRAY_STAINED_GLASS_PANE");
            return "Modern (1.13+)";
        } catch (IllegalArgumentException e) {
            return "Legacy (1.12-)";
        }
    }
}