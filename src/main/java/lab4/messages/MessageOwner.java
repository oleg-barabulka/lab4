package lab4.messages;

import lab4.datatransfer.NetNode;
import lab4.messages.messages.Message;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@RequiredArgsConstructor
public final class MessageOwner {
    @Getter private final Message message;
    @Getter private final NetNode owner;

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof MessageOwner other)) {
            return false;
        }
        return (message.equals(other.message)) && owner.equals(other.owner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, owner);
    }
}
