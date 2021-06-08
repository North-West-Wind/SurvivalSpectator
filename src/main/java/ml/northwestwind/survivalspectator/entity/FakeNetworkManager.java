package ml.northwestwind.survivalspectator.entity;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;

public class FakeNetworkManager extends ClientConnection
{
    public FakeNetworkManager(NetworkSide p)
    {
        super(p);
    }

    @Override
    public void disableAutoRead()
    {
    }

    @Override
    public void handleDisconnection()
    {
    }
}