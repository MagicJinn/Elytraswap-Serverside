package magicjinn.elytraswap.serverside;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Server-side equivalent of JJElytraSwap's chest swap.
 */
public final class InventorySwap {
	/** Player inventory index for the chest armor slot. */
	public static final int CHEST_INVENTORY_SLOT = 38;

	private InventorySwap() {
	}

	/**
	 * Swaps {@code inventorySlot} with the chest equipment slot.
	 * Uses copies so creative/shared stack refs cannot alias both slots.
	 */
	public static boolean swapWithChest(ServerPlayer player, int inventorySlot) {
		ItemStack invCopy = player.getInventory().getItem(inventorySlot).copy();
		ItemStack chestCopy = player.getItemBySlot(EquipmentSlot.CHEST).copy();

		boolean wasSilent = player.isSilent();
		player.setSilent(true);
		try {
			player.getInventory().setItem(inventorySlot, chestCopy);
			player.setItemSlot(EquipmentSlot.CHEST, invCopy);
		} finally {
			player.setSilent(wasSilent);
		}

		syncAfterSwap(player, inventorySlot);
		return true;
	}

	public static boolean wearBestElytra(ServerPlayer player) {
		if (!SwapAlgorithms.canOperate(player)
				|| SwapAlgorithms.isWearingGlider(player)
				|| SwapAlgorithms.isChestBound(player))
			return false;

		// Find the best elytra slot for swapping
		Integer slot = SwapAlgorithms.findBestElytraSlot(player);
		if (slot == null || slot == CHEST_INVENTORY_SLOT)
			return false;

		return swapWithChest(player, slot);
	}

	public static boolean wearBestChestplate(ServerPlayer player) {
		if (!SwapAlgorithms.canOperate(player) || SwapAlgorithms.isChestBound(player))
			return false;

		// Find the best chestplate slot for swapping
		Integer slot = SwapAlgorithms.findBestChestplateSlot(player);
		if (slot == null || slot == CHEST_INVENTORY_SLOT)
			return false;

		return swapWithChest(player, slot);
	}

	// Make sure the client is aware of the swap
	private static void syncAfterSwap(ServerPlayer player, int inventorySlot) {
		player.connection.send(player.getInventory().createInventoryUpdatePacket(inventorySlot));
		player.connection.send(player.getInventory().createInventoryUpdatePacket(CHEST_INVENTORY_SLOT));
		player.inventoryMenu.broadcastFullState();
	}
}
