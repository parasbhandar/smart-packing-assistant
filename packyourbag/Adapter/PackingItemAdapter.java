
package com.example.packyourbag.Adapter;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.packyourbag.DatabaseEntities.PackingItem;
import com.example.packyourbag.R;
import com.example.packyourbag.Utils.PackingUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PackingItemAdapter extends RecyclerView.Adapter<PackingItemAdapter.ItemViewHolder> {
    private List<PackingItem> items = new ArrayList<>();
    private final OnToggleListener toggleListener;
    private final OnDeleteListener deleteListener;
    private final OnEditListener editListener; // New listener for edit functionality

    // Three separate listener interfaces
    public interface OnToggleListener {
        void onItemToggle(PackingItem item);
    }

    public interface OnDeleteListener {
        void onItemDelete(PackingItem item);
    }

    public interface OnEditListener {
        void onItemEdit(PackingItem item);
    }

    // Updated constructor with three listeners
    public PackingItemAdapter(OnToggleListener toggleListener, OnDeleteListener deleteListener, OnEditListener editListener) {
        this.toggleListener = toggleListener;
        this.deleteListener = deleteListener;
        this.editListener = editListener;
    }

    @Override
    public ItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_packing, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ItemViewHolder holder, int position) {
        PackingItem item = items.get(position);

        holder.textItemName.setText(item.itemName);
        holder.textCategory.setText(item.category);
        holder.checkBoxPacked.setChecked(item.isPacked);

        // Display created date
        if (item.createdAt > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            String dateText = "Added: " + sdf.format(new Date(item.createdAt));
            holder.textCreatedDate.setText(dateText);
            holder.textCreatedDate.setVisibility(View.VISIBLE);
        } else {
            holder.textCreatedDate.setVisibility(View.GONE);
        }

        // Strike through if packed
        if (item.isPacked) {
            holder.textItemName.setPaintFlags(
                    holder.textItemName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG
            );
            holder.textItemName.setAlpha(0.6f);
            holder.textCategory.setAlpha(0.6f);
        } else {
            holder.textItemName.setPaintFlags(
                    holder.textItemName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG)
            );
            holder.textItemName.setAlpha(1.0f);
            holder.textCategory.setAlpha(1.0f);
        }

        // Listeners
        holder.checkBoxPacked.setOnClickListener(v -> toggleListener.onItemToggle(item));
        holder.btnDelete.setOnClickListener(v -> deleteListener.onItemDelete(item));
        holder.btnEdit.setOnClickListener(v -> editListener.onItemEdit(item));

        // Long click on item also allows editing
        holder.itemView.setOnLongClickListener(v -> {
            editListener.onItemEdit(item);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateItems(List<PackingItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView textItemName, textCategory, textCreatedDate;
        CheckBox checkBoxPacked;
        ImageButton btnDelete, btnEdit;

        ItemViewHolder(View itemView) {
            super(itemView);
            textItemName = itemView.findViewById(R.id.textItemName);
            textCategory = itemView.findViewById(R.id.textCategory);
            textCreatedDate = itemView.findViewById(R.id.textCreatedDate);
            checkBoxPacked = itemView.findViewById(R.id.checkBoxPacked);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            btnEdit = itemView.findViewById(R.id.btnEdit);
        }
    }
}