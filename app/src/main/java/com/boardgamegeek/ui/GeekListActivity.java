package com.boardgamegeek.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.MenuItem;

import com.boardgamegeek.R;
import com.boardgamegeek.io.Adapter;
import com.boardgamegeek.io.BggService;
import com.boardgamegeek.model.GeekList;
import com.boardgamegeek.provider.BggContract;
import com.boardgamegeek.ui.loader.BggLoader;
import com.boardgamegeek.ui.loader.SafeResponse;
import com.boardgamegeek.ui.model.GeekListValue;
import com.boardgamegeek.util.ActivityUtils;
import com.boardgamegeek.util.DateTimeUtils;
import com.boardgamegeek.util.StringUtils;
import com.boardgamegeek.util.UIUtils;
import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.ContentViewEvent;

public class GeekListActivity extends TabActivity implements LoaderManager.LoaderCallbacks<SafeResponse<GeekList>> {
	private static final int LOADER_ID = 1;
	private int geekListId;
	private String geekListTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Intent intent = getIntent();
		geekListId = intent.getIntExtra(ActivityUtils.KEY_ID, BggContract.INVALID_ID);
		geekListTitle = intent.getStringExtra(ActivityUtils.KEY_TITLE);
		safelySetTitle(geekListTitle);

		if (savedInstanceState == null) {
			Answers.getInstance().logContentView(new ContentViewEvent()
				.putContentType("GeekList")
				.putContentId(String.valueOf(geekListId))
				.putContentName(geekListTitle));
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		getSupportLoaderManager().initLoader(LOADER_ID, null, this);
	}

	@Override
	protected int getOptionsMenuId() {
		return R.menu.view_share;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_view:
				ActivityUtils.linkToBgg(this, "geeklist", geekListId);
				return true;
			case R.id.menu_share:
				ActivityUtils.shareGeekList(this, geekListId, geekListTitle);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void setUpViewPager() {
		GeekListPagerAdapter adapter = new GeekListPagerAdapter(getSupportFragmentManager());
		viewPager.setAdapter(adapter);
	}

	private final class GeekListPagerAdapter extends FragmentPagerAdapter {
		private Fragment headerFragment;
		private Fragment itemsFragment;

		public GeekListPagerAdapter(FragmentManager fragmentManager) {
			super(fragmentManager);
		}

		@Override
		public CharSequence getPageTitle(int position) {
			if (position == 0) return getString(R.string.title_description);
			if (position == 1) return getString(R.string.title_items);
			return "";
		}

		@Override
		public Fragment getItem(int position) {
			if (position == 0) {
				headerFragment = Fragment.instantiate(
					GeekListActivity.this,
					GeekListDescriptionFragment.class.getName(),
					UIUtils.intentToFragmentArguments(getIntent()));
				return headerFragment;
			}
			if (position == 1) {
				itemsFragment = Fragment.instantiate(
					GeekListActivity.this,
					GeekListItemsFragment.class.getName(),
					UIUtils.intentToFragmentArguments(getIntent()));
				return itemsFragment;
			}
			return null;
		}

		@Override
		public int getCount() {
			return 2;
		}

		@Nullable
		public Fragment getHeaderFragment() {
			return headerFragment;
		}

		@Nullable
		public Fragment getItemsFragment() {
			return itemsFragment;
		}
	}

	@Override
	public Loader<SafeResponse<GeekList>> onCreateLoader(int id, Bundle data) {
		return new GeekListLoader(this, geekListId);
	}

	@Override
	public void onLoadFinished(Loader<SafeResponse<GeekList>> loader, SafeResponse<GeekList> data) {
		if (viewPager == null) return;
		GeekListPagerAdapter adapter = (GeekListPagerAdapter) viewPager.getAdapter();
		if (adapter == null) return;

		GeekList body = data.getBody();
		GeekListValue glv = GeekListValue.builder()
			.setId(body.id)
			.setTitle(TextUtils.isEmpty(body.title) ? "" : body.title.trim())
			.setUsername(body.username)
			.setDescription(body.description)
			.setNumberOfItems(StringUtils.parseInt(body.numitems))
			.setNumberOfThumbs(StringUtils.parseInt(body.thumbs))
			.setPostTicks(DateTimeUtils.tryParseDate(DateTimeUtils.UNPARSED_DATE, body.postdate, GeekList.FORMAT))
			.setEditTicks(DateTimeUtils.tryParseDate(DateTimeUtils.UNPARSED_DATE, body.editdate, GeekList.FORMAT))
			.build();

		GeekListDescriptionFragment descriptionFragment = ((GeekListDescriptionFragment) adapter.getHeaderFragment());
		if (descriptionFragment != null) descriptionFragment.setData(glv);

		GeekListItemsFragment itemsFragment = ((GeekListItemsFragment) adapter.getItemsFragment());
		if (itemsFragment != null) {
			if (data.hasParseError()) {
				itemsFragment.setError(R.string.parse_error);
			} else if (data.hasError()) {
				itemsFragment.setError(data.getErrorMessage());
			} else if (glv.numberOfItems() == 0 || body.getItems().size() == 0) {
				itemsFragment.setError();
			} else {
				itemsFragment.setData(glv, body.getItems());
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<SafeResponse<GeekList>> loader) {
	}

	private static class GeekListLoader extends BggLoader<SafeResponse<GeekList>> {
		private final BggService service;
		private final int geekListId;

		public GeekListLoader(Context context, int geekListId) {
			super(context);
			service = Adapter.createForXml();
			this.geekListId = geekListId;
		}

		@Override
		public SafeResponse<GeekList> loadInBackground() {
			return new SafeResponse<>(service.geekList(geekListId, 1));
		}
	}
}
