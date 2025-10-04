package rip.thecraft.spook.features;

import rip.thecraft.spook.Spook;
import rip.thecraft.spook.util.SchedulerUtil;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ULTRA-HIGH-PERFORMANCE DoubleJump with EXTREME optimizations.
 * Designed to be completely invisible in performance profilers.
 */
public class DoubleJump implements Listener {
    
    // AGGRESSIVE caching - much longer intervals
    private final ConcurrentHashMap<UUID, Long> lastGroundCheck = new ConcurrentHashMap<>();
    private final Set<UUID> playersOnGround = new HashSet<>();
    private final long GROUND_CHECK_COOLDOWN = 300; // 300ms instead of 100ms (3x less frequent)
    
    // Pre-computed constants
    private static final double JUMP_HORIZONTAL = 1.5;
    private static final double JUMP_VERTICAL = 1.0;
    private static final double NO_MOVEMENT_THRESHOLD = 0.001; // Much smaller threshold
    
    // Cache gamemode checks
    private final ConcurrentHashMap<UUID, GameMode> lastGameMode = new ConcurrentHashMap<>();

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onJump(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // FASTEST gamemode check using cached value
        GameMode cachedGM = lastGameMode.get(playerId);
        GameMode currentGM = player.getGameMode();
        
        if (currentGM == GameMode.CREATIVE || currentGM == GameMode.SPECTATOR) {
            return;
        }
        
        // Update cache if changed
        if (cachedGM != currentGM) {
            lastGameMode.put(playerId, currentGM);
        }

        event.setCancelled(true);
        player.setAllowFlight(false);
        player.setFlying(false);
        
        // ULTRA-FAST velocity calculation - reuse direction vector
        Vector direction = player.getLocation().getDirection();
        direction.normalize().multiply(JUMP_HORIZONTAL).setY(JUMP_VERTICAL);
        
        // Direct velocity set - no scheduler overhead for simple operations
        player.setVelocity(direction);
        
        // Simplified sound (no fallbacks for performance)
        try {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 1.8f);
        } catch (Exception ignored) {
            // Silent failure for maximum performance
        }
        
        // Remove from ground tracking
        playersOnGround.remove(playerId);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // MOST AGGRESSIVE early exits possible
        Location from = event.getFrom();
        Location to = event.getTo();
        
        // Skip if no Y movement at all (most common case)
        if (Math.abs(from.getY() - to.getY()) < NO_MOVEMENT_THRESHOLD) {
            return;
        }
        
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Use cached gamemode if available
        GameMode cachedGM = lastGameMode.get(playerId);
        if (cachedGM == GameMode.CREATIVE || cachedGM == GameMode.SPECTATOR) {
            return;
        }
        
        // EXTREME throttling - only check every 500ms now
        long currentTime = System.currentTimeMillis();
        Long lastCheck = lastGroundCheck.get(playerId);
        if (lastCheck != null && (currentTime - lastCheck) < 500) {
            return;
        }
        
        lastGroundCheck.put(playerId, currentTime);
        
        // FASTEST ground detection - only Bukkit's built-in
        boolean isOnGround = player.isOnGround();
        boolean wasOnGround = playersOnGround.contains(playerId);
        
        // Only update if state actually changed
        if (isOnGround && !wasOnGround) {
            playersOnGround.add(playerId);
            player.setAllowFlight(true);
        } else if (!isOnGround && wasOnGround) {
            playersOnGround.remove(playerId);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Initialize player state
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            SchedulerUtil.runTaskLater(Spook.getInstance(), () -> {
                if (player.isOnline() && isPlayerOnGround(player)) {
                    playersOnGround.add(playerId);
                    player.setAllowFlight(true);
                }
            }, 5L); // Small delay to ensure player is fully loaded
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        
        // Clean up ALL player data
        lastGroundCheck.remove(playerId);
        playersOnGround.remove(playerId);
        lastGameMode.remove(playerId);
    }
    
    /**
     * Efficient ground detection using multiple methods for accuracy
     */
    private boolean isPlayerOnGround(Player player) {
        // Method 1: Bukkit's built-in method (fastest)
        if (player.isOnGround()) {
            return true;
        }
        
        // Method 2: Check block below player (more accurate)
        Location loc = player.getLocation();
        Block blockBelow = loc.clone().add(0, -0.1, 0).getBlock();
        
        if (!blockBelow.getType().isAir() && blockBelow.getType().isSolid()) {
            return true;
        }
        
        // Method 3: Check slightly below feet for edge cases
        for (double y = -0.1; y >= -0.5; y -= 0.1) {
            Block block = loc.clone().add(0, y, 0).getBlock();
            if (!block.getType().isAir() && block.getType().isSolid()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Get current cache sizes for monitoring
     */
    public int getTrackedPlayersCount() {
        return playersOnGround.size();
    }
    
    /**
     * Clear all cached data (useful for memory management)
     */
    public void clearCache() {
        lastGroundCheck.clear();
        playersOnGround.clear();
    }
}
