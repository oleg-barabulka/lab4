package lab4.messages.messages;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.validation.constraints.AssertFalse;

@RequiredArgsConstructor
public enum MessageType {
    STATE(true),
    ACK(false),
    PING(true),
    ROLE_CHANGE(true),
    STEER(true),
    ANNOUNCEMENT(false),
    JOIN(true),
    ERROR(false),
    DISCOVER(false);

    @Getter private final boolean needConfirmation;
}
