package rip.thecraft.spook.features;

import rip.thecraft.spook.Spook;
import rip.thecraft.spook.util.CC;
import rip.thecraft.spook.util.FastItemUtil;
import rip.thecraft.spook.util.SchedulerUtil;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

import static org.bukkit.Bukkit.getName;

/**
 * Optimized EnderButt feature with proper error handling and thread safety.
 * Compatible with both Folia and Paper.
 */
public class EnderButt implements Listener {

    private final double LAUNCH_MULTIPLIER = 2.5;
    private final float SOUND_VOLUME = 1.0f;
    private final float SOUND_PITCH = 1.2f;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(@NotNull PlayerInteractEvent event) {
        // ULTRA FAST: Single line check with immediate return
        if (!FastItemUtil.isEnderButt(event.getItem())) return;
        
        event.setCancelled(true);
        event.setUseItemInHand(Event.Result.DENY);
        
        Player player = event.getPlayer();
        
        // Direct calls in scheduler - minimal overhead
        SchedulerUtil.runTaskForEntity(Spook.getInstance(), player, () -> {
            launchPlayer(player);
            playLaunchSound(player);
        });
    }
    
    private void launchPlayer(@NotNull Player player) {
        try {
            Vector direction = player.getLocation().getDirection();
            Vector velocity = direction.normalize().multiply(LAUNCH_MULTIPLIER);
            
            // Add some upward momentum
            velocity.setY(Math.max(velocity.getY(), 0.5));
            
            player.setVelocity(velocity);
            
        } catch (Exception e) {
            Spook.getInstance().getLogger().log(Level.WARNING, "Error launching player " + player.getName(), e);
        }
    }
    
    private void playLaunchSound(@NotNull Player player) {
        try {
            String soundName = Spook.getInstance().getConfig().getString("ITEM.ENDER_BUTT.SOUND", "ENTITY_ENDERMAN_TELEPORT");
            
            // Try modern sound first
            try {
                Sound sound = Sound.valueOf(soundName.toUpperCase());
                player.playSound(player.getLocation(), sound, SOUND_VOLUME, SOUND_PITCH);
            } catch (IllegalArgumentException e) {
                // Fallback sounds for different versions
                try {
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SOUND_VOLUME, SOUND_PITCH);
                } catch (Exception e2) {
                    try {
                        player.playSound(player.getLocation(), Sound.valueOf("ENDERMAN_TELEPORT"), SOUND_VOLUME, SOUND_PITCH);
                    } catch (Exception e3) {
                        // No sound if all fail
                        Spook.getInstance().getLogger().warning("Could not play EnderButt sound: " + soundName);
                    }
                }
            }
            
        } catch (Exception e) {
            Spook.getInstance().getLogger().log(Level.WARNING, "Error playing EnderButt sound for " + player.getName(), e);
        }
    }
}
