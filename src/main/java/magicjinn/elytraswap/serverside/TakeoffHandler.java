package magicjinn.elytraswap.serverside;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * Ensures a usable elytra is equipped before vanilla takeoff
 */
public final class TakeoffHandler {
	private TakeoffHandler() {
	}

	/**
	 * Called at the start of
	 * {@link net.minecraft.world.entity.player.Player#tryToStartFallFlying()}.
	 * Swaps in the best inventory elytra when the chest slot cannot glide yet.
	 */
	public static void prepareForTakeoff(ServerPlayer player) {
		if (!SwapAlgorithms.canOperate(player) || SwapAlgorithms.isChestBound(player)) {
			PhantomElytra.clear(player);
			return;
		}

		PhantomElytra.clearQuiet(player);

		if (LivingEntity.canGlideUsing(player.getItemBySlot(EquipmentSlot.CHEST), EquipmentSlot.CHEST))
			return;

		if (SwapAlgorithms.findBestElytraSlot(player) == null)
			return;

		InventorySwap.wearBestElytra(player);
	}
}
