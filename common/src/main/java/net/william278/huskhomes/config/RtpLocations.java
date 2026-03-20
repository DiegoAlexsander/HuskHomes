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

package net.william278.huskhomes.config;

import de.exlll.configlib.Configuration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.william278.huskhomes.random.RtpLocation;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * Configuration class that loads rtp-locations.yml and provides access to named RTP locations.
 */
@Getter
@Configuration
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RtpLocations {

    static final String CONFIG_HEADER = """
            ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓
            ┃    HuskHomes RTP Locations   ┃
            ┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛
            ┣╸ Define named RTP locations with custom center, radius, and distribution parameters.
            ┣╸ Each location specifies which server hosts its world (for cross-server RTP).
            ┗╸ This file should be identical across all servers in your network.""";

    private Map<String, RtpLocation> locations = new LinkedHashMap<>();

    @NotNull
    public Optional<RtpLocation> getLocation(@NotNull String name) {
        return locations.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    @NotNull
    public List<String> getLocationNames() {
        return new ArrayList<>(locations.keySet());
    }

    @NotNull
    public List<Map.Entry<String, RtpLocation>> getLocationsForServer(@NotNull String server) {
        return locations.entrySet().stream()
                .filter(e -> e.getValue().getServer().equalsIgnoreCase(server))
                .toList();
    }

}
