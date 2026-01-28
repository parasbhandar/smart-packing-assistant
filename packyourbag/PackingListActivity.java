package com.example.packyourbag;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.example.packyourbag.Adapter.PackingItemAdapter;
import com.example.packyourbag.Database.PackingDatabase;
import com.example.packyourbag.DatabaseEntities.PackingItem;
import com.example.packyourbag.DatabaseEntities.Trip;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PackingListActivity extends AppCompatActivity {
    private PackingDatabase database;
    private RecyclerView recyclerItems;
    private PackingItemAdapter itemAdapter;
    private EditText editNewItem;
    private Spinner spinnerCategory, spinnerCategoryFilter;
    private Button btnAddItem, btnWeatherInfo, btnEditTripTitle, btnClearFilter;
    private ProgressBar progressBar;
    private TextView textProgress, textTripInfo, textTripCreatedDate;
    private long tripId;
    private Trip currentTrip;
    private List<PackingItem> allItems = new ArrayList<>();
    private List<PackingItem> filteredItems = new ArrayList<>();
    private String currentFilter = "All Categories";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_packing_list);

        database = Room.databaseBuilder(getApplicationContext(),
                        PackingDatabase.class, "packing_db")
                .allowMainThreadQueries()
                .build();

        tripId = getIntent().getLongExtra("tripId", -1);
        if (tripId == -1) {
            finish();
            return;
        }

        currentTrip = database.tripDao().getTripById(tripId);
        initViews();
        setupRecyclerView();
        setupFilterSpinner();
        loadItems();
        updateProgress();
    }

    private void initViews() {
        recyclerItems = findViewById(R.id.recyclerItems);
        editNewItem = findViewById(R.id.editNewItem);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        spinnerCategoryFilter = findViewById(R.id.spinnerCategoryFilter);
        btnAddItem = findViewById(R.id.btnAddItem);
        btnWeatherInfo = findViewById(R.id.btnWeatherInfo);
        btnEditTripTitle = findViewById(R.id.btnEditTripTitle);
        btnClearFilter = findViewById(R.id.btnClearFilter);
        progressBar = findViewById(R.id.progressBar);
        textProgress = findViewById(R.id.textProgress);
        textTripInfo = findViewById(R.id.textTripInfo);
        textTripCreatedDate = findViewById(R.id.textTripCreatedDate);

        updateTripInfo();

        // Setup category spinner for adding items
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.enhanced_item_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        btnAddItem.setOnClickListener(v -> addNewItem());
        btnWeatherInfo.setOnClickListener(v -> showWeatherInfo());
        btnEditTripTitle.setOnClickListener(v -> editTripTitle());
        btnClearFilter.setOnClickListener(v -> clearFilter());
    }

    private void setupFilterSpinner() {
        // Create filter categories list (All Categories + actual categories)
        List<String> filterCategories = new ArrayList<>();
        filterCategories.add("All Categories");

        // Add all category options from arrays.xml
        String[] categories = getResources().getStringArray(R.array.enhanced_item_categories);
        for (String category : categories) {
            filterCategories.add(category);
        }

        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, filterCategories);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategoryFilter.setAdapter(filterAdapter);

        // Set up filter listener
        spinnerCategoryFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCategory = (String) parent.getItemAtPosition(position);
                filterItemsByCategory(selectedCategory);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void filterItemsByCategory(String category) {
        currentFilter = category;
        filteredItems.clear();

        if (category.equals("All Categories")) {
            filteredItems.addAll(allItems);
        } else {
            for (PackingItem item : allItems) {
                if (item.category.equals(category)) {
                    filteredItems.add(item);
                }
            }
        }

        itemAdapter.updateItems(filteredItems);
        updateProgress(); // Update progress based on filtered items if needed
    }

    private void clearFilter() {
        spinnerCategoryFilter.setSelection(0); // Select "All Categories"
        filterItemsByCategory("All Categories");
    }

    private void updateTripInfo() {
        textTripInfo.setText(currentTrip.destination + " (" +
                currentTrip.startDate + " to " + currentTrip.endDate + ") - " +
                currentTrip.duration + " days");

        // Display created date
        if (currentTrip.createdAt > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy 'at' HH:mm", Locale.getDefault());
            String createdText = "Created: " + sdf.format(new Date(currentTrip.createdAt));
            textTripCreatedDate.setText(createdText);
            textTripCreatedDate.setVisibility(View.VISIBLE);
        } else {
            textTripCreatedDate.setVisibility(View.GONE);
        }
    }

    private void editTripTitle() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Trip Title");

        // Create custom layout for the dialog
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_trip, null);
        builder.setView(dialogView);

        EditText editDestination = dialogView.findViewById(R.id.editDestination);
        editDestination.setText(currentTrip.destination);
        editDestination.selectAll();

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newDestination = editDestination.getText().toString().trim();
            if (!newDestination.isEmpty()) {
                currentTrip.destination = newDestination;
                database.tripDao().updateTrip(currentTrip);
                updateTripInfo();
                Toast.makeText(this, "Trip title updated", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addNewItem() {
        String itemName = editNewItem.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();

        if (itemName.isEmpty()) {
            Toast.makeText(this, "Please enter item name", Toast.LENGTH_SHORT).show();
            return;
        }

        PackingItem item = new PackingItem(tripId, itemName, category, false);
        database.packingItemDao().insertItem(item);
        editNewItem.setText("");
        loadItems();
        updateProgress();
    }

    private void showWeatherInfo() {
        Intent intent = new Intent(this, WeatherActivity.class);
        intent.putExtra("destination", currentTrip.destination);
        intent.putExtra("weatherInfo", currentTrip.weatherInfo);
        intent.putExtra("startDate", currentTrip.startDate);
        intent.putExtra("endDate", currentTrip.endDate);
        startActivity(intent);
    }

    private void setupRecyclerView() {
        itemAdapter = new PackingItemAdapter(this::toggleItem, this::deleteItem, this::editItem);
        recyclerItems.setLayoutManager(new LinearLayoutManager(this));
        recyclerItems.setAdapter(itemAdapter);
    }

    private void toggleItem(PackingItem item) {
        item.isPacked = !item.isPacked;
        database.packingItemDao().updateItem(item);
        // Update the item in both lists
        updateItemInLists(item);
        itemAdapter.notifyDataSetChanged();
        updateProgress();
    }

    private void deleteItem(PackingItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete \"" + item.itemName + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    database.packingItemDao().deleteItem(item);
                    loadItems();
                    updateProgress();
                    Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void editItem(PackingItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Edit Item");

        // Create custom layout for the dialog
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_item, null);
        builder.setView(dialogView);

        EditText editItemName = dialogView.findViewById(R.id.editItemName);
        Spinner spinnerEditCategory = dialogView.findViewById(R.id.spinnerEditCategory);

        // Setup the spinner with categories
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.enhanced_item_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEditCategory.setAdapter(adapter);

        // Set current values
        editItemName.setText(item.itemName);
        editItemName.selectAll();

        // Set current category selection
        for (int i = 0; i < spinnerEditCategory.getCount(); i++) {
            if (spinnerEditCategory.getItemAtPosition(i).toString().equals(item.category)) {
                spinnerEditCategory.setSelection(i);
                break;
            }
        }

        builder.setPositiveButton("Save", (dialog, which) -> {
            String newItemName = editItemName.getText().toString().trim();
            String newCategory = spinnerEditCategory.getSelectedItem().toString();

            if (!newItemName.isEmpty()) {
                item.itemName = newItemName;
                item.category = newCategory;
                database.packingItemDao().updateItem(item);
                loadItems();
                Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Item name cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void loadItems() {
        allItems = database.packingItemDao().getItemsForTrip(tripId);
        filterItemsByCategory(currentFilter);
    }

    private void updateItemInLists(PackingItem updatedItem) {
        // Update item in allItems list
        for (int i = 0; i < allItems.size(); i++) {
            if (allItems.get(i).id == updatedItem.id) {
                allItems.set(i, updatedItem);
                break;
            }
        }

        // Update item in filteredItems list
        for (int i = 0; i < filteredItems.size(); i++) {
            if (filteredItems.get(i).id == updatedItem.id) {
                filteredItems.set(i, updatedItem);
                break;
            }
        }
    }

    private void updateProgress() {
        // Calculate progress based on ALL items, not just filtered ones
        int totalItems = allItems.size();
        int packedItems = 0;

        for (PackingItem item : allItems) {
            if (item.isPacked) packedItems++;
        }

        int progressPercent = totalItems > 0 ? (packedItems * 100) / totalItems : 0;
        progressBar.setProgress(progressPercent);

        // Show filter info in progress text if filtering is active
        String progressText = packedItems + "/" + totalItems + " items packed (" + progressPercent + "%)";
        if (!currentFilter.equals("All Categories")) {
            progressText += " | Showing: " + filteredItems.size() + " " + currentFilter + " items";
        }
        textProgress.setText(progressText);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh trip data in case it was edited
        currentTrip = database.tripDao().getTripById(tripId);
        updateTripInfo();
        loadItems();
        updateProgress();
    }
}
