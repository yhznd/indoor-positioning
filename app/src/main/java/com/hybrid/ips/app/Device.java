package com.hybrid.ips.app;


import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Device extends RealmObject
{
    @PrimaryKey
    String id;
    String measureId;
    String macAddres;
    Double distance;
    Double x;
    Double y;
    Double rssi;
    String createdAt;
    Integer batteryLevel;

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

    public String getMacAddres() {
        return macAddres;
    }

    public void setMacAddres(String macAddres) {
        this.macAddres = macAddres;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public Double getX() {
        return x;
    }

    public void setX(Double x) {
        this.x = x;
    }

    public Double getY() {
        return y;
    }

    public void setY(Double y) {
        this.y = y;
    }

    public Double getRssi() {
        return rssi;
    }

    public void setRssi(Double rssi) {
        this.rssi = rssi;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public Device() {
    }

    public Integer getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(Integer batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public Device(String measureId, String macAddres, Double distance, Double x, Double y, Double rssi, Integer batteryLevel,String createdAt) {
        this.measureId = measureId;
        this.macAddres = macAddres;
        this.distance = distance;
        this.x = x;
        this.y = y;
        this.rssi = rssi;
        this.batteryLevel=batteryLevel;
        this.createdAt = createdAt;
    }
}
