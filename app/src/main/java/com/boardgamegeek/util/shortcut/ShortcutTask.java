package com.boardgamegeek.util.shortcut;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.boardgamegeek.R;
import com.boardgamegeek.util.HttpUtils;
import com.boardgamegeek.util.ShortcutUtils;
import com.squareup.picasso.Picasso;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import timber.log.Timber;

public abstract class ShortcutTask extends AsyncTask<Void, Void, Void> {
	protected final Context context;
	private final String thumbnailUrl;

	public ShortcutTask(Context context) {
		this(context, null);
	}

	public ShortcutTask(Context context, String thumbnailUrl) {
		this.context = context.getApplicationContext();
		this.thumbnailUrl = HttpUtils.ensureScheme(thumbnailUrl);
	}

	protected abstract Intent createIntent();

	@Override
	protected Void doInBackground(Void... params) {
		Intent shortcutIntent = createIntent();
		if (!TextUtils.isEmpty(thumbnailUrl)) {
			Bitmap bitmap = fetchThumbnail();
			if (bitmap != null) {
				shortcutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON, bitmap);
			}
		}

		context.sendBroadcast(shortcutIntent);
		return null;
	}

	@Override
	protected void onPostExecute(Void nothing) {
	}

	private Bitmap fetchThumbnail() {
		Bitmap bitmap = null;
		File file = ShortcutUtils.getThumbnailFile(context, thumbnailUrl);
		if (file != null) {
			if (file.exists()) {
				bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
			} else {
				try {
					bitmap = Picasso.with(context)
						.load(thumbnailUrl)
						.resizeDimen(R.dimen.shortcut_icon_size, R.dimen.shortcut_icon_size)
						.centerCrop().get();
				} catch (IOException e) {
					Timber.e(e, "Error downloading the thumbnail.");
				}
				if (bitmap != null) {
					try {
						OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
						bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
						out.close();
					} catch (IOException e) {
						Timber.e(e, "Error saving the thumbnail file.");
					}
				}
			}
		}
		return bitmap;
	}
}
