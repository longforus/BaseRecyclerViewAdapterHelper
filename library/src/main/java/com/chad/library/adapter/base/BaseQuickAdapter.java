/*
 * Copyright 2013 Joan Zapata
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.chad.library.adapter.base;

import android.animation.Animator;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.annotation.IdRes;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagedList;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.chad.library.adapter.base.animation.AlphaInAnimation;
import com.chad.library.adapter.base.animation.BaseAnimation;
import com.chad.library.adapter.base.animation.ScaleInAnimation;
import com.chad.library.adapter.base.animation.SlideInBottomAnimation;
import com.chad.library.adapter.base.animation.SlideInLeftAnimation;
import com.chad.library.adapter.base.animation.SlideInRightAnimation;
import com.chad.library.adapter.base.diff.BaseQuickAdapterListUpdateCallback;
import com.chad.library.adapter.base.diff.BaseQuickDiffCallback;
import com.chad.library.adapter.base.entity.IExpandable;
import com.chad.library.adapter.base.loadmore.LoadMoreView;
import com.chad.library.adapter.base.loadmore.SimpleLoadMoreView;
import com.chad.library.adapter.base.util.MultiTypeDelegate;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * https://github.com/CymChad/BaseRecyclerViewAdapterHelper
 */
public abstract class BaseQuickAdapter<T,K extends BaseViewHolder> extends PagedListAdapter<T,K> {

    //load more
    protected boolean mNextLoadEnable = false;
    protected boolean mLoadMoreEnable = false;
    protected boolean mLoading = false;
    protected LoadMoreView mLoadMoreView = new SimpleLoadMoreView();
    protected RequestLoadMoreListener mRequestLoadMoreListener;
    protected boolean mEnableLoadMoreEndClick = false;

    protected boolean usePaged = false;
    //Animation
    /**
     * Use with {@link #openLoadAnimation}
     */
    public static final int ALPHAIN = 0x00000001;
    /**
     * Use with {@link #openLoadAnimation}
     */
    public static final int SCALEIN = 0x00000002;
    /**
     * Use with {@link #openLoadAnimation}
     */
    public static final int SLIDEIN_BOTTOM = 0x00000003;
    /**
     * Use with {@link #openLoadAnimation}
     */
    public static final int SLIDEIN_LEFT = 0x00000004;
    /**
     * Use with {@link #openLoadAnimation}
     */
    public static final int SLIDEIN_RIGHT = 0x00000005;
    protected OnItemClickListener<T> mOnItemClickListener;
    protected OnItemLongClickListener<T> mOnItemLongClickListener;
    protected OnItemChildClickListener<T> mOnItemChildClickListener;
    protected OnItemChildLongClickListener<T> mOnItemChildLongClickListener;
    protected boolean mFirstOnlyEnable = true;
    protected boolean mOpenAnimationEnable = false;
    protected Interpolator mInterpolator = new LinearInterpolator();
    protected int mDuration = 300;
    protected int mLastPosition = -1;

    protected BaseAnimation mCustomAnimation;
    protected BaseAnimation mSelectAnimation = new AlphaInAnimation();
    //header footer
    protected LinearLayout mHeaderLayout;
    protected LinearLayout mFooterLayout;
    //empty
    protected FrameLayout mEmptyLayout;
    protected View mEmptyView;
    protected boolean mIsUseEmpty = true;
    protected boolean mHeadAndEmptyEnable;
    protected boolean mFootAndEmptyEnable;

    protected static final String TAG = BaseQuickAdapter.class.getSimpleName();
    protected Context mContext;
    protected int mLayoutResId;
    protected LayoutInflater mLayoutInflater;
    protected List<T> mData;
    public static final int HEADER_VIEW = 0x00000111;
    public static final int LOADING_VIEW = 0x00000222;
    public static final int FOOTER_VIEW = 0x00000333;
    public static final int EMPTY_VIEW = 0x00000555;
    /**
     * up fetch start
     */
    protected boolean mUpFetchEnable;
    protected boolean mUpFetching;
    protected UpFetchListener mUpFetchListener;
    protected RecyclerView mRecyclerView;
    protected int mPreLoadNumber = 10;

    /**
     * start up fetch position, default is 1.
     */
    protected int mStartUpFetchPosition = 1;
    /**
     * if asFlow is true, footer/header will arrange like normal item view.
     * only works when use {@link GridLayoutManager},and it will ignore span size.
     */
    protected boolean headerViewAsFlow, footerViewAsFlow;

    @IntDef( { ALPHAIN,SCALEIN,SLIDEIN_BOTTOM,SLIDEIN_LEFT,SLIDEIN_RIGHT })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AnimationType {
    }

    protected RecyclerView getRecyclerView() {
        return mRecyclerView;
    }

