package pl.openmc.paper.core.models;

import org.bukkit.Color;
import org.bukkit.Particle;

/**
 * Represents different types of wing particles.
 */
public enum WingParticle {
    VAMPIRE(Particle.REDSTONE, Color.fromBGR(255, 153, 209), 0.5F),
    ANGEL(Particle.END_ROD, null, 0),
    DEMON(Particle.FLAME, null, 0),
    FAIRY(Particle.SPELL_MOB, null, 0);

    private final Particle particleType;
    private final Color color;
    private final float size;

    /**
     * Creates a new WingParticle.
     *
     * @param particleType The Bukkit particle type
     * @param color The color for colored particles (null if not applicable)
     * @param size The size for sized particles (0 if not applicable)
     */
    WingParticle(Particle particleType, Color color, float size) {
        this.particleType = particleType;
        this.color = color;
        this.size = size;
    }

    /**
     * Gets the particle type.
     *
     * @return The Bukkit particle type
     */
    public Particle getParticleType() {
        return particleType;
    }

    /**
     * Gets the particle color.
     *
     * @return The color for colored particles (null if not applicable)
     */
    public Color getColor() {
        return color;
    }

    /**
     * Gets the particle size.
     *
     * @return The size for sized particles (0 if not applicable)
     */
    public float getSize() {
        return size;
    }

    /**
     * Checks if this particle type uses color.
     *
     * @return True if this particle type uses color
     */
    public boolean usesColor() {
        return color != null;
    }
}