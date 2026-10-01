package com.blueberryjoy.notes.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.blueberryjoy.notes.R;
import com.blueberryjoy.notes.auth.AuthManager;
import com.blueberryjoy.notes.sqlite.SQLite;
import com.blueberryjoy.notes.utils.Message;
import com.blueberryjoy.notes.utils.RecyclerViewItem;

import java.util.ArrayList;
import java.util.List;

public class MainRecyclerViewAdapter extends RecyclerView.Adapter<MainRecyclerViewAdapter.ViewHolder> {

    private final List<RecyclerViewItem> list;
    private final List<RecyclerViewItem> visibleList = new ArrayList<>();
    private final LayoutInflater layoutInflater;
    private ItemClickListener itemclickListener;


    MainRecyclerViewAdapter(Context context, List<RecyclerViewItem> list) {
        this.layoutInflater = LayoutInflater.from(context);
        this.list = list;
        setVisibleList();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = layoutInflater.inflate(R.layout.recyclerview_main_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (visibleList.get(position).title == null || visibleList.get(position).title.trim().isEmpty()) {
            holder.textViewTitle.setVisibility(View.GONE);
        } else {
            holder.textViewTitle.setVisibility(View.VISIBLE);
            holder.textViewTitle.setText(visibleList.get(position).title);
        }

        if (AuthManager.isOwner(visibleList.get(position).userId)) {
            holder.linearLayout.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.note_card));
        } else {
            holder.linearLayout.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.note_card_other_user));
        }

        if (!visibleList.get(position).isEncrypted) {
            holder.textViewContent.setText(visibleList.get(position).content);
            holder.textViewContent.setVisibility(View.VISIBLE);
            holder.secureNote.setVisibility(View.GONE);
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
        final LinearLayout linearLayout;
        final LinearLayout secureNote;


        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewContent = itemView.findViewById(R.id.textViewContent);
            linearLayout = itemView.findViewById(R.id.linearLayout);
            secureNote = itemView.findViewById(R.id.secureNote);
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

    public void setVisibleList() {
        visibleList.clear();
        if (AuthManager.getIsShowAllNotes()) {
            visibleList.addAll(list);
        } else {
            for (RecyclerViewItem note : list) {
                if (AuthManager.isShowNote(note.userId))
                    visibleList.add(note);
            }
        }
    }

    public void addToList(RecyclerViewItem note) {
        list.add(note);
        if (AuthManager.isShowNote(note.userId)) {
            visibleList.add(note);
            this.notifyItemInserted(visibleList.size() - 1);
        }
    }

    public void noteChanged(RecyclerViewItem note) {
        for (int i = 0; i < visibleList.size(); i++) {
            if (visibleList.get(i).id == note.id) {
                this.notifyItemChanged(i);
                break;
            }
        }
    }

    public void removeNote(RecyclerViewItem note) {
        for (int i = 0; i < visibleList.size(); i++) {
            if (visibleList.get(i).id == note.id) {
                visibleList.remove(i);
                this.notifyItemRemoved(i);
                break;
            }
        }
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == note.id) {
                list.remove(i);
                break;
            }
        }
    }

    private int getIdxFromVisibleIdx(int i) {
        for (int j = 0; j < list.size(); j++) {
            if (list.get(j) == visibleList.get(i)) return j;
        }
        Message.debug("getIdxFromVisibleIdx: Should not be called");
        return -1;
    }

    public void swap(int l, int r) {
        int i = getIdxFromVisibleIdx(l);
        int j = getIdxFromVisibleIdx(r);
        int start = l;
        if (i > j) {
            while (i > j) {
                RecyclerViewItem a = list.get(i);
                RecyclerViewItem b = list.get(i - 1);
                int temp = a.idx;
                a.idx = b.idx;
                b.idx = temp;
                list.set(i, b);
                list.set(i - 1, a);
                SQLite.saveIdx(a);
                SQLite.saveIdx(b);
                i--;
            }
            while (l > r) {
                RecyclerViewItem temp = visibleList.get(l);
                visibleList.set(l, visibleList.get(l - 1));
                visibleList.set(l - 1, temp);
                l--;
            }
        } else {
            while (i < j) {
                RecyclerViewItem a = list.get(i);
                RecyclerViewItem b = list.get(i + 1);
                int temp = a.idx;
                a.idx = b.idx;
                b.idx = temp;
                list.set(i, b);
                list.set(i + 1, a);
                SQLite.saveIdx(a);
                SQLite.saveIdx(b);
                i++;
            }
            while (l < r) {
                RecyclerViewItem temp = visibleList.get(l);
                visibleList.set(l, visibleList.get(l + 1));
                visibleList.set(l + 1, temp);
                l++;
            }
        }
        this.notifyItemMoved(start, r);
    }

    public int getIdFromVisibleIdx(int idx) {
        return visibleList.get(idx).id;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setVisibleListAndRefreshView() {
        setVisibleList();
        this.notifyDataSetChanged();
    }
}
