package ml.northwestwind.survivalspectator.mixin;

import net.minecraft.network.MessageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin {
    @Shadow @Final private static Logger LOGGER;

    @Shadow public ServerPlayerEntity player;

    @Shadow @Final private MinecraftServer server;

    @Shadow protected abstract boolean isHost();

    /**
     * @author Fabric
     */
    @Overwrite
    public void onDisconnected(Text reason) {
        this.LOGGER.info("{} lost connection: {}", this.player.getName().getString(), reason.getString());
        this.server.forcePlayerSampleUpdate();
        if (!reason.getString().equals("Disconnect fake player"))
            this.server.getPlayerManager().broadcastChatMessage((new TranslatableText("multiplayer.player.left", new Object[]{this.player.getDisplayName()})).formatted(Formatting.YELLOW), MessageType.SYSTEM, Util.NIL_UUID);
        this.player.onDisconnect();
        this.server.getPlayerManager().remove(this.player);
        this.player.getTextStream().onDisconnect();
        if (this.isHost()) {
            LOGGER.info("Stopping singleplayer server as player logged out");
            this.server.stop(false);
        }

    }
}
