package lab4.client.controller;

import lab4.client.controller.events.UserEvent;

public interface GameController {
    void event(UserEvent userEvent);
}
