package magicjinn.elytraswap.serverside;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;

/**
 * Persisted per-player opt-out from automatic elytra swapping.
 */
public final class PlayerOptOut {
	private static final Path FILE = FabricLoader.getInstance()
			.getConfigDir()
			.resolve(ElytraswapServerside.MOD_ID)
			.resolve("opt-out.txt");

	private static final Set<UUID> OPTED_OUT = new HashSet<>();
	private static boolean loaded;

	private PlayerOptOut() {
	}

	public static void load() {
		OPTED_OUT.clear();
		loaded = true;
		if (!Files.isRegularFile(FILE)) {
			return;
		}
		try {
			for (String line : Files.readAllLines(FILE)) {
				String trimmed = line.trim();
				if (trimmed.isEmpty() || trimmed.startsWith("#"))
					continue;

				OPTED_OUT.add(UUID.fromString(trimmed));
			}
		} catch (IOException | IllegalArgumentException e) {
			ElytraswapServerside.LOGGER.error("Failed to load elytraswap opt-out list", e);
		}
	}

	public static boolean isOptedOut(ServerPlayer player) {
		ensureLoaded();
		return OPTED_OUT.contains(player.getUUID());
	}

	/** @return true if the player is now opted out */
	public static boolean toggle(ServerPlayer player) {
		ensureLoaded();
		UUID id = player.getUUID();
		boolean nowOptedOut;
		if (OPTED_OUT.contains(id)) {
			OPTED_OUT.remove(id);
			nowOptedOut = false;
		} else {
			OPTED_OUT.add(id);
			nowOptedOut = true;
		}
		save();
		return nowOptedOut;
	}

	private static void ensureLoaded() {
		if (!loaded)
			load();
	}

	private static void save() {
		try {
			Files.createDirectories(FILE.getParent());
			Files.write(FILE, OPTED_OUT.stream().map(UUID::toString).sorted().collect(Collectors.toList()));
		} catch (IOException e) {
			ElytraswapServerside.LOGGER.error("Failed to save elytraswap opt-out list", e);
		}
	}
}
