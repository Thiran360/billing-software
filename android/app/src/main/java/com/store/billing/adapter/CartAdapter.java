package com.store.billing.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.store.billing.R;
import com.store.billing.model.BillItem;

import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    public interface OnCartChangeListener {
        void onCartChanged();
    }

    private final List<BillItem> items;
    private final OnCartChangeListener listener;

    public CartAdapter(List<BillItem> items, OnCartChangeListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BillItem item = items.get(position);
        
        String displayName = item.getProductName();
        if (item.getTamilName() != null && !item.getTamilName().trim().isEmpty()) {
            displayName += " (" + item.getTamilName() + ")";
        }
        holder.tvName.setText(displayName);
        holder.tvPriceUnit.setText(String.format(Locale.getDefault(), "₹ %.2f / %s", item.getUnitPrice(), item.getUnitType()));
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));
        holder.tvSubtotal.setText(String.format(Locale.getDefault(), "₹ %.2f", item.getSubtotal()));

        holder.btnPlus.setOnClickListener(v -> {
            item.setQuantity(item.getQuantity() + 1);
            notifyItemChanged(holder.getAdapterPosition());
            if (listener != null) listener.onCartChanged();
        });

        holder.btnMinus.setOnClickListener(v -> {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                notifyItemChanged(holder.getAdapterPosition());
                if (listener != null) listener.onCartChanged();
            } else {
                int pos = holder.getAdapterPosition();
                items.remove(pos);
                notifyItemRemoved(pos);
                if (listener != null) listener.onCartChanged();
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            items.remove(pos);
            notifyItemRemoved(pos);
            if (listener != null) listener.onCartChanged();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPriceUnit, tvQuantity, tvSubtotal;
        Button btnMinus, btnPlus;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCartItemName);
            tvPriceUnit = itemView.findViewById(R.id.tvCartItemPriceUnit);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvSubtotal = itemView.findViewById(R.id.tvSubtotal);
            btnMinus = itemView.findViewById(R.id.btnMinus);
            btnPlus = itemView.findViewById(R.id.btnPlus);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
