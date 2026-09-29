package magicjinn.elytraswap.serverside;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * Per-player tick logic: phantom while airborne, chestplate on land edge.
 */
public final class PlayerSwapTicker {
	private static final int LAND_STABLE_TICKS = 3;

	private static final Map<UUID, Boolean> WAS_AIRBORNE = new ConcurrentHashMap<>();
	private static final Map<UUID, Integer> GROUNDED_TICKS = new ConcurrentHashMap<>();

	private PlayerSwapTicker() {
	}

	public static void tick(ServerPlayer player) {
		UUID id = player.getUUID();

		if (player.isSpectator() || player.isDeadOrDying() || player.isCreative() || PlayerOptOut.isOptedOut(player)) {
			PhantomElytra.clear(player);
			WAS_AIRBORNE.put(id, false);
			GROUNDED_TICKS.put(id, LAND_STABLE_TICKS);
			return;
		}

		if (player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
			PhantomElytra.clear(player);
			WAS_AIRBORNE.put(id, SwapAlgorithms.isAirborneForElytra(player) || player.isFallFlying());
			if (SwapAlgorithms.shouldWearChestplateBecauseOfGroundState(player)) {
				GROUNDED_TICKS.put(id, GROUNDED_TICKS.getOrDefault(id, 0) + 1);
			} else {
				GROUNDED_TICKS.put(id, 0);
			}
			return;
		}

		// Opening a non-inventory menu risks ghost armor. Clear the phantom.
		if (player.containerMenu != player.inventoryMenu)
			PhantomElytra.clear(player);

		boolean groundedLike = SwapAlgorithms.shouldWearChestplateBecauseOfGroundState(player);
		int groundedTicks = groundedLike
				? GROUNDED_TICKS.getOrDefault(id, 0) + 1
				: 0;
		GROUNDED_TICKS.put(id, groundedTicks);

		boolean wasAirborne = WAS_AIRBORNE.getOrDefault(id, false);
		boolean stablyGrounded = groundedTicks >= LAND_STABLE_TICKS;

		if (stablyGrounded && wasAirborne) {
			onLandEdge(player);
			WAS_AIRBORNE.put(id, false);
		} else if (SwapAlgorithms.isAirborneForElytra(player)) {
			onAirborne(player);
			WAS_AIRBORNE.put(id, true);
		} else if (stablyGrounded) {
			PhantomElytra.clear(player);
			WAS_AIRBORNE.put(id, false);
		} else if (player.isFallFlying()) {
			PhantomElytra.clearQuiet(player);
			WAS_AIRBORNE.put(id, true);
		}
	}

	private static void onAirborne(ServerPlayer player) {
		if (!SwapAlgorithms.canOperate(player) || SwapAlgorithms.isChestBound(player)) {
			PhantomElytra.clear(player);
			return;
		}
		if (SwapAlgorithms.isWearingGlider(player)) {
			PhantomElytra.clearQuiet(player);
			return;
		}
		if (SwapAlgorithms.findBestElytraSlot(player) == null) {
			PhantomElytra.clear(player);
			return;
		}
		PhantomElytra.apply(player);
	}

	private static void onLandEdge(ServerPlayer player) {
		if (!SwapAlgorithms.canOperate(player) || SwapAlgorithms.isChestBound(player)) {
			PhantomElytra.clear(player);
			return;
		}
		if (SwapAlgorithms.isWearingGlider(player)) {
			PhantomElytra.clearQuiet(player);
			InventorySwap.wearBestChestplate(player);
		} else {
			PhantomElytra.clear(player);
		}
	}

	public static void remove(ServerPlayer player) {
		UUID id = player.getUUID();
		WAS_AIRBORNE.remove(id);
		GROUNDED_TICKS.remove(id);
		PhantomElytra.remove(player);
	}
}
