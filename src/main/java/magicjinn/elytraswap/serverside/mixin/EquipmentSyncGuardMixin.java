package magicjinn.elytraswap.serverside.mixin;

import java.util.ArrayList;
import java.util.List;

import com.mojang.datafixers.util.Pair;

import magicjinn.elytraswap.serverside.PhantomElytra;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ServerCommonPacketListenerImpl.class)
public class EquipmentSyncGuardMixin {
	@ModifyVariable(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), argsOnly = true)
	private Packet<?> elytraswap$rewritePhantomEquipment(Packet<?> packet) {
		if (!(packet instanceof ClientboundSetEquipmentPacket equipment))
			return packet;

		if (!((Object) this instanceof ServerGamePacketListenerImpl game))
			return packet;

		ServerPlayer player = game.player;
		if (equipment.getEntity() != player.getId())
			return packet;

		ItemStack phantom = PhantomElytra.phantomOrNull(player);
		if (phantom == null)
			return packet;

		List<Pair<EquipmentSlot, ItemStack>> slots = equipment.getSlots();
		boolean touchedChest = false;
		List<Pair<EquipmentSlot, ItemStack>> rewritten = new ArrayList<>(slots.size());
		for (Pair<EquipmentSlot, ItemStack> pair : slots) {
			if (pair.getFirst() == EquipmentSlot.CHEST) {
				rewritten.add(Pair.of(EquipmentSlot.CHEST, phantom));
				touchedChest = true;
			} else {
				rewritten.add(pair);
			}
		}

		return touchedChest ? new ClientboundSetEquipmentPacket(equipment.getEntity(), rewritten) : packet;
	}
}
