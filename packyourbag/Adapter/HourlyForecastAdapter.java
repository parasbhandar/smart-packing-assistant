package com.example.packyourbag.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.example.packyourbag.R;
import com.example.packyourbag.Services.WeatherService;
import java.util.List;

public class HourlyForecastAdapter extends RecyclerView.Adapter<HourlyForecastAdapter.HourlyViewHolder> {
    private List<WeatherService.HourlyForecast> forecasts;

    public HourlyForecastAdapter(List<WeatherService.HourlyForecast> forecasts) {
        this.forecasts = forecasts;
    }

    @Override
    public HourlyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hourly_forecast, parent, false);
        return new HourlyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(HourlyViewHolder holder, int position) {
        WeatherService.HourlyForecast forecast = forecasts.get(position);

        holder.textTime.setText(forecast.getFormattedTime());
        holder.textTemperature.setText(Math.round(forecast.temperature) + "°C");
        holder.textCondition.setText(forecast.description);

        if (forecast.precipitationChance > 0) {
            holder.textPrecipitation.setText(Math.round(forecast.precipitationChance) + "% rain");
            holder.textPrecipitation.setVisibility(View.VISIBLE);
        } else {
            holder.textPrecipitation.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return forecasts.size();
    }

    static class HourlyViewHolder extends RecyclerView.ViewHolder {
        TextView textTime, textTemperature, textCondition, textPrecipitation;

        HourlyViewHolder(View itemView) {
            super(itemView);
            textTime = itemView.findViewById(R.id.textTime);
            textTemperature = itemView.findViewById(R.id.textTemperature);
            textCondition = itemView.findViewById(R.id.textCondition);
            textPrecipitation = itemView.findViewById(R.id.textPrecipitation);
        }
    }
}