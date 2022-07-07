package com.sadharan.indoor_positioning.client;

import androidx.annotation.NonNull;

public class LocationElement {
    public float x_coordinate;
    public float y_coordinate;
    public int floor_id;
    public LocationElement(float x_coordinate, float y_coordinate, int floor_id) {
        this.x_coordinate = x_coordinate;
        this.y_coordinate = y_coordinate;
        this.floor_id = floor_id;
    }

    @NonNull
    public String toString() {
        return "X:" + this.x_coordinate + "\n" +
                "Y:" + this.y_coordinate + "\n" +
                "Floor:" + this.floor_id;
    }
}
