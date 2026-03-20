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
 */
@Getter
@Configuration
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RtpLocation {

    private String server = "";
    private String world = "world";
    private double centerX = 0;
    private double centerZ = 0;
    private int minRadius = 500;
    private int maxRadius = 5000;
    private float distributionMean = 0;
    private float distributionStandardDeviation = 0;

    public RtpLocation(String server, String world, double centerX, double centerZ,
                       int minRadius, int maxRadius, float distributionMean, float distributionStdDev) {
        this.server = server;
        this.world = world;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        this.distributionMean = distributionMean;
        this.distributionStandardDeviation = distributionStdDev;
    }

}
