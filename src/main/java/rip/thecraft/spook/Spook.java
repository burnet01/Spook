package rip.thecraft.spook;

import rip.thecraft.spook.features.*;
import rip.thecraft.spook.server.*;
import rip.thecraft.spook.util.CC;
import rip.thecraft.spook.util.ConfigManager;
import rip.thecraft.spook.util.FastItemUtil;
import rip.thecraft.spook.util.ItemBuilder;
import rip.thecraft.spook.util.MaterialUtil;
import rip.thecraft.spook.util.SchedulerUtil;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.plugin.java.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;

@Getter
public class Spook extends JavaPlugin {
    
    private static volatile Spook instance;
    private boolean isShuttingDown = false;
    private ConfigManager configManager;
    
    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        try {
            // Log server type for debugging
            SchedulerUtil.logSchedulerInfo();
            
            // Initialize configuration with validation
            this.saveDefaultConfig();
            this.reloadConfig();
            
            // Initialize configuration manager
            this.configManager = new ConfigManager(this);
            
            // Validate and set defaults - this will create missing config sections
            if (!configManager.validateAndSetDefaults()) {
                getLogger().warning("Some configuration validation failed, but plugin will continue with defaults.");
            }
            
            // Save config after setting defaults
            this.saveConfig();
            
            // Reload to ensure we have the latest config
            this.reloadConfig();
            
            // Initialize ULTRA-FAST item system
            initializeFastItems();
            
            // Setup event listeners
            this.setupListeners();
            
            // Start periodic cleanup task
            this.startPeriodicTasks();
            
            getLogger().info("HubCore has been enabled successfully on " + SchedulerUtil.getServerType());
            getLogger().info("Material compatibility: " + MaterialUtil.getVersionInfo());
            getLogger().info("Cache statistics: " + configManager.getCacheStats());
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable HubCore", e);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }
    
    @Override
    public void onDisable() {
        isShuttingDown = true;
        
        // Clear all caches on shutdown
        if (configManager != null) {
            configManager.clearAllCaches();
        }
        
        getLogger().info("HubCore has been disabled.");
    }

    private void setupListeners() {
        // Create ServerSelector separately to initialize its GUI cache
        ServerSelector serverSelector = new ServerSelector();
        
        Arrays.asList(
                new PlayerListener(),
                serverSelector,
                new AlwaysDay(),
                new EnderButt(),
                new DoubleJump()
        ).forEach(listener -> {
            try {
                Bukkit.getPluginManager().registerEvents(listener, this);
                getLogger().info("Registered listener: " + listener.getClass().getSimpleName());
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Failed to register listener: " + listener.getClass().getSimpleName(), e);
            }
        });
        
        // Initialize ServerSelector GUI cache for maximum performance
        try {
            serverSelector.initializeGUICache();
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to initialize ServerSelector GUI cache", e);
        }
    }

    @NotNull
    public static Spook getInstance() {
        Spook result = instance;
        if (result == null) {
            synchronized (Spook.class) {
                result = instance;
                if (result == null) {
                    throw new IllegalStateException("HubCore plugin is not initialized!");
                }
            }
        }
        return result;
    }
    
    @Nullable
    public static Spook get() {
        return instance;
    }
    
    public boolean isShuttingDown() {
        return isShuttingDown;
    }
    
    @NotNull
    public ConfigManager getConfigManager() {
        if (configManager == null) {
            throw new IllegalStateException("ConfigManager not initialized yet!");
        }
        return configManager;
    }
    
    private void startPeriodicTasks() {
        // Start cache cleanup task every 5 minutes
        SchedulerUtil.runTaskTimer(this, () -> {
            if (!isShuttingDown && configManager != null) {
                try {
                    // Clean up ItemBuilder cache
                    ItemBuilder.clearCache();
                    
                    // Log cache statistics periodically
                    if (System.currentTimeMillis() % 300000 < 20000) { // Every 5 minutes
                        getLogger().info("Cache statistics: " + configManager.getCacheStats());
                    }
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Error during periodic cleanup", e);
                }
            }
        }, 6000L, 6000L); // 5 minutes delay, 5 minutes period
    }
    
    private void initializeFastItems() {
        try {
            // Get item names from config and initialize ultra-fast comparison system
            String serverSelectorName = CC.translate(getConfig().getString("ITEM.SERVER_SELECTOR.NAME", "&6Servers"));
            String enderButtName = CC.translate(getConfig().getString("ITEM.ENDER_BUTT.NAME", "&bEnderButt"));
            
            // Initialize the blazing-fast item system
            FastItemUtil.initializeHubItems(serverSelectorName, enderButtName);
            
            getLogger().info("Initialized ultra-fast item system: " + FastItemUtil.getCacheStats());
            
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error initializing fast item system", e);
        }
    }

}
