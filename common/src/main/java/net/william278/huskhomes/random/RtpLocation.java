/*
 * This file is part of HuskHomes, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.huskhomes.random;

import de.exlll.configlib.Configuration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Represents a named RTP location with custom center, radius, and distribution parameters.
 * Supports both circular (CIRCLE) and rectangular (RECTANGLE) area shapes.
 * <p>
 * Shape-specific and optional fields use nullable boxed types so that configlib only writes
 * them to YAML when they are explicitly set (e.g. CIRCLE locations won't show min_x/max_x fields).
 */
@Configuration
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RtpLocation {

    /**
     * The shape of the RTP area.
     * CIRCLE uses center_x/center_z + min_radius/max_radius (default behaviour).
     * RECTANGLE uses min_x/max_x/min_z/max_z absolute coordinates.
     */
    public enum Shape {
        CIRCLE,
        RECTANGLE
    }

    @Getter private String server = "";
    @Getter private String world = "world";

    /** Shape of the RTP area. Defaults to CIRCLE for backwards compatibility. */
    @Getter private Shape shape = Shape.CIRCLE;

    // --- CIRCLE mode fields (null when not applicable) ---
    private Double centerX;
    private Double centerZ;
    private Integer minRadius;
    private Integer maxRadius;

    // --- RECTANGLE mode fields (null when not applicable) ---
    private Double minX;
    private Double maxX;
    private Double minZ;
    private Double maxZ;

    // --- Optional distribution overrides (null = use global config values) ---
    private Float distributionMean;
    private Float distributionStandardDeviation;

    // Manual getters returning primitives with sensible defaults

    public double getCenterX() { return centerX != null ? centerX : 0.0; }
    public double getCenterZ() { return centerZ != null ? centerZ : 0.0; }
    public int getMinRadius() { return minRadius != null ? minRadius : 500; }
    public int getMaxRadius() { return maxRadius != null ? maxRadius : 5000; }

    public double getMinX() { return minX != null ? minX : 0.0; }
    public double getMaxX() { return maxX != null ? maxX : 0.0; }
    public double getMinZ() { return minZ != null ? minZ : 0.0; }
    public double getMaxZ() { return maxZ != null ? maxZ : 0.0; }

    public float getDistributionMean() { return distributionMean != null ? distributionMean : 0f; }
    public float getDistributionStandardDeviation() { return distributionStandardDeviation != null ? distributionStandardDeviation : 0f; }

    /** Constructor for CIRCLE mode (backwards compatible). */
    public RtpLocation(String server, String world, double centerX, double centerZ,
                       int minRadius, int maxRadius, float distributionMean, float distributionStdDev) {
        this.server = server;
        this.world = world;
        this.shape = Shape.CIRCLE;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        if (distributionMean > 0) this.distributionMean = distributionMean;
        if (distributionStdDev > 0) this.distributionStandardDeviation = distributionStdDev;
    }

    /** Constructor for RECTANGLE mode. */
    public RtpLocation(String server, String world,
                       double minX, double maxX, double minZ, double maxZ,
                       float distributionMean, float distributionStdDev) {
        this.server = server;
        this.world = world;
        this.shape = Shape.RECTANGLE;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        if (distributionMean > 0) this.distributionMean = distributionMean;
        if (distributionStdDev > 0) this.distributionStandardDeviation = distributionStdDev;
    }

    public boolean isRectangle() {
        return shape == Shape.RECTANGLE;
    }

}
