
package com.example.packyourbag;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.example.packyourbag.Adapter.OverviewAdapter;
import com.example.packyourbag.Database.PackingDatabase;
import com.example.packyourbag.DatabaseEntities.PackingItem;
import com.example.packyourbag.DatabaseEntities.Trip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OverviewActivity extends AppCompatActivity {
    private PackingDatabase database;
    private RecyclerView recyclerOverview;
    private OverviewAdapter overviewAdapter;
    private TextView textOverviewStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);

        database = Room.databaseBuilder(getApplicationContext(),
                        PackingDatabase.class, "packing_db")
                .allowMainThreadQueries()
                .build();

        initViews();
        setupRecyclerView();
        loadOverviewData();
    }

    private void initViews() {
        recyclerOverview = findViewById(R.id.recyclerOverview);
        textOverviewStats = findViewById(R.id.textOverviewStats);

        setTitle("Complete Overview");
    }

    private void setupRecyclerView() {
        overviewAdapter = new OverviewAdapter(this::openTrip);
        recyclerOverview.setLayoutManager(new LinearLayoutManager(this));
        recyclerOverview.setAdapter(overviewAdapter);
    }

    private void openTrip(Trip trip) {
        Intent intent = new Intent(this, PackingListActivity.class);
        intent.putExtra("tripId", trip.id);
        startActivity(intent);
    }

    private void loadOverviewData() {
        List<Trip> allTrips = database.tripDao().getAllTrips();
        List<OverviewAdapter.TripOverview> overviewItems = new ArrayList<>();

        int totalTrips = allTrips.size();
        int totalItems = 0;
        int totalPackedItems = 0;
        int completedTrips = 0;

        for (Trip trip : allTrips) {
            List<PackingItem> items = database.packingItemDao().getItemsForTrip(trip.id);
            int tripItems = items.size();
            int tripPackedItems = 0;

            for (PackingItem item : items) {
                if (item.isPacked) tripPackedItems++;
            }

            totalItems += tripItems;
            totalPackedItems += tripPackedItems;

            if (tripItems > 0 && tripPackedItems == tripItems) {
                completedTrips++;
            }

            // Calculate completion percentage
            int completionPercentage = tripItems > 0 ? (tripPackedItems * 100) / tripItems : 0;

            // Create overview item
            OverviewAdapter.TripOverview overview = new OverviewAdapter.TripOverview(
                    trip,
                    tripItems,
                    tripPackedItems,
                    completionPercentage
            );
            overviewItems.add(overview);
        }

        // Update adapter
        overviewAdapter.updateOverviewItems(overviewItems);

        // Update statistics
        updateStatistics(totalTrips, totalItems, totalPackedItems, completedTrips);
    }

    private void updateStatistics(int totalTrips, int totalItems, int totalPackedItems, int completedTrips) {
        StringBuilder stats = new StringBuilder();

        stats.append("Total Trips: ").append(totalTrips).append("\n");
        stats.append("Completed Trips: ").append(completedTrips).append("\n");
        stats.append("Total Items: ").append(totalItems).append("\n");
        stats.append("Packed Items: ").append(totalPackedItems).append("\n");

        if (totalItems > 0) {
            int overallProgress = (totalPackedItems * 100) / totalItems;
            stats.append("Overall Progress: ").append(overallProgress).append("%");
        } else {
            stats.append("Overall Progress: 0%");
        }

        textOverviewStats.setText(stats.toString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadOverviewData(); // Refresh data when returning to this activity
    }
}