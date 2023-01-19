package lab4.client.controller.events;

public final class NewGameEvent extends UserEvent {
    public NewGameEvent() {
        super(EventType.NEW_GAME);
    }
}
