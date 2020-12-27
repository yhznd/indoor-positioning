package com.hybrid.ips.app;


import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class Device extends RealmObject
{
    @PrimaryKey
    String id;
    String UUID;
    String macAddres;
    Double distance;
    Double x;
    Double y;
    Double rssi;
    String createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUUID() {
        return UUID;
    }

    public void setUUID(String UUID) {
        this.UUID = UUID;
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

    public Device(String UUID, String macAddres, Double distance, Double x, Double y, Double rssi, String createdAt) {
        this.UUID = UUID;
        this.macAddres = macAddres;
        this.distance = distance;
        this.x = x;
        this.y = y;
        this.rssi = rssi;
        this.createdAt = createdAt;
    }
}
