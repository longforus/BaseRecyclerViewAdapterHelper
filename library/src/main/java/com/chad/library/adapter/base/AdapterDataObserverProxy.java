package com.chad.library.adapter.base;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class AdapterDataObserverProxy extends RecyclerView.AdapterDataObserver {

    RecyclerView.AdapterDataObserver adapterDataObserver;

    int headerCount;

    public AdapterDataObserverProxy(RecyclerView.AdapterDataObserver adapterDataObserver,int headerCount) {
        this.adapterDataObserver = adapterDataObserver;
        this.headerCount = headerCount;
    }

    @Override
    public void onChanged() {
        adapterDataObserver.onChanged();
    }

    @Override
    public void onItemRangeChanged(int positionStart,int itemCount) {
        adapterDataObserver.onItemRangeChanged(positionStart + headerCount,itemCount);
    }

    @Override
    public void onItemRangeChanged(int positionStart,int itemCount,@Nullable Object payload) {
        adapterDataObserver.onItemRangeChanged(positionStart + headerCount,itemCount,payload);
    }

    @Override
    public void onItemRangeInserted(int positionStart,int itemCount) {
        adapterDataObserver.onItemRangeInserted(positionStart + headerCount,itemCount);
    }

    @Override
    public void onItemRangeRemoved(int positionStart,int itemCount) {
        adapterDataObserver.onItemRangeRemoved(positionStart + headerCount,itemCount);
    }

    @Override
    public void onItemRangeMoved(int fromPosition,int toPosition,int itemCount) {
        super.onItemRangeMoved(fromPosition + headerCount,toPosition + headerCount,itemCount);
    }
}