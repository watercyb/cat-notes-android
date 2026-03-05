package com.blueberryjoy.notes.trash_can;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.blueberryjoy.notes.R;
import com.blueberryjoy.notes.main.MainRecyclerViewAdapter;
import com.blueberryjoy.notes.utils.RecyclerViewItem;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public class RemovedRecyclerViewAdapter extends RecyclerView.Adapter<RemovedRecyclerViewAdapter.ViewHolder> {

    private final List<RecyclerViewItem> list;
    private final List<RecyclerViewItem> visibleList = new ArrayList<>();
    private final LayoutInflater layoutInflater;
    private MainRecyclerViewAdapter.ItemClickListener itemclickListener;


    RemovedRecyclerViewAdapter(Context context, List<RecyclerViewItem> list) {
        this.layoutInflater = LayoutInflater.from(context);
        this.list = list;
        getVisibleList();
    }

    @NonNull
    @Override
    public RemovedRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.trash_can_recyclerview_item, parent, false);
        return new RemovedRecyclerViewAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RemovedRecyclerViewAdapter.ViewHolder holder, int position) {
        if (visibleList.get(position).title == null || visibleList.get(position).title.trim().isEmpty()) {
            holder.textViewTitle.setVisibility(View.GONE);
        } else {
            holder.textViewTitle.setVisibility(View.VISIBLE);
            holder.textViewTitle.setText(visibleList.get(position).title);
        }

        holder.textViewContent.setText(visibleList.get(position).content);
        holder.textViewRemoveInfo.setText(MessageFormat.format("{0} RemovedFromServer: {1}", visibleList.get(position).getDate(), visibleList.get(position).isRemovedFromServer));

    }

    @Override
    public int getItemCount() {
        return visibleList.size();
    }


    // stores and recycles views as they are scrolled off-screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView textViewTitle;
        TextView textViewContent;
        TextView textViewRemoveInfo;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewContent = itemView.findViewById(R.id.textViewContent);
            textViewRemoveInfo = itemView.findViewById(R.id.textViewRemoveInfo);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (itemclickListener != null)
                itemclickListener.onItemClick(view, getAbsoluteAdapterPosition());
        }
    }

    public void setClickListener(MainRecyclerViewAdapter.ItemClickListener itemClickListener) {
        this.itemclickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    public void getVisibleList() {
        visibleList.clear();
        for (RecyclerViewItem note : list) {
            if (note.removeTime == 0 || note.removeTime > System.currentTimeMillis())
                visibleList.add(note);
        }
    }
}