    private void setRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
    }

    private void checkNotNull() {
        if (getRecyclerView() == null) {
            throw new IllegalStateException("please bind recyclerView first!");
        }
    }

    /**
     * same as recyclerView.setAdapter(), and save the instance of recyclerView
     */
    public void bindToRecyclerView(RecyclerView recyclerView) {
        if (getRecyclerView() != null) {
            throw new IllegalStateException("Don't bind twice");
        }
        setRecyclerView(recyclerView);
        if (sEmptyLayoutConfig != null) {
            setEmptyView(sEmptyLayoutConfig.layoutID,(ViewGroup)getRecyclerView().getParent());
        }
        getRecyclerView().setAdapter(this);
    }

    public void notifyChanged(int pos) {
        notifyItemChanged(pos - mDataObserverProxy.headerCount);
    }

    public void notifyInserted(int pos) {
        notifyItemInserted(pos - mDataObserverProxy.headerCount);
    }

    public void notifyRemoved(int pos) {
        notifyItemRemoved(pos - mDataObserverProxy.headerCount);
    }

    public void notifyMoved(int fromPosition,int toPosition) {
        notifyItemMoved(fromPosition - mDataObserverProxy.headerCount,toPosition - mDataObserverProxy.headerCount);
    }

    /**
     * @see #setOnLoadMoreListener(RequestLoadMoreListener,RecyclerView)
     * @deprecated This method is because it can lead to crash: always call this method while RecyclerView is computing a layout or scrolling.
     * Please use {@link #setOnLoadMoreListener(RequestLoadMoreListener,RecyclerView)}
     */
    @Deprecated
    public void setOnLoadMoreListener(RequestLoadMoreListener requestLoadMoreListener) {
        openLoadMore(requestLoadMoreListener);
    }

    private void openLoadMore(RequestLoadMoreListener requestLoadMoreListener) {
        this.mRequestLoadMoreListener = requestLoadMoreListener;
        mNextLoadEnable = true;
        mLoadMoreEnable = true;
        mLoading = false;
    }

    public void setOnLoadMoreListener(RequestLoadMoreListener requestLoadMoreListener,RecyclerView recyclerView) {
        openLoadMore(requestLoadMoreListener);
        if (getRecyclerView() == null) {
            setRecyclerView(recyclerView);
        }
    }

    /**
     * bind recyclerView {@link #bindToRecyclerView(RecyclerView)} before use!
     *
     * @see #disableLoadMoreIfNotFullPage(RecyclerView)
     */
    public void disableLoadMoreIfNotFullPage() {
        checkNotNull();
        disableLoadMoreIfNotFullPage(getRecyclerView());
    }

    /**
     * check if full page after {@link #setNewData(List)}, if full, it will enable load more again.
     * <p>
     * 不是配置项！！
     * <p>
     * 这个方法是用来检查是否满一屏的，所以只推荐在 {@link #setNewData(List)} 之后使用
     * 原理很简单，先关闭 load more，检查完了再决定是否开启
     * <p>
     * 不是配置项！！
     *
     * @param recyclerView your recyclerView
     * @see #setNewData(List)
     */
    public void disableLoadMoreIfNotFullPage(RecyclerView recyclerView) {
        setEnableLoadMore(false);
        if (recyclerView == null) {
            return;
        }
        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
        if (manager == null) {
            return;
        }
        if (manager instanceof LinearLayoutManager) {
            final LinearLayoutManager linearLayoutManager = (LinearLayoutManager)manager;
            recyclerView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (isFullScreen(linearLayoutManager)) {
                        setEnableLoadMore(true);
                    }
                }
            },50);
        } else if (manager instanceof StaggeredGridLayoutManager) {
            final StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager)manager;
            recyclerView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    final int[] positions = new int[staggeredGridLayoutManager.getSpanCount()];
                    staggeredGridLayoutManager.findLastCompletelyVisibleItemPositions(positions);
                    int pos = getTheBiggestNumber(positions) + 1;
                    if (pos != getItemCount()) {
                        setEnableLoadMore(true);
                    }
                }
            },50);
        }
    }

    private boolean isFullScreen(LinearLayoutManager llm) {
        return (llm.findLastCompletelyVisibleItemPosition() + 1) != getItemCount() || llm.findFirstCompletelyVisibleItemPosition() != 0;
    }

    private int getTheBiggestNumber(int[] numbers) {
        int tmp = -1;
        if (numbers == null || numbers.length == 0) {
            return tmp;
        }
        for (int num : numbers) {
            if (num > tmp) {
                tmp = num;
            }
        }
        return tmp;
    }

    public void setUpFetchEnable(boolean upFetch) {
        this.mUpFetchEnable = upFetch;
    }

    public boolean isUpFetchEnable() {
        return mUpFetchEnable;
    }

    public void setStartUpFetchPosition(int startUpFetchPosition) {
        mStartUpFetchPosition = startUpFetchPosition;
    }

    private void autoUpFetch(int positions) {
        if (!isUpFetchEnable() || isUpFetching()) {
            return;
        }
        if (positions <= mStartUpFetchPosition && mUpFetchListener != null) {
            mUpFetchListener.onUpFetch();
        }
    }

    public boolean isUpFetching() {
        return mUpFetching;
    }

    public void setUpFetching(boolean upFetching) {
        this.mUpFetching = upFetching;
    }

    public void setUpFetchListener(UpFetchListener upFetchListener) {
        mUpFetchListener = upFetchListener;
    }

    @FunctionalInterface
    public interface UpFetchListener {
        void onUpFetch();
    }

    /**
     * up fetch end
     */
    public void setNotDoAnimationCount(int count) {
        mLastPosition = count;
    }

    /**
     * Set custom load more
     *
     * @param loadingView 加载视图
     */
    public void setLoadMoreView(LoadMoreView loadingView) {
        this.mLoadMoreView = loadingView;
    }

    /**
     * Load more view count
     *
     * @return 0 or 1
     */
    public int getLoadMoreViewCount() {
        if (!usePaged) {
            if (mRequestLoadMoreListener == null || !mLoadMoreEnable) {
                return 0;
            }
        }
        if (!mNextLoadEnable && mLoadMoreView.isLoadEndMoreGone()) {
            return 0;
        }
        if (getAdapterCount() == 0) {
            return 0;
        }
        return 1;
    }

    /**
     * Gets to load more locations
     */
    public int getLoadMoreViewPosition() {
        return getHeaderLayoutCount() + getAdapterCount() + getFooterLayoutCount();
    }

    /**
     * @return Whether the Adapter is actively showing load
     * progress.
     */
    public boolean isLoading() {
        return mLoading;
    }

    /**
     * Refresh end, no more data
     */
    public void loadMoreEnd() {
        loadMoreEnd(false);
    }

    /**
     * Refresh end, no more data
     *
     * @param gone if true gone the load more view
     */
    public void loadMoreEnd(boolean gone) {
        if (getLoadMoreViewCount() == 0) {
            setLoadingViewState(gone);
            return;
        }
        mLoading = false;
        mNextLoadEnable = false;
        setLoadingViewState(gone);
    }

    public void setLoadingViewState(boolean gone) {
        if (mLoadMoreView != null) {
            mLoadMoreView.setLoadMoreEndGone(gone);
            if (gone) {
                notifyItemRemoved(getLoadMoreViewPosition());
            } else {
                mLoadMoreView.setLoadMoreStatus(LoadMoreView.STATUS_END);
                notifyDataSetChanged();
            }
        }
    }

    /**
     * Refresh complete
     */
    public void loadMoreComplete() {
        if (getLoadMoreViewCount() == 0) {
            return;
        }
        mLoading = false;
        mNextLoadEnable = true;
        mLoadMoreView.setLoadMoreStatus(LoadMoreView.STATUS_DEFAULT);
        notifyItemChanged(getLoadMoreViewPosition());
    }

    /**
     * Refresh failed
     */
    public void loadMoreFail() {
        if (getLoadMoreViewCount() == 0) {
            return;
        }
        mLoading = false;
        mLoadMoreView.setLoadMoreStatus(LoadMoreView.STATUS_FAIL);
        notifyItemChanged(getLoadMoreViewPosition());
    }

    /**
     * Set the enabled state of load more.
     *
     * @param enable True if load more is enabled, false otherwise.
     */
    public void setEnableLoadMore(boolean enable) {
        int oldLoadMoreCount = getLoadMoreViewCount();
        mLoadMoreEnable = enable;
        int newLoadMoreCount = getLoadMoreViewCount();

        if (oldLoadMoreCount == 1) {
            if (newLoadMoreCount == 0) {
                //notifyItemRemoved(getLoadMoreViewPosition());
                notifyDataSetChanged();
            }
        } else {
            if (newLoadMoreCount == 1) {
                if (!usePaged) {
                    mLoadMoreView.setLoadMoreStatus(LoadMoreView.STATUS_DEFAULT);
                }
                //notifyItemInserted(getLoadMoreViewPosition());
                notifyDataSetChanged();
            }
        }
    }

    /**
     * Returns the enabled status for load more.
     *
     * @return True if load more is enabled, false otherwise.
     */
    public boolean isLoadMoreEnable() {
        return mLoadMoreEnable;
    }

    /**
     * Sets the duration of the animation.
     *
     * @param duration The length of the animation, in milliseconds.
     */
    public void setDuration(int duration) {
        mDuration = duration;
    }

    /**
     * If you have added headeview, the notification view refreshes.
     * Do not need to care about the number of headview, only need to pass in the position of the final view
     */
    public final void refreshNotifyItemChanged(int position) {
        notifyItemChanged(position + getHeaderLayoutCount());
    }

    public boolean isUsePaged() {
        return usePaged;
    }

    public void setUsePaged(boolean usePaged) {
        this.usePaged = usePaged;
    }

    /**
     * Same as QuickAdapter#QuickAdapter(Context,int) but with
     * some initialization data.
     *
     * @param layoutResId The layout resource id of each item.
     * @param data A new list is created out of this one to avoid mutable list
     */
    public BaseQuickAdapter(@LayoutRes int layoutResId,@Nullable List<T> data,@Nullable DiffUtil.ItemCallback<T> diffCallback) {
        super(diffCallback == null ? new DefaultDiffCallback<T>() : diffCallback);
        usePaged = diffCallback != null;
        this.mData = data == null ? new ArrayList<T>() : data;
        if (layoutResId != 0) {
            this.mLayoutResId = layoutResId;
        }
    }

    public BaseQuickAdapter(@LayoutRes int layoutResId,@Nullable List<T> data) {
        this(layoutResId,data,null);
    }

    public BaseQuickAdapter(@Nullable List<T> data,@NonNull DiffUtil.ItemCallback<T> diffCallback) {
        this(0,data,diffCallback);
    }

    public BaseQuickAdapter(@LayoutRes int layoutResId,@NonNull DiffUtil.ItemCallback<T> diffCallback) {
        this(layoutResId,null,diffCallback);
    }

    public BaseQuickAdapter(@Nullable List<T> data) {
        this(0,data,null);
    }

    public BaseQuickAdapter(@LayoutRes int layoutResId) {
        this(layoutResId,null,null);
    }

    protected AdapterDataObserverProxy mDataObserverProxy;
    public RecyclerView.AdapterDataObserver srcObserver;

    @Override
    public void registerAdapterDataObserver(@NonNull RecyclerView.AdapterDataObserver observer) {
        srcObserver = observer;
        mDataObserverProxy = new AdapterDataObserverProxy(observer,getHeaderLayoutCount() + getEmptyViewCount());
        super.registerAdapterDataObserver(mDataObserverProxy);
    }

    @Override
    public void unregisterAdapterDataObserver(@NonNull RecyclerView.AdapterDataObserver observer) {
        if (observer == srcObserver) {
            super.unregisterAdapterDataObserver(mDataObserverProxy);
        } else {
            super.unregisterAdapterDataObserver(observer);
        }
    }

    /**
     * setting up a new instance to data;
     */
    public void setNewData(@Nullable List<T> data) {
        this.mData = data == null ? new ArrayList<T>() : data;
        if (mRequestLoadMoreListener != null) {
            mNextLoadEnable = true;
            mLoadMoreEnable = true;
            mLoading = false;
            mLoadMoreView.setLoadMoreStatus(LoadMoreView.STATUS_DEFAULT);
        }
        mLastPosition = -1;
        notifyDataSetChanged();
    }

    /**
     * use Diff setting up a new instance to data
     *
     * @param baseQuickDiffCallback implementation {@link BaseQuickDiffCallback}
     */
    public void setNewDiffData(@NonNull BaseQuickDiffCallback<T> baseQuickDiffCallback) {
        setNewDiffData(baseQuickDiffCallback,false);
    }

    /**
     * use Diff setting up a new instance to data.
     * this is sync, if you need use async, see {@link #setNewDiffData(DiffUtil.DiffResult,List)}.
     *
     * @param baseQuickDiffCallback implementation {@link BaseQuickDiffCallback}.
     * @param detectMoves Whether to detect the movement of the Item
     */
    public void setNewDiffData(@NonNull BaseQuickDiffCallback<T> baseQuickDiffCallback,boolean detectMoves) {
        if (getEmptyViewCount() == 1) {
            // If the current view is an empty view, set the new data directly without diff
            setNewData(baseQuickDiffCallback.getNewList());
            return;
        }
        baseQuickDiffCallback.setOldList(this.getData());
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(baseQuickDiffCallback,detectMoves);
        diffResult.dispatchUpdatesTo(new BaseQuickAdapterListUpdateCallback(this));
        mData = baseQuickDiffCallback.getNewList();
    }

    /**
     * use DiffResult setting up a new instance to data
     *
     * If you need to use async computing Diff, please use this method.
     * You only need to tell the calculation result,
     * this adapter does not care about the calculation process.
     *
     * @param diffResult DiffResult
     * @param newData New Data
     */
    public void setNewDiffData(@NonNull DiffUtil.DiffResult diffResult,@NonNull List<T> newData) {
        if (getEmptyViewCount() == 1) {
            // If the current view is an empty view, set the new data directly without diff
            setNewData(newData);
            return;
        }
        diffResult.dispatchUpdatesTo(new BaseQuickAdapterListUpdateCallback(BaseQuickAdapter.this));
        mData = newData;
    }

    /**
     * insert  a item associated with the specified position of adapter
     *
     * @deprecated use {@link #addData(int,Object)} instead
     */
    @Deprecated
    public void add(@IntRange(from = 0) int position,@NonNull T item) {
        addData(position,item);
    }

    /**
     * add one new data in to certain location
     */
    public void addData(@IntRange(from = 0) int position,@NonNull T data) {
        mData.add(position,data);
        notifyItemInserted(position + getHeaderLayoutCount());
        compatibilityDataSizeChanged(1);
    }

    /**
     * add one new data
     */
    public void addData(@NonNull T data) {
        mData.add(data);
        notifyItemInserted(getAdapterCount() + getHeaderLayoutCount());
        compatibilityDataSizeChanged(1);
    }

    /**
     * remove the item associated with the specified position of adapter
     */
    public void remove(@IntRange(from = 0) int position) {
        mData.remove(position);
        int internalPosition = position + getHeaderLayoutCount();
        notifyItemRemoved(internalPosition);
        compatibilityDataSizeChanged(0);
        notifyItemRangeChanged(internalPosition,getAdapterCount() - internalPosition);
    }

    /**
     * change data
     */
    public void setData(@IntRange(from = 0) int index,@NonNull T data) {
        mData.set(index,data);
        notifyItemChanged(index + getHeaderLayoutCount());
    }

    /**
     * add new data in to certain location
     *
     * @param position the insert position
     * @param newData the new data collection
     */
    public void addData(@IntRange(from = 0) int position,@NonNull Collection<? extends T> newData) {
        mData.addAll(position,newData);
        notifyItemRangeInserted(position + getHeaderLayoutCount(),newData.size());
        compatibilityDataSizeChanged(newData.size());
    }

    /**
     * add new data to the end of mData
     *
     * @param newData the new data collection
     */
    public void addData(@NonNull Collection<? extends T> newData) {
        mData.addAll(newData);
        notifyItemRangeInserted(getAdapterCount() - newData.size() + getHeaderLayoutCount(),newData.size());
        compatibilityDataSizeChanged(newData.size());
    }

    /**
     * use data to replace all item in mData. this method is different {@link #setNewData(List)},
     * it doesn't change the mData reference
     *
     * @param data data collection
     */
    public void replaceData(@NonNull Collection<? extends T> data) {
        // 不是同一个引用才清空列表
        if (data != mData) {
            mData.clear();
            mData.addAll(data);
        }
        notifyDataSetChanged();
    }

    /**
     * compatible getLoadMoreViewCount and getEmptyViewCount may change
     *
     * @param size Need compatible data size
     */
    private void compatibilityDataSizeChanged(int size) {
        final int dataSize = mData == null ? 0 : getAdapterCount();
        if (dataSize == size) {
            notifyDataSetChanged();
        }
    }

    /**
     * Get the data of list
     *
     * @return 列表数据
     */
    @NonNull
    public List<T> getData() {
        return mData;
    }

    /**
     * Get the data item associated with the specified position in the data set.
     *
     * @param position Position of the item whose data we want within the adapter's
     * data set.
     * @return The data at the specified position.
     */
    @Nullable
    public T getItem(@IntRange(from = 0) int position) {
        if (usePaged) {
            return super.getItem(position);
        } else if (position >= 0 && position < mData.size()) {
            return mData.get(position);
        } else {
            return null;
        }
    }

    /**
     * if addHeaderView will be return 1, if not will be return 0
     */
    public int getHeaderLayoutCount() {
        if (mHeaderLayout == null || mHeaderLayout.getChildCount() == 0) {
            return 0;
        }
        return 1;
    }

    /**
     * if addFooterView will be return 1, if not will be return 0
     */
    public int getFooterLayoutCount() {
        if (mFooterLayout == null || mFooterLayout.getChildCount() == 0) {
            return 0;
        }
        return 1;
    }

    /**
     * if show empty view will be return 1 or not will be return 0
     */
    public int getEmptyViewCount() {
        if (mEmptyLayout == null || mEmptyLayout.getChildCount() == 0) {
            return 0;
        }
        if (!mIsUseEmpty) {
            return 0;
        }
        if (getAdapterCount() != 0) {
            return 0;
        }
        return 1;
    }

    @Override
    public int getItemCount() {
        int count;
        if (1 == getEmptyViewCount()) {
            count = 1;
            if (mHeadAndEmptyEnable && getHeaderLayoutCount() != 0) {
                count++;
            }
            if (mFootAndEmptyEnable && getFooterLayoutCount() != 0) {
                count++;
            }
        } else {
            count = getHeaderLayoutCount() + getAdapterCount() + getFooterLayoutCount() + getLoadMoreViewCount();
        }
        return count;
    }

    public static EmptyLayoutConfig sEmptyLayoutConfig;

    protected PagedList<T> mCurrentList;

    @Override
    public void onCurrentListChanged(@Nullable PagedList<T> previousList,@Nullable PagedList<T> currentList) {
        if (previousList != currentList) {
            mCurrentList = currentList;
            if (currentList == null || currentList.size() == 0) {
                //setEnableLoadMore(false);
                setEmptyViewState(0);
                if (getAdapterCount() != 0) {
                    mLoadMoreView.setLoadMoreStatus(LoadMoreView.STATUS_LOADING);
                }
                if (currentList != null) {
                    currentList.addWeakCallback(null,new PagedList.Callback() {
                        @Override
                        public void onChanged(int position,int count) {
                            onPagedListOnChanged(position,count);
                        }

                        @Override
                        public void onInserted(int position,int count) {
                            onPagedListOnInserted(position,count);
                        }

                        @Override
                        public void onRemoved(int position,int count) {
                            onPagedListOnRemoved(position,count);
                        }
                    });
                }
            }
        }
    }

    protected void onPagedListOnRemoved(int position,int count) {

    }

    protected void onPagedListOnInserted(int position,int count) {
        if (position == 0 && count != 0) {
            if (mIsUseEmpty) {
                mIsUseEmpty = false;
                notifyDataSetChanged();
            }
            //setEnableLoadMore(true);
        }
    }

    protected void onPagedListOnChanged(int position,int count) {

    }

    @Override
    public int getItemViewType(int position) {
        if (getEmptyViewCount() == 1) {
            boolean header = mHeadAndEmptyEnable && getHeaderLayoutCount() != 0;
            switch (position) {
                case 0:
                    if (header) {
                        return HEADER_VIEW;
                    } else {
                        return EMPTY_VIEW;
                    }
                case 1:
                    if (header) {
                        return EMPTY_VIEW;
                    } else {
                        return FOOTER_VIEW;
                    }
                case 2:
                    return FOOTER_VIEW;
                default:
                    return EMPTY_VIEW;
            }
        }
        int numHeaders = getHeaderLayoutCount();
        if (position < numHeaders) {
            return HEADER_VIEW;
        } else {
            int adjPosition = position - numHeaders;
            int adapterCount = getAdapterCount();
            if (adjPosition < adapterCount) {
                return getDefItemViewType(adjPosition);
            } else {
                adjPosition = adjPosition - adapterCount;
                int numFooters = getFooterLayoutCount();
                if (adjPosition < numFooters) {
                    return FOOTER_VIEW;
                } else {
                    return LOADING_VIEW;
                }
            }
        }
    }

    public int getAdapterCount() {
        //todo padgedList  不支持直接添加项目,我现在有一个大胆的想法,pagedList和mData同用会不会能解决这个问题呢?
        return usePaged ? super.getItemCount() : mData.size();
    }

    protected int getDefItemViewType(int position) {
        if (mMultiTypeDelegate != null) {
            return mMultiTypeDelegate.getDefItemViewType(getItem(position));
        }
        return super.getItemViewType(position);
    }

    @NonNull
    @Override
    public K onCreateViewHolder(@NonNull ViewGroup parent,int viewType) {
        K baseViewHolder = null;
        this.mContext = parent.getContext();
        this.mLayoutInflater = LayoutInflater.from(mContext);
        switch (viewType) {
            case LOADING_VIEW:
                baseViewHolder = getLoadingView(parent);
                break;
            case HEADER_VIEW:
                baseViewHolder = createBaseViewHolder(mHeaderLayout);
                break;
            case EMPTY_VIEW:
                baseViewHolder = createBaseViewHolder(mEmptyLayout);
                break;
            case FOOTER_VIEW:
                baseViewHolder = createBaseViewHolder(mFooterLayout);
                break;
            default:
                baseViewHolder = onCreateDefViewHolder(parent,viewType);
                bindViewClickListener(baseViewHolder);
        }
        baseViewHolder.setAdapter(this);
        return baseViewHolder;
    }

    private K getLoadingView(ViewGroup parent) {
        View view = getItemView(mLoadMoreView.getLayoutId(),parent);
        K holder = createBaseViewHolder(view);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoadMoreView.getLoadMoreStatus() == LoadMoreView.STATUS_FAIL) {
                    notifyLoadMoreToLoading();
                }
                if (mEnableLoadMoreEndClick && mLoadMoreView.getLoadMoreStatus() == LoadMoreView.STATUS_END) {
                    notifyLoadMoreToLoading();
                }
            }
        });
        return holder;
    }

    /**
     * The notification starts the callback and loads more
     */
    public void notifyLoadMoreToLoading() {
        if (mLoadMoreView.getLoadMoreStatus() == LoadMoreView.STATUS_LOADING) {
            return;
        }
        mLoadMoreView.setLoadMoreStatus(LoadMoreView.STATUS_DEFAULT);
        notifyItemChanged(getLoadMoreViewPosition());
    }

    /**
     * Load more without data when settings are clicked loaded
     */
    public void enableLoadMoreEndClick(boolean enable) {
        mEnableLoadMoreEndClick = enable;
    }

    /**
     * Called when a view created by this adapter has been attached to a window.
     * simple to solve item will layout using all
     * {@link #setFullSpan(RecyclerView.ViewHolder)}
     */
    @Override
    public void onViewAttachedToWindow(@NonNull K holder) {
        super.onViewAttachedToWindow(holder);
        int type = holder.getItemViewType();
        if (type == EMPTY_VIEW || type == HEADER_VIEW || type == FOOTER_VIEW || type == LOADING_VIEW) {
            setFullSpan(holder);
        } else {
            addAnimation(holder);
        }
    }

    /**
     * When set to true, the item will layout using all span area. That means, if orientation
     * is vertical, the view will have full width; if orientation is horizontal, the view will
     * have full height.
     * if the hold view use StaggeredGridLayoutManager they should using all span area
     *
     * @param holder True if this item should traverse all spans.
     */
    protected void setFullSpan(RecyclerView.ViewHolder holder) {
        if (holder.itemView.getLayoutParams() instanceof StaggeredGridLayoutManager.LayoutParams) {
            StaggeredGridLayoutManager.LayoutParams params = (StaggeredGridLayoutManager.LayoutParams)holder.itemView.getLayoutParams();
            params.setFullSpan(true);
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull final RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
        if (manager instanceof GridLayoutManager) {
            final GridLayoutManager gridManager = ((GridLayoutManager)manager);
            gridManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    int type = getItemViewType(position);
                    if (type == HEADER_VIEW && isHeaderViewAsFlow()) {
                        return 1;
                    }
                    if (type == FOOTER_VIEW && isFooterViewAsFlow()) {
                        return 1;
                    }
                    if (mSpanSizeLookup == null) {
                        return isFixedViewType(type) ? gridManager.getSpanCount() : 1;
                    } else {
                        return (isFixedViewType(type)) ? gridManager.getSpanCount() : mSpanSizeLookup.getSpanSize(gridManager,position - getHeaderLayoutCount());
                    }
                }
            });
        }
    }

    protected boolean isFixedViewType(int type) {
        return type == EMPTY_VIEW || type == HEADER_VIEW || type == FOOTER_VIEW || type == LOADING_VIEW;
    }

    public void setHeaderViewAsFlow(boolean headerViewAsFlow) {
        this.headerViewAsFlow = headerViewAsFlow;
    }

    public boolean isHeaderViewAsFlow() {
        return headerViewAsFlow;
    }

    public void setFooterViewAsFlow(boolean footerViewAsFlow) {
        this.footerViewAsFlow = footerViewAsFlow;
    }

    public boolean isFooterViewAsFlow() {
        return footerViewAsFlow;
    }

    private SpanSizeLookup mSpanSizeLookup;

    public interface SpanSizeLookup {
        int getSpanSize(GridLayoutManager gridLayoutManager,int position);
    }

    /**
     * @param spanSizeLookup instance to be used to query number of spans occupied by each item
     */
    public void setSpanSizeLookup(SpanSizeLookup spanSizeLookup) {
        this.mSpanSizeLookup = spanSizeLookup;
    }

    /**
     * To bind different types of holder and solve different the bind events
     *
     * @see #getDefItemViewType(int)
     */
    @Override
    public void onBindViewHolder(@NonNull K holder,int position) {
        //Add up fetch logic, almost like load more, but simpler.
        autoUpFetch(position);
        //Do not move position, need to change before LoadMoreView binding
        autoLoadMore(position);
        int viewType = holder.getItemViewType();
        switch (viewType) {
            case LOADING_VIEW:
                mLoadMoreView.convert(holder);
                break;
            case HEADER_VIEW:
                break;
            case EMPTY_VIEW:
                break;
            case FOOTER_VIEW:
                break;
            case 0:
            default:
                T item = getItem(position - getHeaderLayoutCount());
                if (item != null) {
                    convert(holder,item);
                }
                break;
        }
    }

    /**
     * To bind different types of holder and solve different the bind events
     *
     * the ViewHolder is currently bound to old data and Adapter may run an efficient partial
     * update using the payload info.  If the payload is empty,  Adapter run a full bind.
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the
     * item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     * @param payloads A non-null list of merged payloads. Can be empty list if requires full
     * update.
     * @see #getDefItemViewType(int)
     */
    @Override
    public void onBindViewHolder(@NonNull K holder,int position,@NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder,position);
            return;
        }
        //Add up fetch logic, almost like load more, but simpler.
        autoUpFetch(position);
        //Do not move position, need to change before LoadMoreView binding
        autoLoadMore(position);
        int viewType = holder.getItemViewType();

        switch (viewType) {
            case LOADING_VIEW:
                mLoadMoreView.convert(holder);
                break;
            case HEADER_VIEW:
                break;
            case EMPTY_VIEW:
                break;
            case FOOTER_VIEW:
                break;
            case 0:
            default:
                convertPayloads(holder,getItem(position - getHeaderLayoutCount()),payloads);
                break;
        }
    }

    private void bindViewClickListener(final BaseViewHolder baseViewHolder) {
        if (baseViewHolder == null) {
            return;
        }
        final View view = baseViewHolder.itemView;
        if (getOnItemClickListener() != null) {
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = baseViewHolder.getAdapterPosition();
                    if (position == RecyclerView.NO_POSITION) {
                        return;
                    }
                    //position -= getHeaderLayoutCount();
                    setOnItemClick(v,position);
                }
            });
        }
        if (getOnItemLongClickListener() != null) {
            view.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int position = baseViewHolder.getAdapterPosition();
                    if (position == RecyclerView.NO_POSITION) {
                        return false;
                    }
                    //position -= getHeaderLayoutCount();
                    return setOnItemLongClick(v,position);
                }
            });
        }
    }

    /**
     * override this method if you want to override click event logic
     */
    public void setOnItemClick(View v,int position) {
        getOnItemClickListener().onItemClick(getItem(position),v,position);
    }

    /**
     * override this method if you want to override longClick event logic
     */
    public boolean setOnItemLongClick(View v,int position) {
        return getOnItemLongClickListener().onItemLongClick(getItem(position),v,position);
    }

    private MultiTypeDelegate<T> mMultiTypeDelegate;

    public void setMultiTypeDelegate(MultiTypeDelegate<T> multiTypeDelegate) {
        mMultiTypeDelegate = multiTypeDelegate;
    }

    public MultiTypeDelegate<T> getMultiTypeDelegate() {
        return mMultiTypeDelegate;
    }

    protected K onCreateDefViewHolder(ViewGroup parent,int viewType) {
        int layoutId = mLayoutResId;
        if (mMultiTypeDelegate != null) {
            layoutId = mMultiTypeDelegate.getLayoutId(viewType);
        }
        return createBaseViewHolder(parent,layoutId);
    }

    protected K createBaseViewHolder(ViewGroup parent,int layoutResId) {
        return createBaseViewHolder(getItemView(layoutResId,parent));
    }

    /**
     * if you want to use subclass of BaseViewHolder in the adapter,
     * you must override the method to create new ViewHolder.
     *
     * @param view view
     * @return new ViewHolder
     */
    @SuppressWarnings("unchecked")
    protected K createBaseViewHolder(View view) {
        Class temp = getClass();
        Class z = null;
        while (z == null && null != temp) {
            z = getInstancedGenericKClass(temp);
            temp = temp.getSuperclass();
        }
        K k;
        // 泛型擦除会导致z为null
        if (z == null) {
            k = (K)new BaseViewHolder(view);
        } else {
            k = createGenericKInstance(z,view);
        }
        return k != null ? k : (K)new BaseViewHolder(view);
    }

    /**
     * try to create Generic K instance
     */
    @SuppressWarnings("unchecked")
    private K createGenericKInstance(Class z,View view) {
        try {
            Constructor constructor;
            // inner and unstatic class
            if (z.isMemberClass() && !Modifier.isStatic(z.getModifiers())) {
                constructor = z.getDeclaredConstructor(getClass(),View.class);
                constructor.setAccessible(true);
                return (K)constructor.newInstance(this,view);
            } else {
                constructor = z.getDeclaredConstructor(View.class);
                constructor.setAccessible(true);
                return (K)constructor.newInstance(view);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * get generic parameter K
     */
    private Class getInstancedGenericKClass(Class z) {
        Type type = z.getGenericSuperclass();
        if (type instanceof ParameterizedType) {
            Type[] types = ((ParameterizedType)type).getActualTypeArguments();
            for (Type temp : types) {
                if (temp instanceof Class) {
                    Class tempClass = (Class)temp;
                    if (BaseViewHolder.class.isAssignableFrom(tempClass)) {
                        return tempClass;
                    }
                } else if (temp instanceof ParameterizedType) {
                    Type rawType = ((ParameterizedType)temp).getRawType();
                    if (rawType instanceof Class && BaseViewHolder.class.isAssignableFrom((Class<?>)rawType)) {
                        return (Class<?>)rawType;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Return root layout of header
     */

    public LinearLayout getHeaderLayout() {
        return mHeaderLayout;
    }

    /**
     * Return root layout of footer
     */
    public LinearLayout getFooterLayout() {
        return mFooterLayout;
    }

    /**
     * Append header to the rear of the mHeaderLayout.
     */
    public int addHeaderView(View header) {
        return addHeaderView(header,-1);
    }

    /**
     * Add header view to mHeaderLayout and set header view position in mHeaderLayout.
     * When index = -1 or index >= child count in mHeaderLayout,
     * the effect of this method is the same as that of {@link #addHeaderView(View)}.
     *
     * @param index the position in mHeaderLayout of this header.
     * When index = -1 or index >= child count in mHeaderLayout,
     * the effect of this method is the same as that of {@link #addHeaderView(View)}.
     */
    public int addHeaderView(View header,int index) {
        return addHeaderView(header,index,LinearLayout.VERTICAL);
    }

    /**
     *
     */
    public int addHeaderView(View header,final int index,int orientation) {
        if (mHeaderLayout == null) {
            mHeaderLayout = new LinearLayout(header.getContext());
            if (orientation == LinearLayout.VERTICAL) {
                mHeaderLayout.setOrientation(LinearLayout.VERTICAL);
                mHeaderLayout.setLayoutParams(new RecyclerView.LayoutParams(MATCH_PARENT,WRAP_CONTENT));
            } else {
                mHeaderLayout.setOrientation(LinearLayout.HORIZONTAL);
                mHeaderLayout.setLayoutParams(new RecyclerView.LayoutParams(WRAP_CONTENT,MATCH_PARENT));
            }
        }
        final int childCount = mHeaderLayout.getChildCount();
        int mIndex = index;
        if (index < 0 || index > childCount) {
            mIndex = childCount;
        }
        mHeaderLayout.addView(header,mIndex);
        if (mHeaderLayout.getChildCount() == 1) {
            int position = getHeaderViewPosition();
            if (position != -1) {
                notifyItemInserted(position);
            }
        }
        return mIndex;
    }

    public int setHeaderView(View header) {
        return setHeaderView(header,0,LinearLayout.VERTICAL);
    }

    public int setHeaderView(View header,int index) {
        return setHeaderView(header,index,LinearLayout.VERTICAL);
    }

    public int setHeaderView(View header,int index,int orientation) {
        if (mHeaderLayout == null || mHeaderLayout.getChildCount() <= index) {
            return addHeaderView(header,index,orientation);
        } else {
            mHeaderLayout.removeViewAt(index);
            mHeaderLayout.addView(header,index);
            return index;
        }
    }

    /**
     * Append footer to the rear of the mFooterLayout.
     */
    public int addFooterView(View footer) {
        return addFooterView(footer,-1,LinearLayout.VERTICAL);
    }

    public int addFooterView(View footer,int index) {
        return addFooterView(footer,index,LinearLayout.VERTICAL);
    }

    /**
     * Add footer view to mFooterLayout and set footer view position in mFooterLayout.
     * When index = -1 or index >= child count in mFooterLayout,
     * the effect of this method is the same as that of {@link #addFooterView(View)}.
     *
     * @param index the position in mFooterLayout of this footer.
     * When index = -1 or index >= child count in mFooterLayout,
     * the effect of this method is the same as that of {@link #addFooterView(View)}.
     */
    public int addFooterView(View footer,int index,int orientation) {
        if (mFooterLayout == null) {
            mFooterLayout = new LinearLayout(footer.getContext());
            if (orientation == LinearLayout.VERTICAL) {
                mFooterLayout.setOrientation(LinearLayout.VERTICAL);
                mFooterLayout.setLayoutParams(new RecyclerView.LayoutParams(MATCH_PARENT,WRAP_CONTENT));
            } else {
                mFooterLayout.setOrientation(LinearLayout.HORIZONTAL);
                mFooterLayout.setLayoutParams(new RecyclerView.LayoutParams(WRAP_CONTENT,MATCH_PARENT));
            }
        }
        final int childCount = mFooterLayout.getChildCount();
        if (index < 0 || index > childCount) {
            index = childCount;
        }
        mFooterLayout.addView(footer,index);
        if (mFooterLayout.getChildCount() == 1) {
            int position = getFooterViewPosition();
            if (position != -1) {
                notifyItemInserted(position);
            }
        }
        return index;
    }

    public int setFooterView(View header) {
        return setFooterView(header,0,LinearLayout.VERTICAL);
    }

    public int setFooterView(View header,int index) {
        return setFooterView(header,index,LinearLayout.VERTICAL);
    }

    public int setFooterView(View header,int index,int orientation) {
        if (mFooterLayout == null || mFooterLayout.getChildCount() <= index) {
            return addFooterView(header,index,orientation);
        } else {
            mFooterLayout.removeViewAt(index);
            mFooterLayout.addView(header,index);
            return index;
        }
    }

    /**
     * remove header view from mHeaderLayout.
     * When the child count of mHeaderLayout is 0, mHeaderLayout will be set to null.
     */
    public void removeHeaderView(View header) {
        if (getHeaderLayoutCount() == 0) {
            return;
        }

        mHeaderLayout.removeView(header);
        if (mHeaderLayout.getChildCount() == 0) {
            int position = getHeaderViewPosition();
            if (position != -1) {
                notifyItemRemoved(position);
            }
        }
    }

    /**
     * remove footer view from mFooterLayout,
     * When the child count of mFooterLayout is 0, mFooterLayout will be set to null.
     */
    public void removeFooterView(View footer) {
        if (getFooterLayoutCount() == 0) {
            return;
        }

        mFooterLayout.removeView(footer);
        if (mFooterLayout.getChildCount() == 0) {
            int position = getFooterViewPosition();
            if (position != -1) {
                notifyItemRemoved(position);
            }
        }
    }

    /**
     * remove all header view from mHeaderLayout and set null to mHeaderLayout
     */
    public void removeAllHeaderView() {
        if (getHeaderLayoutCount() == 0) {
            return;
        }

        mHeaderLayout.removeAllViews();
        int position = getHeaderViewPosition();
        if (position != -1) {
            notifyItemRemoved(position);
        }
    }

    /**
     * remove all footer view from mFooterLayout and set null to mFooterLayout
     */
    public void removeAllFooterView() {
        if (getFooterLayoutCount() == 0) {
            return;
        }

        mFooterLayout.removeAllViews();
        int position = getFooterViewPosition();
        if (position != -1) {
            notifyItemRemoved(position);
        }
    }

    private int getHeaderViewPosition() {
        //Return to header view notify position
        if (getEmptyViewCount() == 1) {
            if (mHeadAndEmptyEnable) {
                return 0;
            }
        } else {
            return 0;
        }
        return -1;
    }

    private int getFooterViewPosition() {
        //Return to footer view notify position
        if (getEmptyViewCount() == 1) {
            int position = 1;
            if (mHeadAndEmptyEnable && getHeaderLayoutCount() != 0) {
                position++;
            }
            if (mFootAndEmptyEnable) {
                return position;
            }
        } else {
            return getHeaderLayoutCount() + getAdapterCount();
        }
        return -1;
    }

    public void setEmptyView(int layoutResId,ViewGroup viewGroup) {
        if (mEmptyView == null) {
            mEmptyView = LayoutInflater.from(viewGroup.getContext()).inflate(layoutResId,viewGroup,false);
        }
        configEmptyView(mEmptyView);
    }

    /**
     * bind recyclerView {@link #bindToRecyclerView(RecyclerView)} before use!
     *
     * @see #bindToRecyclerView(RecyclerView)
     */
    @Deprecated
    public void setEmptyView(int layoutResId) {
        checkNotNull();
        setEmptyView(layoutResId,getRecyclerView());
    }

    public void configEmptyView(View emptyView) {
        int oldItemCount = getItemCount();
        boolean insert = false;
        if (mEmptyLayout == null) {
            mEmptyLayout = new FrameLayout(emptyView.getContext());
            final RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT,RecyclerView.LayoutParams.MATCH_PARENT);
            final ViewGroup.LayoutParams lp = emptyView.getLayoutParams();
            if (lp != null) {
                layoutParams.width = lp.width;
                layoutParams.height = lp.height;
            }
            mEmptyLayout.setLayoutParams(layoutParams);
            insert = true;
        }
        mEmptyLayout.removeAllViews();
        mEmptyLayout.addView(emptyView);
        if (sEmptyLayoutConfig != null) {
            emptyView.findViewById(sEmptyLayoutConfig.retryId).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onRetryLoad();
                }
            });
        }
        mIsUseEmpty = true;
        if (insert && getEmptyViewCount() == 1) {
            int position = 0;
            if (mHeadAndEmptyEnable && getHeaderLayoutCount() != 0) {
                position++;
            }
            if (getItemCount() > oldItemCount) {
                notifyItemInserted(position);
            } else {
                notifyDataSetChanged();
            }
        }
    }

    public Function0<Unit> onRetryFun;

    public void onRetryLoad() {
        setEmptyViewState(0);
        if (onRetryFun != null) {
            onRetryFun.invoke();
        }
    }

    public void setEmptyViewState(int state) {
        if (mEmptyView == null) {
            return;
        }
        View loading = mEmptyView.findViewById(sEmptyLayoutConfig.loadingGroupId);
        loading.setVisibility(View.GONE);
        View empty = mEmptyView.findViewById(sEmptyLayoutConfig.emptyGroupId);
        empty.setVisibility(View.GONE);
        View error = mEmptyView.findViewById(sEmptyLayoutConfig.errorGroupId);
        error.setVisibility(View.GONE);
        switch (state) {
            case 0:
                loading.setVisibility(View.VISIBLE);
                break;
            case 1:
                empty.setVisibility(View.VISIBLE);
                break;
            case 2:
                error.setVisibility(View.VISIBLE);
                break;
            default:
        }
        mIsUseEmpty = true;
    }

    /**
     * Call before {@link RecyclerView#setAdapter(RecyclerView.Adapter)}
     *
     * @param isHeadAndEmpty false will not show headView if the data is empty true will show emptyView and headView
     */
    public void setHeaderAndEmpty(boolean isHeadAndEmpty) {
        setHeaderFooterEmpty(isHeadAndEmpty,false);
    }

    /**
     * set emptyView show if adapter is empty and want to show headview and footview
     * Call before {@link RecyclerView#setAdapter(RecyclerView.Adapter)}
     */
    public void setHeaderFooterEmpty(boolean isHeadAndEmpty,boolean isFootAndEmpty) {
        mHeadAndEmptyEnable = isHeadAndEmpty;
        mFootAndEmptyEnable = isFootAndEmpty;
    }

    /**
     * Set whether to use empty view
     */
    public void isUseEmpty(boolean isUseEmpty) {
        mIsUseEmpty = isUseEmpty;
    }

    /**
     * When the current adapter is empty, the BaseQuickAdapter can display a special view
     * called the empty view. The empty view is used to provide feedback to the user
     * that no data is available in this AdapterView.
     *
     * @return The view to show if the adapter is empty.
     */
    public FrameLayout getEmptyLayout() {
        return mEmptyLayout;
    }

    public View getEmptyView() {
        return mEmptyView;
    }

    @Deprecated
    public void setAutoLoadMoreSize(int preLoadNumber) {
        setPreLoadNumber(preLoadNumber);
    }

    public void setPreLoadNumber(int preLoadNumber) {
        if (preLoadNumber > 1) {
            mPreLoadNumber = preLoadNumber;
        }
    }

    private void autoLoadMore(int position) {
        if (getLoadMoreViewCount() == 0) {
            return;
        }
        if (position < getItemCount() - mPreLoadNumber) {
            return;
        }
        if (mLoadMoreView.getLoadMoreStatus() != LoadMoreView.STATUS_DEFAULT) {
            return;
        }
        if (!usePaged) {
            mLoadMoreView.setLoadMoreStatus(LoadMoreView.STATUS_LOADING);
            if (!mLoading) {
                mLoading = true;
                if (getRecyclerView() != null) {
                    getRecyclerView().post(new Runnable() {
                        @Override
                        public void run() {
                            mRequestLoadMoreListener.onLoadMoreRequested();
                        }
                    });
                } else {
                    mRequestLoadMoreListener.onLoadMoreRequested();
                }
            }
        }
    }

    /**
     * add animation when you want to show time
     */
    private void addAnimation(RecyclerView.ViewHolder holder) {
        if (mOpenAnimationEnable) {
            if (!mFirstOnlyEnable || holder.getLayoutPosition() > mLastPosition) {
                BaseAnimation animation = null;
                if (mCustomAnimation != null) {
                    animation = mCustomAnimation;
                } else {
                    animation = mSelectAnimation;
                }
                for (Animator anim : animation.getAnimators(holder.itemView)) {
                    startAnim(anim,holder.getLayoutPosition());
                }
                mLastPosition = holder.getLayoutPosition();
            }
        }
    }

    /**
     * set anim to start when loading
     */
    protected void startAnim(Animator anim,int index) {
        anim.setDuration(mDuration).start();
        anim.setInterpolator(mInterpolator);
    }

    /**
     * @param layoutResId ID for an XML layout resource to load
     * @param parent Optional view to be the parent of the generated hierarchy or else simply an object that
     * provides a set of LayoutParams values for root of the returned
     * hierarchy
     * @return view will be return
     */
    protected View getItemView(@LayoutRes int layoutResId,ViewGroup parent) {
        return mLayoutInflater.inflate(layoutResId,parent,false);
    }

    @FunctionalInterface
    public interface RequestLoadMoreListener {
        void onLoadMoreRequested();
    }

    /**
     * Set the view animation type.
     *
     * @param animationType One of {@link #ALPHAIN}, {@link #SCALEIN}, {@link #SLIDEIN_BOTTOM},
     * {@link #SLIDEIN_LEFT}, {@link #SLIDEIN_RIGHT}.
     */
    public void openLoadAnimation(@AnimationType int animationType) {
        this.mOpenAnimationEnable = true;
        mCustomAnimation = null;
        switch (animationType) {
            case ALPHAIN:
                mSelectAnimation = new AlphaInAnimation();
                break;
            case SCALEIN:
                mSelectAnimation = new ScaleInAnimation();
                break;
            case SLIDEIN_BOTTOM:
                mSelectAnimation = new SlideInBottomAnimation();
                break;
            case SLIDEIN_LEFT:
                mSelectAnimation = new SlideInLeftAnimation();
                break;
            case SLIDEIN_RIGHT:
                mSelectAnimation = new SlideInRightAnimation();
                break;
            default:
                break;
        }
    }

    /**
     * Set Custom ObjectAnimator
     *
     * @param animation ObjectAnimator
     */
    public void openLoadAnimation(BaseAnimation animation) {
        this.mOpenAnimationEnable = true;
        this.mCustomAnimation = animation;
    }

    /**
     * To open the animation when loading
     */
    public void openLoadAnimation() {
        this.mOpenAnimationEnable = true;
    }

    /**
     * To close the animation when loading
     */
    public void closeLoadAnimation() {
        this.mOpenAnimationEnable = false;
    }

    /**
     * {@link #addAnimation(RecyclerView.ViewHolder)}
     *
     * @param firstOnly true just show anim when first loading false show anim when load the data every time
     */
    public void isFirstOnly(boolean firstOnly) {
        this.mFirstOnlyEnable = firstOnly;
    }

    /**
     * Implement this method and use the helper to adapt the view to the given item.
     *
     * @param helper A fully initialized helper.
     * @param item The item that needs to be displayed.
     */
    protected abstract void convert(@NonNull K helper,@NonNull T item);

    /**
     * Optional implementation this method and use the helper to adapt the view to the given item.
     *
     * If {@link DiffUtil.Callback#getChangePayload(int,int)} is implemented,
     * then {@link BaseQuickAdapter#convert(BaseViewHolder,Object)} will not execute, and will
     * perform this method, Please implement this method for partial refresh.
     *
     * If use {@link RecyclerView.Adapter#notifyItemChanged(int,Object)} with payload,
     * Will execute this method.
     *
     * @param helper A fully initialized helper.
     * @param item The item that needs to be displayed.
     * @param payloads payload info.
     */
    protected void convertPayloads(@NonNull K helper,T item,@NonNull List<Object> payloads) {
    }

    /**
     * get the specific view by position,e.g. getViewByPosition(2, R.id.textView)
     * <p>
     * bind recyclerView {@link #bindToRecyclerView(RecyclerView)} before use!
     *
     * @see #bindToRecyclerView(RecyclerView)
     */
    @Nullable
    public View getViewByPosition(int position,@IdRes int viewId) {
        checkNotNull();
        return getViewByPosition(getRecyclerView(),position,viewId);
    }

    @Nullable
    public View getViewByPosition(RecyclerView recyclerView,int position,@IdRes int viewId) {
        if (recyclerView == null) {
            return null;
        }
        BaseViewHolder viewHolder = (BaseViewHolder)recyclerView.findViewHolderForLayoutPosition(position);
        if (viewHolder == null) {
            return null;
        }
        return viewHolder.getView(viewId);
    }

    /**
     * Get the row id associated with the specified position in the list.
     *
     * @param position The position of the item within the adapter's data set whose row id we want.
     * @return The id of the item at the specified position.
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressWarnings("unchecked")
    private int recursiveExpand(int position,@NonNull List list) {
        int count = list.size();
        int pos = position + list.size() - 1;
        for (int i = list.size() - 1; i >= 0; i--,pos--) {
            if (list.get(i) instanceof IExpandable) {
                IExpandable item = (IExpandable)list.get(i);
                if (item.isExpanded() && hasSubItems(item)) {
                    List subList = item.getSubItems();
                    mData.addAll(pos + 1,subList);
                    int subItemCount = recursiveExpand(pos + 1,subList);
                    count += subItemCount;
                }
            }
        }
        return count;
    }

    /**
     * Expand an expandable item
     *
     * @param position position of the item
     * @param animate expand items with animation
     * @param shouldNotify notify the RecyclerView to rebind items, <strong>false</strong> if you want to do it
     * yourself.
     * @return the number of items that have been added.
     */
    @SuppressWarnings("unchecked")
    public int expand(@IntRange(from = 0) int position,boolean animate,boolean shouldNotify) {
        position -= getHeaderLayoutCount();

        IExpandable expandable = getExpandableItem(position);
        if (expandable == null) {
            return 0;
        }
        if (!hasSubItems(expandable)) {
            expandable.setExpanded(true);
            notifyItemChanged(position);
            return 0;
        }
        int subItemCount = 0;
        if (!expandable.isExpanded()) {
            List list = expandable.getSubItems();
            mData.addAll(position + 1,list);
            subItemCount += recursiveExpand(position + 1,list);

            expandable.setExpanded(true);
            //            subItemCount += list.size();
        }
        int parentPos = position + getHeaderLayoutCount();
        if (shouldNotify) {
            if (animate) {
                notifyItemChanged(parentPos);
                notifyItemRangeInserted(parentPos + 1,subItemCount);
            } else {
                notifyDataSetChanged();
            }
        }
        return subItemCount;
    }

    /**
     * Expand an expandable item
     *
     * @param position position of the item, which includes the header layout count.
     * @param animate expand items with animation
     * @return the number of items that have been added.
     */
    public int expand(@IntRange(from = 0) int position,boolean animate) {
        return expand(position,animate,true);
    }

    /**
     * Expand an expandable item with animation.
     *
     * @param position position of the item, which includes the header layout count.
     * @return the number of items that have been added.
     */
    public int expand(@IntRange(from = 0) int position) {
        return expand(position,true,true);
    }

    public int expandAll(int position,boolean animate,boolean notify) {
        position -= getHeaderLayoutCount();

        T endItem = null;
        if (position + 1 < this.getAdapterCount()) {
            endItem = getItem(position + 1);
        }

        IExpandable expandable = getExpandableItem(position);
        if (expandable == null) {
            return 0;
        }

        if (!hasSubItems(expandable)) {
            expandable.setExpanded(true);
            notifyItemChanged(position);
            return 0;
        }

        int count = expand(position + getHeaderLayoutCount(),false,false);
        for (int i = position + 1; i < this.getAdapterCount(); i++) {
            T item = getItem(i);

            if (item != null && item.equals(endItem)) {
                break;
            }
            if (isExpandable(item)) {
                count += expand(i + getHeaderLayoutCount(),false,false);
            }
        }

        if (notify) {
            if (animate) {
                notifyItemRangeInserted(position + getHeaderLayoutCount() + 1,count);
            } else {
                notifyDataSetChanged();
            }
        }
        return count;
    }

    /**
     * expand the item and all its subItems
     *
     * @param position position of the item, which includes the header layout count.
     * @param init whether you are initializing the recyclerView or not.
     * if <strong>true</strong>, it won't notify recyclerView to redraw UI.
     * @return the number of items that have been added to the adapter.
     */
    public int expandAll(int position,boolean init) {
        return expandAll(position,true,!init);
    }

    public void expandAll() {

        for (int i = getAdapterCount() - 1 + getHeaderLayoutCount(); i >= getHeaderLayoutCount(); i--) {
            expandAll(i,false,false);
        }
    }

    @SuppressWarnings("unchecked")
    private int recursiveCollapse(@IntRange(from = 0) int position) {
        T item = getItem(position);
        if (item == null || !isExpandable(item)) {
            return 0;
        }
        IExpandable expandable = (IExpandable)item;
        if (!expandable.isExpanded()) {
            return 0;
        }
        List<T> collapseList = new ArrayList<>();
        int itemLevel = expandable.getLevel();
        T itemTemp;
        for (int i = position + 1, n = getAdapterCount(); i < n; i++) {
            itemTemp = mData.get(i);
            if (itemTemp instanceof IExpandable && ((IExpandable)itemTemp).getLevel() == itemLevel) {
                break;
            }
            collapseList.add(itemTemp);
        }
        mData.removeAll(collapseList);
        return collapseList.size();
    }

    /**
     * Collapse an expandable item that has been expanded..
     *
     * @param position the position of the item, which includes the header layout count.
     * @param animate collapse with animation or not.
     * @param notify notify the recyclerView refresh UI or not.
     * @return the number of subItems collapsed.
     */
    public int collapse(@IntRange(from = 0) int position,boolean animate,boolean notify) {
        position -= getHeaderLayoutCount();

        IExpandable expandable = getExpandableItem(position);
        if (expandable == null) {
            return 0;
        }
        int subItemCount = recursiveCollapse(position);
        expandable.setExpanded(false);
        int parentPos = position + getHeaderLayoutCount();
        if (notify) {
            if (animate) {
                notifyItemChanged(parentPos);
                notifyItemRangeRemoved(parentPos + 1,subItemCount);
            } else {
                notifyDataSetChanged();
            }
        }
        return subItemCount;
    }

    /**
     * Collapse an expandable item that has been expanded..
     *
     * @param position the position of the item, which includes the header layout count.
     * @return the number of subItems collapsed.
     */
    public int collapse(@IntRange(from = 0) int position) {
        return collapse(position,true,true);
    }

    /**
     * Collapse an expandable item that has been expanded..
     *
     * @param position the position of the item, which includes the header layout count.
     * @return the number of subItems collapsed.
     */
    public int collapse(@IntRange(from = 0) int position,boolean animate) {
        return collapse(position,animate,true);
    }

    private int getItemPosition(T item) {
        return item != null && mData != null && !mData.isEmpty() ? mData.indexOf(item) : -1;
    }

    public boolean hasSubItems(IExpandable item) {
        if (item == null) {
            return false;
        }
        List list = item.getSubItems();
        return list != null && list.size() > 0;
    }

    public boolean isExpandable(T item) {
        return item != null && item instanceof IExpandable;
    }

    private IExpandable getExpandableItem(int position) {
        T item = getItem(position);
        if (isExpandable(item)) {
            return (IExpandable)item;
        } else {
            return null;
        }
    }

    /**
     * Get the parent item position of the IExpandable item
     *
     * @return return the closest parent item position of the IExpandable.
     * if the IExpandable item's level is 0, return itself position.
     * if the item's level is negative which mean do not implement this, return a negative
     * if the item is not exist in the data list, return a negative.
     */
    public int getParentPosition(@NonNull T item) {
        int position = getItemPosition(item);
        if (position == -1) {
            return -1;
        }

        // if the item is IExpandable, return a closest IExpandable item position whose level smaller than this.
        // if it is not, return the closest IExpandable item position whose level is not negative
        int level;
        if (item instanceof IExpandable) {
            level = ((IExpandable)item).getLevel();
        } else {
            level = Integer.MAX_VALUE;
        }
        if (level == 0) {
            return position;
        } else if (level == -1) {
            return -1;
        }

        for (int i = position; i >= 0; i--) {
            T temp = mData.get(i);
            if (temp instanceof IExpandable) {
                IExpandable expandable = (IExpandable)temp;
                if (expandable.getLevel() >= 0 && expandable.getLevel() < level) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Interface definition for a callback to be invoked when an itemchild in this
     * view has been clicked
     */
    @FunctionalInterface
    public interface OnItemChildClickListener<T> {
        /**
         * callback method to be invoked when an itemchild in this view has been click
         *
         * @param view The view whihin the ItemView that was clicked
         * @param position The position of the view int the adapter
         */
        void onItemChildClick(T item,View view,int position);
    }

    /**
     * Interface definition for a callback to be invoked when an childView in this
     * view has been clicked and held.
     */
    @FunctionalInterface
    public interface OnItemChildLongClickListener<T> {
        /**
         * callback method to be invoked when an item in this view has been
         * click and held
         *
         * @param view The childView whihin the itemView that was clicked and held.
         * @param position The position of the view int the adapter
         * @return true if the callback consumed the long click ,false otherwise
         */
        boolean onItemChildLongClick(T item,View view,int position);
    }

    /**
     * Interface definition for a callback to be invoked when an item in this
     * view has been clicked and held.
     */
    @FunctionalInterface
    public interface OnItemLongClickListener<T> {
        /**
         * callback method to be invoked when an item in this view has been
         * click and held
         *
         * @param view The view whihin the RecyclerView that was clicked and held.
         * @param position The position of the view int the adapter
         * @return true if the callback consumed the long click ,false otherwise
         */
        boolean onItemLongClick(T item,View view,int position);
    }

    /**
     * Interface definition for a callback to be invoked when an item in this
     * RecyclerView itemView has been clicked.
     */
    @FunctionalInterface
    public interface OnItemClickListener<T> {

        /**
         * Callback method to be invoked when an item in this RecyclerView has
         * been clicked.
         *
         * @param view The itemView within the RecyclerView that was clicked (this
         * will be a view provided by the adapter)
         * @param position The position of the view in the adapter.
         */
        void onItemClick(T item,View view,int position);
    }

    /**
     * Register a callback to be invoked when an item in this RecyclerView has
     * been clicked.
     *
     * @param listener The callback that will be invoked.
     */
    public void setOnItemClickListener(@Nullable OnItemClickListener<T> listener) {
        mOnItemClickListener = listener;
    }

    /**
     * Register a callback to be invoked when an itemchild in View has
     * been  clicked
     *
     * @param listener The callback that will run
     */
    public void setOnItemChildClickListener(OnItemChildClickListener<T> listener) {
        mOnItemChildClickListener = listener;
    }

    /**
     * Register a callback to be invoked when an item in this RecyclerView has
     * been long clicked and held
     *
     * @param listener The callback that will run
     */
    public void setOnItemLongClickListener(OnItemLongClickListener<T> listener) {
        mOnItemLongClickListener = listener;
    }

    /**
     * Register a callback to be invoked when an itemchild  in this View has
     * been long clicked and held
     *
     * @param listener The callback that will run
     */
    public void setOnItemChildLongClickListener(OnItemChildLongClickListener<T> listener) {
        mOnItemChildLongClickListener = listener;
    }

    /**
     * @return The callback to be invoked with an item in this RecyclerView has
     * been long clicked and held, or null id no callback as been set.
     */
    public final OnItemLongClickListener<T> getOnItemLongClickListener() {
        return mOnItemLongClickListener;
    }

    /**
     * @return The callback to be invoked with an item in this RecyclerView has
     * been clicked and held, or null id no callback as been set.
     */
    public final OnItemClickListener<T> getOnItemClickListener() {
        return mOnItemClickListener;
    }

    /**
     * @return The callback to be invoked with an itemchild in this RecyclerView has
     * been clicked, or null id no callback has been set.
     */
    @Nullable
    public final OnItemChildClickListener<T> getOnItemChildClickListener() {
        return mOnItemChildClickListener;
    }

    /**
     * @return The callback to be invoked with an itemChild in this RecyclerView has
     * been long clicked, or null id no callback has been set.
     */
    @Nullable
    public final OnItemChildLongClickListener<T> getOnItemChildLongClickListener() {
        return mOnItemChildLongClickListener;
    }
}
