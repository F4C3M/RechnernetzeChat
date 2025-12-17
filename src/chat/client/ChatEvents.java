package chat.client;
import java.util.*;

public interface ChatEvents {
    void beiUserliste(List<String> users);
    void beiEinladung(String from);
    void beiUdpNachricht(String msg);
    void beiTrennung();
}