package com.hybrid.ips.app.filters;

import java.io.Serializable;

public class KalmanFilter implements Serializable {

    private double R;   //  Process Noise
    private double Q;   //  Measurement Noise
    private double A;   //  State Vector
    private double B;   //  Control Vector
    private double C;   //  Measurement Vector

    private Double x;   //  Filtered Measurement Value (No Noise)
    private double cov; //  Covariance

    public KalmanFilter(double r, double q, double a, double b, double c) {
        R = r;
        Q = q;
        A = a;
        B = b;
        C = c;
    }

    public KalmanFilter(double r, double q){
        R = r;
        Q = q;
        A = 1;
        B = 0;
        C = 1;
    }

    /** Public Methods **/
    public double applyFilter(double rssi){
        return applyFilter(rssi, 0.0d);
    }

    /**
     * Filters a measurement
     *
     * @param measurement The measurement value to be filtered
     * @param u The controlled input value
     * @return The filtered value
     */
    public double applyFilter(double measurement, double u) {
        double predX;           //  Predicted Measurement Value
        double K;               //  Kalman Gain
        double predCov;         //  Predicted Covariance
        if (x == null) {
            x = (1 / C) * measurement;
            cov = (1 / C) * Q * (1 / C);
        } else {
            predX = predictValue(u);
            predCov = getUncertainty();
            K = predCov * C * (1 / ((C * predCov * C) + Q));
            x = predX + K * (measurement - (C * predX));
            cov = predCov - (K * C * predCov);
        }
        return x;
    }

    /** Private Methods **/
    private double predictValue(double control){
        return (A * x) + (B * control);
    }

    private double getUncertainty(){
        return ((A * cov) * A) + R;
    }

    @Override
    public String toString() {
        return "KalmanFilter{" +
                "R=" + R +
                ", Q=" + Q +
                ", A=" + A +
                ", B=" + B +
                ", C=" + C +
                ", x=" + x +
                ", cov=" + cov +
                '}';
    }
}