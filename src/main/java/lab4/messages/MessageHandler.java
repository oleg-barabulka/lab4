package lab4.messages;

import lab4.datatransfer.NetNode;
import lab4.messages.messages.*;

public interface MessageHandler {
    void handle(NetNode sender, SteerMessage message);
    void handle(NetNode sender, JoinMessage message);
    void handle(NetNode sender, PingMessage message);
    void handle(NetNode sender, StateMessage message);
    void handle(NetNode sender, ErrorMessage message);
    void handle(NetNode sender, RoleChangeMessage message);
    void handle(NetNode sender, DiscoverMessage message);
}
