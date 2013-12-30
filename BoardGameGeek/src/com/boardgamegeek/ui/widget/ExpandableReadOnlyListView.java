package com.boardgamegeek.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.boardgamegeek.R;

public class ExpandableReadOnlyListView extends RelativeLayout {

	private TextView mLabelView;
	private TextView mSummaryView;
	private ImageView mToggleView;
	private TextView mDetailView;
	private boolean mExpanded;
	private String mOneMore;
	private String mSomeMore;
	private int mNameColumnIndex;
	private int mCount;

	public ExpandableReadOnlyListView(Context context) {
		super(context);
		init(context, null);
	}

	public ExpandableReadOnlyListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	public ExpandableReadOnlyListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context, attrs);
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable p = super.onSaveInstanceState();
		SavedState state = new SavedState(p);
		state.expanded = mExpanded;
		return p;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		if (!(state instanceof SavedState)) {
			super.onRestoreInstanceState(state);
			return;
		}

		SavedState saved = (SavedState) state;
		super.onRestoreInstanceState(saved.getSuperState());

		mExpanded = saved.expanded;
		expandOrCollapse();
	}

	private void init(Context context, AttributeSet attrs) {
		mOneMore = context.getString(R.string.one_more);
		mSomeMore = context.getString(R.string.some_more);

		LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.widget_expandable_readonly_list, this, true);
		mLabelView = (TextView) findViewById(R.id.label);
		mSummaryView = (TextView) findViewById(R.id.summary);
		mToggleView = (ImageView) findViewById(R.id.toggle);
		mDetailView = (TextView) findViewById(R.id.detail);

		mExpanded = false;
		expandOrCollapse();
		setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mExpanded = !mExpanded;
				expandOrCollapse();
			}
		});

		if (attrs != null) {
			TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ExpandableReadOnlyListView);
			try {
				mLabelView.setText(a.getString(R.styleable.ExpandableReadOnlyListView_xlabel));
			} finally {
				a.recycle();
			}
		}
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		updateSummary();
	}

	public void setLabel(CharSequence label) {
		mLabelView.setText(label);
	}

	public void clear() {
		mSummaryView.setText("");
		mDetailView.setText("");
	}

	public void bind(Cursor cursor, int nameColumnIndex) {
		mNameColumnIndex = nameColumnIndex;
		updateData(cursor);
	}

	private void updateData(Cursor cursor) {
		mCount = cursor.getCount();
		String names = joinNames(cursor);
		mDetailView.setText(names);
		updateSummary();
	}

	private void updateSummary() {
		CharSequence summary = null;
		final CharSequence text = mDetailView.getText();
		if (!"".equals(text)) {
			TextPaint paint = new TextPaint();
			paint.setTextSize(mSummaryView.getTextSize());
			summary = TextUtils.commaEllipsize(text, paint, mSummaryView.getWidth() - mSummaryView.getPaddingLeft()
				- mSummaryView.getPaddingRight(), mOneMore, mSomeMore);
			if (TextUtils.isEmpty(summary)) {
				summary = String.format(mSomeMore, mCount);
			}
		}
		mSummaryView.setText(summary);
	}

	private void expandOrCollapse() {
		mSummaryView.setVisibility(mExpanded ? GONE : VISIBLE);
		mDetailView.setVisibility(mExpanded ? VISIBLE : GONE);
		mToggleView.setImageResource(mExpanded ? R.drawable.expander_close : R.drawable.expander_open);
		requestLayout();
	}

	private String joinNames(Cursor cursor) {
		StringBuilder sb = new StringBuilder();
		if (cursor != null && cursor.moveToFirst()) {
			boolean firstTime = true;
			do {
				if (firstTime) {
					firstTime = false;
				} else {
					sb.append(", ");
				}
				sb.append(cursor.getString(mNameColumnIndex));
			} while (cursor.moveToNext());
		}
		return sb.toString();
	}

	private static class SavedState extends BaseSavedState {
		public boolean expanded;

		SavedState(Parcelable superState) {
			super(superState);
		}

		private SavedState(Parcel in) {
			super(in);
			expanded = in.readInt() != 0;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(expanded ? 1 : 0);
		}

		@SuppressWarnings("unused")
		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in);
			}

			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}
}