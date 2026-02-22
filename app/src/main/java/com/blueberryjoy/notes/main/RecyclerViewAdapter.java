package com.blueberryjoy.notes.main;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.blueberryjoy.notes.R;

import java.util.List;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    private final List<RecyclerViewItem> list;
    private final LayoutInflater layoutInflater;
    private ItemClickListener itemclickListener;


    RecyclerViewAdapter(Context context, List<RecyclerViewItem> list) {
        this.layoutInflater = LayoutInflater.from(context);
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.recyclerview_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (list.get(position).title == null || list.get(position).title.trim().isEmpty()) {
            holder.textViewTitle.setVisibility(View.GONE);
        } else {
            holder.textViewTitle.setVisibility(View.VISIBLE);
            holder.textViewTitle.setText(list.get(position).title);
        }

        holder.textViewContent.setText(list.get(position).content);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


    // stores and recycles views as they are scrolled off-screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView textViewTitle;
        TextView textViewContent;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewContent = itemView.findViewById(R.id.textViewContent);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (itemclickListener != null)
                itemclickListener.onItemClick(view, getAbsoluteAdapterPosition());
        }
    }

    public void setClickListener(ItemClickListener itemClickListener) {
        this.itemclickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }
}
