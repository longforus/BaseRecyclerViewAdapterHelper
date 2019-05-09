package com.chad.library.adapter.base;

import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;

/**
 * @author XQ Yang
 * @describe
 * @date 5/9/2019  3:01 PM
 */
public class EmptyLayoutConfig {
    @LayoutRes
    public int layoutID;
    @IdRes
    public int loadingGroupId;
    @IdRes
    public int emptyGroupId;
    @IdRes
    public int errorGroupId;
    @IdRes
    public int retryId;

    public EmptyLayoutConfig(int layoutID,int loadingGroupId,int emptyGroupId,int errorGroupId,int retryId) {
        this.layoutID = layoutID;
        this.loadingGroupId = loadingGroupId;
        this.emptyGroupId = emptyGroupId;
        this.errorGroupId = errorGroupId;
        this.retryId = retryId;
    }
}
