package com.ryg.slideview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static com.nineoldandroids.view.ViewPropertyAnimator.animate;
import com.ryg.slideview.SlideView.OnSlideListener;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import com.nineoldandroids.animation.Animator;
import com.nineoldandroids.animation.AnimatorListenerAdapter;
import com.nineoldandroids.animation.ValueAnimator;
import com.nineoldandroids.view.ViewHelper;

public class MainActivity extends Activity implements OnSlideListener {

    private ListViewCompat mListView;
    private List<MessageItem> mMessageItems = new ArrayList<MainActivity.MessageItem>();
    private SlideView mLastSlideViewWithStatusOn;
    private SlideAdapter adapter;
    
    private List<PendingDismissData> mPendingDismisses = new ArrayList<PendingDismissData>();
	private int mDismissAnimationRefCount = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mListView = (ListViewCompat) findViewById(R.id.list);
        for (int i = 0; i < 20; i++) {
        	MessageItem item = new MessageItem();
        	item.msg = "This is "+i;
        	mMessageItems.add(item);
        }
        adapter = new SlideAdapter();
        mListView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private class SlideAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mMessageItems.size();
        }
        @Override
        public Object getItem(int position) {
            return mMessageItems.get(position);
        }
        @Override
        public long getItemId(int position) {
            return position;
        }
        @Override
        public View getView(final int position,View convertView, ViewGroup parent) {
            ViewHolder holder;
            SlideView slideView = (SlideView) convertView;
            if (slideView == null) {
                View itemView = LayoutInflater.from(MainActivity.this).inflate(R.layout.list_item, null);
                slideView = new SlideView(MainActivity.this);
                slideView.setContentView(itemView);
                holder = new ViewHolder(slideView);
                slideView.setOnSlideListener(MainActivity.this);
                slideView.setTag(holder);
            } else {
                holder = (ViewHolder) slideView.getTag();
            }
            final MessageItem item = mMessageItems.get(position);
            item.slideView = slideView;
            item.slideView.shrink();
            holder.msg.setText(item.msg);
            holder.deleteHolder.setOnClickListener(new OnClickListener() {
            	@Override
            	public void onClick(View v) {
            		// TODO Auto-generated method stub
            		final View downView = item.slideView; 
					final int downPosition = position;
					++mDismissAnimationRefCount;
					animate(item.slideView).translationX(-800).setDuration(300).setListener(new AnimatorListenerAdapter() {
						@Override
						public void onAnimationEnd(Animator animation) {
							item.slideView.shrink();
							performDismiss(downView, downPosition);
						}
					});
            	}
            });
            holder.cancleHolder.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					item.slideView.shrink();
				}
			});
            return slideView;
        }
    }

    public class MessageItem {
        public String msg;
        public SlideView slideView;
    }

    private static class ViewHolder {
        public TextView msg;
        public ViewGroup deleteHolder;
        public ViewGroup cancleHolder;
        ViewHolder(View view) {
            msg = (TextView) view.findViewById(R.id.text);
            deleteHolder = (ViewGroup)view.findViewById(R.id.holderDelect);
            cancleHolder = (ViewGroup)view.findViewById(R.id.holderCancle);
        }
    }

    @Override
    public void onSlide(View view, int status) {
        if (mLastSlideViewWithStatusOn != null && mLastSlideViewWithStatusOn != view) {
            mLastSlideViewWithStatusOn.shrink();
        }
        if (status == SLIDE_STATUS_ON) {
            mLastSlideViewWithStatusOn = (SlideView) view;
        }
    }
    
    class PendingDismissData implements Comparable<PendingDismissData> {
		public int position;
		public View view;
		public PendingDismissData(int position, View view) {
			this.position = position;
			this.view = view;
		}
		@Override
		public int compareTo(PendingDismissData other) {
			// Sort by descending position
			return other.position - position;
		}
	}
    public void performDismiss(final View dismissView, final int dismissPosition) {
		final ViewGroup.LayoutParams lp = dismissView.getLayoutParams();//获取item的布局参数
		final int originalHeight = dismissView.getHeight();//item的高度
		ValueAnimator animator = ValueAnimator.ofInt(originalHeight, 0).setDuration(300);
		animator.start();
		animator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				//这段代码很重要，因为我们并没有将item从ListView中移除，而是将item的高度设置为0
				//所以我们在动画执行完毕之后将item设置回来
				--mDismissAnimationRefCount;
				if (mDismissAnimationRefCount == 0) {
					Collections.sort(mPendingDismisses);
					int[] dismissPositions = new int[mPendingDismisses.size()];
					for (int i = mPendingDismisses.size() - 1; i >= 0; i--) {
						dismissPositions[i] = mPendingDismisses.get(i).position;
					}
					
					mMessageItems.remove(dismissPosition);
					adapter.notifyDataSetChanged();
					
					ViewGroup.LayoutParams lp;
					for (PendingDismissData pendingDismiss : mPendingDismisses) {
						// Reset view presentation
						ViewHelper.setAlpha(pendingDismiss.view, 1f);
						ViewHelper.setTranslationX(pendingDismiss.view, 0);
						lp = pendingDismiss.view.getLayoutParams();
						lp.height = 0;
						pendingDismiss.view.setLayoutParams(lp);
					}
					mPendingDismisses.clear();
				}
				
			}
		});
		animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator valueAnimator) {
				//这段代码的效果是ListView删除某item之后，其他的item向上滑动的效果
				lp.height = (Integer) valueAnimator.getAnimatedValue();
				dismissView.setLayoutParams(lp);
			}
		});
		mPendingDismisses.add(new PendingDismissData(dismissPosition, dismissView));
		animator.start();
	}
}
