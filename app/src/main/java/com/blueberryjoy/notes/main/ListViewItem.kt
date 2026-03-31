package com.blueberryjoy.notes.main;

public class ListViewItem {
    private String title;
    private final ListViewAction action;

    public ListViewItem(String title, ListViewAction action) {
        this.title = title;
        this.action = action;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void onClick() {
        if (action != null) action.onClick(this);
    }
}
