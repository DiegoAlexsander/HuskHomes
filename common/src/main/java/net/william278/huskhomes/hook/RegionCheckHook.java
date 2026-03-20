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

package net.william278.huskhomes.hook;

import net.william278.huskhomes.position.Location;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for hooks that check whether a location is allowed for RTP
 * (e.g., not inside a protected region, claim, town, etc.)
 */
public interface RegionCheckHook {

    /**
     * Check whether the given location is allowed for random teleportation.
     *
     * @param location the HuskHomes location to check
     * @return true if the location is allowed, false if it should be rejected
     */
    boolean isLocationAllowed(@NotNull Location location);

}
