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

package net.william278.huskhomes.command;

import net.william278.huskhomes.HuskHomes;
import net.william278.huskhomes.network.Broker;
import net.william278.huskhomes.network.Message;
import net.william278.huskhomes.network.Payload;
import net.william278.huskhomes.position.World;
import net.william278.huskhomes.random.RtpLocation;
import net.william278.huskhomes.teleport.Teleport;
import net.william278.huskhomes.teleport.TeleportBuilder;
import net.william278.huskhomes.user.CommandUser;
import net.william278.huskhomes.user.OnlineUser;
import net.william278.huskhomes.util.TransactionResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class RtpCommand extends Command implements UserListTabCompletable {

    private final Random random = new Random();

    protected RtpCommand(@NotNull HuskHomes plugin) {
        super(
                List.of("rtp"),
                "[player] [<world> [server]|<world>|location <name>]",
                plugin
        );

        addAdditionalPermissions(Map.of(
                "other", true,
                "location", true
        ));
    }

    @Override
    public void execute(@NotNull CommandUser executor, @NotNull String[] args) {
        // Check for "location" subcommand: /rtp location <name> [player]
        if (args.length >= 1 && args[0].equalsIgnoreCase("location")) {
            executeLocationRtp(executor, args);
            return;
        }

        final Optional<OnlineUser> optionalTeleporter = args.length >= 1 ? plugin.getOnlineUser(args[0])
                : executor instanceof OnlineUser ? Optional.of((OnlineUser) executor) : Optional.empty();
        if (optionalTeleporter.isEmpty()) {
            if (args.length == 0) {
                plugin.getLocales().getLocale("error_invalid_syntax", getUsage())
                        .ifPresent(executor::sendMessage);
                return;
            }

            // Could also be "location" keyword with missing name
            plugin.getLocales().getLocale("error_player_not_found", args[0])
                    .ifPresent(executor::sendMessage);
            return;
        }

        final OnlineUser teleporter = optionalTeleporter.get();

        // Check if the second arg is "location": /rtp <player> location <name>
        if (args.length >= 2 && args[1].equalsIgnoreCase("location")) {
            executeLocationRtpForPlayer(teleporter, executor, args);
            return;
        }

        // Determine the target world and server based on the command arguments
        String worldName = teleporter.getPosition().getWorld().getName();
        String targetServer = null;

        if (args.length == 2) {
            // If there's only one argument after the player name, it could be either a world or a server
            if (plugin.getSettings().getRtp().getRandomTargetServers().containsKey(args[1])) {
                targetServer = args[1];
                worldName = teleporter.getPosition().getWorld().getName();
            } else {
                worldName = args[1];
            }
        } else if (args.length > 2) {
            // If two arguments are provided after the player name, treat them as world and server
            worldName = args[1];
            targetServer = args[2];
        }

        // Validate world and server, and execute RTP
        validateRtp(teleporter, executor, worldName.replace("minecraft:", ""), targetServer)
                .ifPresent(entry -> executeRtp(teleporter, executor, entry.getKey(), entry.getValue(), args));
    }

    /**
     * Handles /rtp location &lt;name&gt; [player]
     */
    private void executeLocationRtp(@NotNull CommandUser executor, @NotNull String[] args) {
        if (args.length < 2) {
            plugin.getLocales().getLocale("error_invalid_syntax", "/rtp location <name> [player]")
                    .ifPresent(executor::sendMessage);
            return;
        }

        // Check permission
        if (!executor.hasPermission(getPermission("location"))) {
            plugin.getLocales().getLocale("error_no_permission")
                    .ifPresent(executor::sendMessage);
            return;
        }

        final String locationName = args[1];

        // Determine the teleporter
        final OnlineUser teleporter;
        if (args.length >= 3) {
            final Optional<OnlineUser> optionalTarget = plugin.getOnlineUser(args[2]);
            if (optionalTarget.isEmpty()) {
                plugin.getLocales().getLocale("error_player_not_found", args[2])
                        .ifPresent(executor::sendMessage);
                return;
            }
            if (!executor.hasPermission(getPermission("other"))) {
                plugin.getLocales().getLocale("error_no_permission")
                        .ifPresent(executor::sendMessage);
                return;
            }
            teleporter = optionalTarget.get();
        } else {
            if (!(executor instanceof OnlineUser)) {
                plugin.getLocales().getLocale("error_in_game_only")
                        .ifPresent(executor::sendMessage);
                return;
            }
            teleporter = (OnlineUser) executor;
        }

        performLocationRtp(teleporter, executor, locationName);
    }

    /**
     * Handles /rtp &lt;player&gt; location &lt;name&gt;
     */
    private void executeLocationRtpForPlayer(@NotNull OnlineUser teleporter, @NotNull CommandUser executor,
                                             @NotNull String[] args) {
        if (args.length < 3) {
            plugin.getLocales().getLocale("error_invalid_syntax", "/rtp <player> location <name>")
                    .ifPresent(executor::sendMessage);
            return;
        }

        if (!executor.hasPermission(getPermission("location"))) {
            plugin.getLocales().getLocale("error_no_permission")
                    .ifPresent(executor::sendMessage);
            return;
        }

        if (!executor.equals(teleporter) && !executor.hasPermission(getPermission("other"))) {
            plugin.getLocales().getLocale("error_no_permission")
                    .ifPresent(executor::sendMessage);
            return;
        }

        performLocationRtp(teleporter, executor, args[2]);
    }

    /**
     * Performs an RTP using a named location.
     */
    private void performLocationRtp(@NotNull OnlineUser teleporter, @NotNull CommandUser executor,
                                    @NotNull String locationName) {
        final Optional<RtpLocation> optionalLocation = plugin.getRtpLocations().getLocation(locationName);
        if (optionalLocation.isEmpty()) {
            plugin.getLocales().getLocale("error_rtp_location_not_found", locationName)
                    .ifPresent(executor::sendMessage);
            return;
        }

        // Check economy
        if (!plugin.validateTransaction(teleporter, TransactionResolver.Action.RANDOM_TELEPORT)) {
            return;
        }

        final RtpLocation location = optionalLocation.get();
        final float useMean = location.getDistributionMean() > 0
                ? location.getDistributionMean() : plugin.getSettings().getRtp().getDistributionMean();
        final float useStdDev = location.getDistributionStandardDeviation() > 0
                ? location.getDistributionStandardDeviation()
                : plugin.getSettings().getRtp().getDistributionStandardDeviation();

        // Check if location is on a different server (cross-server)
        final String locationServer = location.getServer();
        final boolean isCrossServer = !locationServer.isEmpty()
                && !locationServer.equalsIgnoreCase(plugin.getServerName());

        if (isCrossServer && plugin.getSettings().getCrossServer().isEnabled()
            && plugin.getSettings().getCrossServer().getBrokerType() == Broker.Type.REDIS) {
            plugin.getLocales().getLocale("teleporting_random_location", locationName)
                    .ifPresent(teleporter::sendMessage);

            plugin.getBroker().ifPresent(b -> Message.builder()
                    .type(Message.MessageType.REQUEST_RTP_LOCATION)
                    .target(locationServer, Message.TargetType.SERVER)
                    .payload(Payload.rtpLocationRequest(
                            location.getWorld(),
                            location.getCenterX(), location.getCenterZ(),
                            location.getMinRadius(), location.getMaxRadius(),
                            useMean, useStdDev
                    ))
                    .build().send(b, teleporter));
            return;
        }

        // Local location RTP
        final Optional<World> localWorld = plugin.getWorlds().stream()
                .filter(w -> w.getName().replace("minecraft:", "")
                        .equalsIgnoreCase(location.getWorld().replace("minecraft:", "")))
                .findFirst();

        if (localWorld.isEmpty()) {
            plugin.getLocales().getLocale("error_invalid_world", location.getWorld())
                    .ifPresent(executor::sendMessage);
            return;
        }

        plugin.getLocales().getLocale("teleporting_random_location", locationName)
                .ifPresent(teleporter::sendMessage);

        final Payload.RtpLocationParams params = new Payload.RtpLocationParams(
                location.getWorld(),
                location.getCenterX(), location.getCenterZ(),
                location.getMinRadius(), location.getMaxRadius(),
                useMean, useStdDev
        );

        plugin.getRandomTeleportEngine()
                .getRandomPosition(localWorld.get(), params)
                .thenAccept(position -> {
                    if (position.isEmpty()) {
                        plugin.getLocales().getLocale("error_rtp_randomization_timeout")
                                .ifPresent(executor::sendMessage);
                        return;
                    }

                    final TeleportBuilder builder = Teleport.builder(plugin)
                            .teleporter(teleporter)
                            .type(Teleport.Type.RANDOM_TELEPORT)
                            .actions(TransactionResolver.Action.RANDOM_TELEPORT)
                            .target(position.get());
                    builder.buildAndComplete(executor.equals(teleporter));
                });
    }

    @Nullable
    @Override
    public List<String> suggest(@NotNull CommandUser user, @NotNull String[] args) {
        return switch (args.length) {
            case 0, 1 -> {
                List<String> suggestions = new ArrayList<>();
                // Suggest "location" keyword
                if (user.hasPermission(getPermission("location"))) {
                    suggestions.add("location");
                }
                // Suggest players
                if (user.hasPermission(getPermission("other"))) {
                    List<String> playerSuggestions = UserListTabCompletable.super.suggest(user, args);
                    if (playerSuggestions != null) {
                        suggestions.addAll(playerSuggestions);
                    }
                } else if (user instanceof OnlineUser online) {
                    suggestions.add(online.getName());
                }
                if (args.length == 1 && !args[0].isEmpty()) {
                    yield suggestions.stream()
                            .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                            .toList();
                }
                yield suggestions;
            }

            case 2 -> {
                // If first arg is "location", suggest location names
                if (args[0].equalsIgnoreCase("location")) {
                    final String input = args[1].toLowerCase();
                    yield plugin.getRtpLocations().getLocationNames().stream()
                            .filter(name -> input.isEmpty() || name.toLowerCase().startsWith(input))
                            .toList();
                }

                String input = args[1].toLowerCase();

                // Check if the input could be a world or a server name
                List<String> possibleSuggestions = new ArrayList<>();

                // Suggest "location" keyword
                if (user.hasPermission(getPermission("location"))) {
                    possibleSuggestions.add("location");
                }

                // Suggest available servers if user has permission
                possibleSuggestions.addAll(plugin.getSettings().getRtp().getRandomTargetServers().keySet().stream()
                        .filter(server -> user.hasPermission(getPermission(server)))
                        .toList());

                // Additionally suggest worlds that the user has permission to RTP into
                possibleSuggestions.addAll(plugin.getWorlds().stream()
                        .filter(world -> !plugin.getSettings().getRtp().isWorldRtpRestricted(world))
                        .map(World::getName)
                        .filter(world -> user.hasPermission(getPermission(world)))
                        .toList());

                if (!input.isEmpty()) {
                    yield possibleSuggestions.stream()
                            .filter(suggestion -> suggestion.toLowerCase().startsWith(input))
                            .toList();
                }

                yield possibleSuggestions;
            }
            case 3 -> {
                // If second arg is "location", suggest location names
                if (args[1].equalsIgnoreCase("location")) {
                    final String input = args[2].toLowerCase();
                    yield plugin.getRtpLocations().getLocationNames().stream()
                            .filter(name -> input.isEmpty() || name.toLowerCase().startsWith(input))
                            .toList();
                }

                // If worldName is a world, suggest servers that contain the world
                String worldName = args[1];

                List<String> possibleSuggestions = new ArrayList<>(plugin.getWorlds().stream()
                        .filter(world -> !plugin.getSettings().getRtp().isWorldRtpRestricted(world))
                        .map(World::getName)
                        .filter(world -> user.hasPermission(getPermission(world)))
                        .toList());

                if (possibleSuggestions.contains(worldName)) {
                    yield plugin.getSettings().getRtp().getRandomTargetServers().entrySet().stream()
                            .filter(entry -> entry.getValue().contains(worldName))
                            .map(Map.Entry::getKey)
                            .toList();
                }

                yield List.of();
            }

            default -> null;
        };
    }

    /**
     * Validates that a random teleport operation is valid.
     *
     * @param teleporter   The player being teleported
     * @param executor     The player executing the command
     * @param worldName    The world name to teleport to
     * @param targetServer The server name to teleport to (optional)
     * @return A pair of the target world and server to use for teleportation, if valid
     */
    private Optional<Map.Entry<World, String>> validateRtp(@NotNull OnlineUser teleporter, @NotNull CommandUser executor,
                                                           @NotNull String worldName, @Nullable String targetServer) {
        // Check permissions if the user is being teleported by another player
        if (!executor.equals(teleporter) && !executor.hasPermission(getPermission("other"))) {
            plugin.getLocales().getLocale("error_no_permission")
                    .ifPresent(executor::sendMessage);
            return Optional.empty();
        }

        // Check they have sufficient funds
        if (!plugin.validateTransaction(teleporter, TransactionResolver.Action.RANDOM_TELEPORT)) {
            return Optional.empty();
        }

        // Validate a cross-server RTP, if applicable
        if (plugin.getSettings().getRtp().isCrossServer() && !plugin.getServerName().equals(targetServer)) {
            return validateCrossServerRtp(executor, worldName, targetServer);
        }

        // Find the local world
        final Optional<World> localWorld = plugin.getWorlds().stream().filter((world) -> world
                .getName().replace("minecraft:", "")
                .equalsIgnoreCase(worldName)).findFirst();
        if (localWorld.isEmpty()) {
            plugin.getLocales().getLocale("error_invalid_world", worldName)
                    .ifPresent(executor::sendMessage);
            return Optional.empty();
        }

        // Check the local world is not restricted
        if (plugin.getSettings().getRtp().isWorldRtpRestricted(localWorld.get())) {
            plugin.getLocales().getLocale("error_rtp_restricted_world")
                    .ifPresent(executor::sendMessage);
            return Optional.empty();
        }
        return localWorld.map(world -> new AbstractMap.SimpleImmutableEntry<>(world, targetServer));
    }

    /**
     * Validates the RTP target world and server based on arguments, ensuring the server contains the target world.
     * - If no server is specified, randomly selects a server containing the world.
     * - Returns both the validated world and server as a pair.
     *
     * @param executor     The player executing the command
     * @param worldName    The world name to teleport to
     * @param targetServer The server name to teleport to (optional)
     * @return A pair of the target world and server to use for teleportation, if valid
     */
    private Optional<Map.Entry<World, String>> validateCrossServerRtp(CommandUser executor, String worldName, String targetServer) {
        // Get a list of servers that have the specified world
        Map<String, List<String>> randomTargetServers = plugin.getSettings().getRtp().getRandomTargetServers();
        List<String> eligibleServers = randomTargetServers.entrySet().stream()
                .filter(entry -> entry.getValue().contains(worldName))
                .map(Map.Entry::getKey)
                .toList();

        // If targetServer is specified, validate it; otherwise, pick a random eligible server
        String selectedServer = targetServer != null ? targetServer :
                (!eligibleServers.isEmpty() ? eligibleServers.get(random.nextInt(eligibleServers.size())) : null);

        // If no server found or the specified server is invalid, return an error
        if (selectedServer == null || (targetServer != null && !eligibleServers.contains(targetServer))) {
            plugin.getLocales().getLocale("error_invalid_world", worldName)
                    .ifPresent(executor::sendMessage);
            return Optional.empty();
        }

        Optional<World> targetWorld = plugin.getWorlds().stream()
                .filter(world -> world.getName().replace("minecraft:", "")
                        .equalsIgnoreCase(worldName))
                .findFirst()
                .or(() -> Optional.of(World.from(worldName)));

        return targetWorld.map(world -> new AbstractMap.SimpleImmutableEntry<>(world, selectedServer));
    }

    /**
     * Executes the RTP, handling both local and cross-server teleportation.
     * Uses the validated world-server pair from validateRtp.
     *
     * @param teleporter   The player to teleport
     * @param executor     The player executing the command
     * @param world        The validated world to teleport to
     * @param targetServer The validated server to teleport to
     * @param args         Arguments to pass to the RTP engine
     */
    private void executeRtp(@NotNull OnlineUser teleporter, @NotNull CommandUser executor,
                            @NotNull World world, @Nullable String targetServer, @NotNull String[] args) {
        // Generate a random position
        plugin.getLocales().getLocale("teleporting_random_generation")
                .ifPresent(teleporter::sendMessage);

        if (plugin.getSettings().getRtp().isCrossServer() && plugin.getSettings().getCrossServer().isEnabled()
            && plugin.getSettings().getCrossServer().getBrokerType() == Broker.Type.REDIS) {
            if (targetServer == null || targetServer.equals(plugin.getServerName())) {
                performLocalRTP(teleporter, executor, world, args);
                return;
            }

            plugin.getBroker().ifPresent(b -> Message.builder()
                    .type(Message.MessageType.REQUEST_RTP_LOCATION)
                    .target(targetServer, Message.TargetType.SERVER)
                    .payload(Payload.string(world.getName()))
                    .build().send(b, teleporter));
            return;
        }

        performLocalRTP(teleporter, executor, world, args);
    }

    /**
     * Performs the RTP locally.
     *
     * @param teleporter person to teleport
     * @param executor   the person executing the teleport
     * @param world      the world to teleport to
     * @param args       rtp engine args
     */
    private void performLocalRTP(@NotNull OnlineUser teleporter, @NotNull CommandUser executor, @NotNull World world,
                                 @NotNull String[] args) {
        plugin.getRandomTeleportEngine()
                .getRandomPosition(world, args.length > 1 ? removeFirstArg(args) : args)
                .thenAccept(position -> {
                    if (position.isEmpty()) {
                        plugin.getLocales().getLocale("error_rtp_randomization_timeout")
                                .ifPresent(executor::sendMessage);
                        return;
                    }

                    // Build and execute the teleport
                    final TeleportBuilder builder = Teleport.builder(plugin)
                            .teleporter(teleporter)
                            .type(Teleport.Type.RANDOM_TELEPORT)
                            .actions(TransactionResolver.Action.RANDOM_TELEPORT)
                            .target(position.get());
                    builder.buildAndComplete(executor.equals(teleporter), args);
                });
    }
}
