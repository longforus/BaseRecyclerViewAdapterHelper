package com.chad.library.adapter.base;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;

public class DefaultDiffCallback<T> extends DiffUtil.ItemCallback<T> {

    @Override
    public boolean areItemsTheSame(@NonNull T t,@NonNull T t1) {
        return t.equals(t1);
    }

    @SuppressLint("DiffUtilEquals")
    @Override
    public boolean areContentsTheSame(@NonNull T t,@NonNull T t1) {
        return t.equals(t1);
    }
}