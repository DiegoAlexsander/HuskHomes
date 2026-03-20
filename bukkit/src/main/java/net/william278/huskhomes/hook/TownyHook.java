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

import com.palmergames.bukkit.towny.TownyAPI;
import net.william278.huskhomes.BukkitHuskHomes;
import net.william278.huskhomes.HuskHomes;
import net.william278.huskhomes.position.Location;
import org.jetbrains.annotations.NotNull;

@PluginHook(name = "Towny", register = PluginHook.Register.ON_ENABLE)
public class TownyHook extends Hook implements RegionCheckHook {

    public TownyHook(@NotNull HuskHomes plugin) {
        super(plugin);
    }

    @Override
    public void load() {
        plugin.log(java.util.logging.Level.INFO, "Towny hook enabled - RTP will avoid claimed areas");
    }

    @Override
    public void unload() {
    }

    @Override
    public boolean isLocationAllowed(@NotNull Location location) {
        final org.bukkit.Location bukkitLoc = BukkitHuskHomes.Adapter.adapt(location);
        if (bukkitLoc == null || bukkitLoc.getWorld() == null) {
            return true;
        }
        return TownyAPI.getInstance().isWilderness(bukkitLoc);
    }
}
