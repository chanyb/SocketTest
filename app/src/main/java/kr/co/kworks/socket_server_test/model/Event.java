package kr.co.kworks.socket_server_test.model;

public class Event<T> {
    private final T content;
    private boolean handled = false;
    public Event(T c){ content = c; }
    public T getContentIfNotHandled() {
        if (handled) return null;
        handled = true;
        return content;
    }
    public T peek() {
      return content;
    }
}