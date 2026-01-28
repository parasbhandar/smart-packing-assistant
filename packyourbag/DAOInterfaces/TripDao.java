package com.example.packyourbag.DAOInterfaces;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.packyourbag.DatabaseEntities.Trip;

import java.util.List;

@Dao
public interface TripDao {
    @Insert
    long insertTrip(Trip trip);

    @Query("SELECT * FROM trips ORDER BY createdAt DESC")
    List<Trip> getAllTrips();

    @Query("SELECT * FROM trips WHERE id = :tripId")
    Trip getTripById(long tripId);

    @Update
    void updateTrip(Trip trip);

    @Delete
    void deleteTrip(Trip trip);

    // Get trips by type
    @Query("SELECT * FROM trips WHERE tripType = :tripType ORDER BY createdAt DESC")
    List<Trip> getTripsByType(String tripType);

    // Get upcoming trips
    @Query("SELECT * FROM trips WHERE startDate >= :currentDate ORDER BY startDate ASC")
    List<Trip> getUpcomingTrips(String currentDate);

    // Get past trips
    @Query("SELECT * FROM trips WHERE endDate < :currentDate ORDER BY createdAt DESC")
    List<Trip> getPastTrips(String currentDate);
}