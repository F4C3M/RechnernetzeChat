package chat.client;
import java.util.*;

public interface ChatEvents {
    void beiUserliste(List<String> users);
    void beiEinladung(String von);
    void beiUdpNachricht(String nachricht);
    void beiTrennung();
}