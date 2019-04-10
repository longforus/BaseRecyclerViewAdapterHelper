package com.chad.library.adapter.base;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;

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