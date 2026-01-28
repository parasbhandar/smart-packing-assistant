package com.example.packyourbag.DatabaseEntities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "packing_items")
public class PackingItem {
    @PrimaryKey(autoGenerate = true)
    public long id;

    public long tripId;
    public String itemName;
    public String category;
    public boolean isPacked;
    public long createdAt; // Added field for creation timestamp

    @androidx.room.Ignore
    public PackingItem(long tripId, String itemName, String category, boolean isPacked) {
        this.tripId = tripId;
        this.itemName = itemName;
        this.category = category;
        this.isPacked = isPacked;
        this.createdAt = System.currentTimeMillis(); // Set current time when created
    }

    // Constructor with createdAt parameter (for database operations)
    public PackingItem(long tripId, String itemName, String category, boolean isPacked, long createdAt) {
        this.tripId = tripId;
        this.itemName = itemName;
        this.category = category;
        this.isPacked = isPacked;
        this.createdAt = createdAt;
    }
}