package com.example.packyourbag.DAOInterfaces;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.packyourbag.DatabaseEntities.PackingItem;

import java.util.List;

@Dao
public interface PackingItemDao {
    @Insert
    void insertItem(PackingItem item);

    @Query("SELECT * FROM packing_items WHERE tripId = :tripId ORDER BY category, itemName")
    List<PackingItem> getItemsForTrip(long tripId);

    @Query("SELECT * FROM packing_items WHERE id = :itemId")
    PackingItem getItemById(long itemId);

    @Update
    void updateItem(PackingItem item);

    @Delete
    void deleteItem(PackingItem item);

    @Query("DELETE FROM packing_items WHERE tripId = :tripId")
    void deleteItemsForTrip(long tripId);

    // Get items by category for better organization
    @Query("SELECT * FROM packing_items WHERE tripId = :tripId AND category = :category ORDER BY itemName")
    List<PackingItem> getItemsByCategoryForTrip(long tripId, String category);

    // Get packed/unpacked items
    @Query("SELECT * FROM packing_items WHERE tripId = :tripId AND isPacked = :isPacked ORDER BY category, itemName")
    List<PackingItem> getItemsByPackedStatus(long tripId, boolean isPacked);
}