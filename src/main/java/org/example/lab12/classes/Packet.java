package org.example.lab12.classes;

import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class Packet {
    private int destination;
    private int currentPosition;
    private Circle packetCircle;

    public Packet(int destination) {
        this.destination = destination;
        this.packetCircle = new Circle(5, Color.BLUE); // Уникальный визуальный элемент
    }

    public int getDestination() {
        return destination;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(int currentPosition) {
        this.currentPosition = currentPosition;
    }

    public Circle getPacketCircle() {
        return packetCircle;
    }
}
