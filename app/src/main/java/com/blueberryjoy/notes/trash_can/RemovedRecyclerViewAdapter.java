package com.blueberryjoy.notes.trash_can;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.blueberryjoy.notes.BuildConfig;
import com.blueberryjoy.notes.R;
import com.blueberryjoy.notes.main.MainRecyclerViewAdapter;
import com.blueberryjoy.notes.utils.FontManager;
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
        setVisibleList();
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
            FontManager.INSTANCE.apply(holder.textViewTitle);
        }
        holder.textViewContent.setText(visibleList.get(position).content);
        if (BuildConfig.DEBUG) {
            holder.textViewRemoveInfo.setText(MessageFormat.format("{0} RemovedFromServer: {1}", visibleList.get(position).getDate(), visibleList.get(position).isRemovedFromServer));
        } else {
            holder.textViewRemoveInfo.setVisibility(View.GONE);
        }

        if (!visibleList.get(position).isEncrypted) {
            holder.textViewContent.setText(visibleList.get(position).content);
            holder.textViewContent.setVisibility(View.VISIBLE);
            holder.secureNote.setVisibility(View.GONE);
            FontManager.INSTANCE.apply(holder.textViewContent);
        } else {
            holder.textViewContent.setVisibility(View.GONE);
            holder.secureNote.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public int getItemCount() {
        return visibleList.size();
    }


    // stores and recycles views as they are scrolled off-screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        final TextView textViewTitle;
        final TextView textViewContent;
        final TextView textViewRemoveInfo;
        final LinearLayout secureNote;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewContent = itemView.findViewById(R.id.textViewContent);
            textViewRemoveInfo = itemView.findViewById(R.id.textViewRemoveInfo);
            secureNote = itemView.findViewById(R.id.secureNote);
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

    public void setVisibleList() {
        visibleList.clear();
        for (RecyclerViewItem note : list) {
            if (note.removeTime == 0 || note.removeTime > System.currentTimeMillis())
                visibleList.add(note);
        }
    }

    @NonNull
    public List<RecyclerViewItem> getVisibleList() {
        return visibleList;
    }
}