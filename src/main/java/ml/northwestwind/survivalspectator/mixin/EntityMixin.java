package ml.northwestwind.survivalspectator.mixin;

import ml.northwestwind.survivalspectator.data.PositionData;
import net.minecraft.entity.Entity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow public World world;

    @Shadow public abstract UUID getUuid();

    @Shadow public abstract boolean isSpectator();

    @Shadow public abstract Vec3d getVelocity();

    @Shadow public float yaw;

    @Shadow public float pitch;

    @Inject(at = @At("HEAD"), method = "getTeleportTarget", cancellable = true)
    public void getTeleportTarget(ServerWorld destination, CallbackInfoReturnable<TeleportTarget> cri) {
        if (world.isClient) return;
        PositionData data = PositionData.get((ServerWorld) world);
        if (data.positions.containsKey(getUuid()) && !isSpectator()) {
            LogManager.getLogger().info("Overriding TeleportTarget");
            cri.setReturnValue(new TeleportTarget(data.positions.get(getUuid()).getLeft(), getVelocity(), yaw, pitch));
        }
    }
}
