package lab4.client.controller.events;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class UserEvent {
    @Getter private final EventType type;
}
