package ml.northwestwind.survivalspectator.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

@Mixin(IntegratedServer.class)
public class IntegratedServerMixin {
    @Shadow private UUID localPlayerUuid;

    /**
     * @author Fabric
     */
    @Overwrite
    public boolean isHost(GameProfile profile) {
        return profile.getId().equals(this.localPlayerUuid);
    }
}
