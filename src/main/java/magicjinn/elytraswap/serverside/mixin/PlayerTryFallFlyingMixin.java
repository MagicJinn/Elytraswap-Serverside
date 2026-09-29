package magicjinn.elytraswap.serverside.mixin;

import magicjinn.elytraswap.serverside.TakeoffHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerTryFallFlyingMixin {
	@Inject(method = "tryToStartFallFlying", at = @At("HEAD"))
	private void elytraswap$prepareTakeoff(CallbackInfoReturnable<Boolean> cir) {
		Player self = (Player) (Object) this;
		if (self instanceof ServerPlayer serverPlayer)
			TakeoffHandler.prepareForTakeoff(serverPlayer);
	}
}
