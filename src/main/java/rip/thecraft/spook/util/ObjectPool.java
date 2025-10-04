package rip.thecraft.spook.util;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * High-performance object pooling system to reduce garbage collection pressure.
 * Pre-allocates and reuses frequently created objects.
 */
public class ObjectPool {
    
    // Vector pool for EnderButt and DoubleJump
    private static final ConcurrentLinkedQueue<Vector> VECTOR_POOL = new ConcurrentLinkedQueue<>();
    private static final int MAX_POOL_SIZE = 50;
    
    // Pre-fill pools
    static {
        // Pre-fill vector pool
        for (int i = 0; i < 20; i++) {
            VECTOR_POOL.offer(new Vector());
        }
    }
    
    /**
     * Get a pooled Vector instance (for velocity calculations)
     */
    @NotNull
    public static Vector getVector() {
        Vector vector = VECTOR_POOL.poll();
        if (vector == null) {
            vector = new Vector();
        } else {
            vector.setX(0).setY(0).setZ(0); // Reset to zero
        }
        return vector;
    }
    
    /**
     * Return a Vector to the pool for reuse
     */
    public static void returnVector(@NotNull Vector vector) {
        if (VECTOR_POOL.size() < MAX_POOL_SIZE) {
            VECTOR_POOL.offer(vector);
        }
    }
    
    /**
     * Get pool statistics for monitoring
     */
    @NotNull
    public static String getPoolStats() {
        return String.format("ObjectPool: %d vectors available", VECTOR_POOL.size());
    }
    
    /**
     * Clear all pools (for shutdown)
     */
    public static void clearPools() {
        VECTOR_POOL.clear();
    }
}