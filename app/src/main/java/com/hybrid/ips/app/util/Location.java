package com.hybrid.ips.app.util;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Location extends RealmObject
{
    @PrimaryKey
    String id;
    String measureId;
    double locationX;
    double locationY;
    String area;
    String createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMeasureId() {
        return measureId;
    }

    public void setMeasureId(String measureId) {
        this.measureId = measureId;
    }

    public double getLocationX() {
        return locationX;
    }

    public void setLocationX(double locationX) {
        this.locationX = locationX;
    }

    public double getLocationY() {
        return locationY;
    }

    public void setLocationY(double locationY) {
        this.locationY = locationY;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public Location() {
    }

    public Location(String id, String measureId, double locationX, double locationY, String area,String createdAt)
    {
        this.id = id;
        this.measureId = measureId;
        this.locationX = locationX;
        this.locationY = locationY;
        this.area=area;
        this.createdAt = createdAt;
    }
}
