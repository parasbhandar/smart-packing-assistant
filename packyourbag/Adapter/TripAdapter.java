package com.example.packyourbag.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.packyourbag.DatabaseEntities.Trip;
import com.example.packyourbag.R;

import java.util.ArrayList;
import java.util.List;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {
    private List<Trip> trips = new ArrayList<>();
    private OnTripClickListener clickListener;
    private OnTripDeleteListener deleteListener;

    public interface OnTripClickListener {
        void onTripClick(Trip trip);
    }

    public interface OnTripDeleteListener {
        void onTripDelete(Trip trip);
    }

    public TripAdapter(OnTripClickListener clickListener, OnTripDeleteListener deleteListener) {
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    @Override
    public TripViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trip, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TripViewHolder holder, int position) {
        Trip trip = trips.get(position);
        holder.textDestination.setText(trip.destination);
        holder.textDuration.setText(trip.duration + " days");
        holder.textTripType.setText(trip.tripType);
        holder.textDates.setText(trip.startDate + " to " + trip.endDate);

        // Show weather info if available
        if (trip.weatherInfo != null && !trip.weatherInfo.equals("Weather data unavailable")) {
            holder.textWeatherInfo.setText("📊 " + trip.weatherInfo);
            holder.textWeatherInfo.setVisibility(View.VISIBLE);
        } else {
            holder.textWeatherInfo.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> clickListener.onTripClick(trip));
        holder.btnDelete.setOnClickListener(v -> deleteListener.onTripDelete(trip));
    }

    @Override
    public int getItemCount() {
        return trips.size();
    }

    public void updateTrips(List<Trip> newTrips) {
        this.trips = newTrips;
        notifyDataSetChanged();
    }

    static class TripViewHolder extends RecyclerView.ViewHolder {
        TextView textDestination, textDuration, textTripType, textDates, textWeatherInfo;
        ImageButton btnDelete;

        TripViewHolder(View itemView) {
            super(itemView);
            textDestination = itemView.findViewById(R.id.textDestination);
            textDuration = itemView.findViewById(R.id.textDuration);
            textTripType = itemView.findViewById(R.id.textTripType);
            textDates = itemView.findViewById(R.id.textDates);
            textWeatherInfo = itemView.findViewById(R.id.textWeatherInfo);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
