package v1_8_R3;

import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import de.Linus122.TimeIsMoney.Utils;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;

public class NBTUtils implements Utils{
  public void sendActionBarMessage(Player p, String message)
  {
	    IChatBaseComponent icbc = IChatBaseComponent.ChatSerializer.a("{\"text\": \"" + message.replace('&', '�') + "\"}");
	    PacketPlayOutChat bar = new PacketPlayOutChat(icbc, (byte)2);
	    ((CraftPlayer)p).getHandle().playerConnection.sendPacket(bar);
  }
}
