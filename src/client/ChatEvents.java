package client;
import java.util.*;

public interface ChatEvents {
    void onUserList(List<String> users);
    void onInvite(String from);
    void onUdpMessage(String msg);
    void onDisconnect();
}