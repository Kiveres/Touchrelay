package com.touchrelay.app.model;

public class TouchEvent {
    public static final String ACTION_DOWN = "down";
    public static final String ACTION_MOVE = "move";
    public static final String ACTION_UP = "up";
    public static final String ACTION_BUTTON = "button";

    public String action;
    public float x;      // normalized 0.0 - 1.0
    public float y;      // normalized 0.0 - 1.0
    public int pointerId;
    public String button; // "back", "home", "recents"

    public TouchEvent() {}

    public TouchEvent(String action, float x, float y, int pointerId) {
        this.action = action;
        this.x = x;
        this.y = y;
        this.pointerId = pointerId;
    }

    public static TouchEvent button(String button) {
        TouchEvent e = new TouchEvent();
        e.action = ACTION_BUTTON;
        e.button = button;
        return e;
    }
}
