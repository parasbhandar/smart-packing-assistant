package com.example.packyourbag.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.packyourbag.DatabaseEntities.Trip;
import com.example.packyourbag.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OverviewAdapter extends RecyclerView.Adapter<OverviewAdapter.OverviewViewHolder> {
    private List<TripOverview> overviewItems = new ArrayList<>();
    private OnTripClickListener clickListener;

    public interface OnTripClickListener {
        void onTripClick(Trip trip);
    }

    public OverviewAdapter(OnTripClickListener clickListener) {
        this.clickListener = clickListener;
    }

    // Data class to hold trip overview information
    public static class TripOverview {
        public Trip trip;
        public int totalItems;
        public int packedItems;
        public int completionPercentage;

        public TripOverview(Trip trip, int totalItems, int packedItems, int completionPercentage) {
            this.trip = trip;
            this.totalItems = totalItems;
            this.packedItems = packedItems;
            this.completionPercentage = completionPercentage;
        }
    }

    @Override
    public OverviewViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trip_overview, parent, false);
        return new OverviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(OverviewViewHolder holder, int position) {
        TripOverview overview = overviewItems.get(position);
        Trip trip = overview.trip;

        holder.textDestination.setText(trip.destination);
        holder.textTripType.setText(trip.tripType);
        holder.textDates.setText(trip.startDate + " to " + trip.endDate);
        holder.textDuration.setText(trip.duration + " days");

        // Display created date
        if (trip.createdAt > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String createdText = "Created: " + sdf.format(new Date(trip.createdAt));
            holder.textCreatedDate.setText(createdText);
        } else {
            holder.textCreatedDate.setText("Created: Unknown");
        }

        // Packing progress
        holder.textPackingProgress.setText(overview.packedItems + "/" + overview.totalItems + " items packed");
        holder.progressBarPacking.setProgress(overview.completionPercentage);
        holder.textProgressPercentage.setText(overview.completionPercentage + "%");

        // Weather info
        if (trip.weatherInfo != null && !trip.weatherInfo.equals("Weather data unavailable")) {
            holder.textWeatherInfo.setText(trip.weatherInfo);
            holder.textWeatherInfo.setVisibility(View.VISIBLE);
        } else {
            holder.textWeatherInfo.setVisibility(View.GONE);
        }

        // Status indicator
        String status;
        if (overview.totalItems == 0) {
            status = "No items added";
        } else if (overview.completionPercentage == 100) {
            status = "Fully packed!";
        } else if (overview.completionPercentage >= 75) {
            status = "Almost ready";
        } else if (overview.completionPercentage >= 50) {
            status = "Half packed";
        } else {
            status = "Just started";
        }
        holder.textStatus.setText(status);

        holder.itemView.setOnClickListener(v -> clickListener.onTripClick(trip));
    }

    @Override
    public int getItemCount() {
        return overviewItems.size();
    }

    public void updateOverviewItems(List<TripOverview> newOverviewItems) {
        this.overviewItems = newOverviewItems;
        notifyDataSetChanged();
    }

    static class OverviewViewHolder extends RecyclerView.ViewHolder {
        TextView textDestination, textTripType, textDates, textDuration;
        TextView textCreatedDate, textPackingProgress, textProgressPercentage;
        TextView textWeatherInfo, textStatus;
        ProgressBar progressBarPacking;

        OverviewViewHolder(View itemView) {
            super(itemView);
            textDestination = itemView.findViewById(R.id.textDestination);
            textTripType = itemView.findViewById(R.id.textTripType);
            textDates = itemView.findViewById(R.id.textDates);
            textDuration = itemView.findViewById(R.id.textDuration);
            textCreatedDate = itemView.findViewById(R.id.textCreatedDate);
            textPackingProgress = itemView.findViewById(R.id.textPackingProgress);
            textProgressPercentage = itemView.findViewById(R.id.textProgressPercentage);
            textWeatherInfo = itemView.findViewById(R.id.textWeatherInfo);
            textStatus = itemView.findViewById(R.id.textStatus);
            progressBarPacking = itemView.findViewById(R.id.progressBarPacking);
        }
    }
}