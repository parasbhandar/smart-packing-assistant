package com.example.packyourbag.DatabaseEntities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "trips")
public class Trip {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String destination;
    public int duration;
    public String tripType;
    public String startDate;
    public String endDate;
    public String weatherInfo;
    public long createdAt;
    public String date;

    public Trip(String destination, int duration, String tripType, String startDate, String endDate, String weatherInfo, long createdAt) {
        this.destination = destination;
        this.duration = duration;
        this.tripType = tripType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.weatherInfo = weatherInfo;
        this.createdAt = createdAt;
    }
}