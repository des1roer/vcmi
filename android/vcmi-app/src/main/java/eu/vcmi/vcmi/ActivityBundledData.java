package eu.vcmi.vcmi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import eu.vcmi.vcmi.util.Log;

/**
 * Entry activity. Copies Heroes 3 data bundled into the APK assets (see bundledGameDataDir in build.gradle)
 * into the VCMI data directory, then opens the launcher. Without bundled data it opens the launcher immediately.
 */
public class ActivityBundledData extends Activity
{
	private static final String MARKER_FILE = ".bundled-data-installed";
	private static final int BUFFER_SIZE = 1024 * 1024;

	private static class AssetEntry
	{
		final String assetPath;
		final File target;
		final long length; ///< -1 if the asset is stored compressed and its length is unknown

		AssetEntry(final String assetPath, final File target, final long length)
		{
			this.assetPath = assetPath;
			this.target = target;
			this.length = length;
		}
	}

	private ProgressBar progressBar;

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		final String installStamp = installStamp();
		if (!hasBundledData() || installStamp.equals(readMarker()))
		{
			openLauncher();
			return;
		}

		showProgressView();
		new Thread(() -> {
			boolean success;
			try
			{
				extract();
				writeMarker(installStamp);
				success = true;
			}
			catch (final IOException e)
			{
				Log.e(this, "Failed to extract bundled game data", e);
				success = false;
			}

			final boolean extracted = success;
			runOnUiThread(() -> {
				if (extracted)
					openLauncher();
				else
					showError();
			});
		}, "BundledDataExtractor").start();
	}

	private boolean hasBundledData()
	{
		try
		{
			final String[] children = getAssets().list(Const.VCMI_DATA_ROOT_FOLDER_NAME);
			return children != null && children.length > 0;
		}
		catch (final IOException e)
		{
			return false;
		}
	}

	/** Changes with every APK install or update, so updated bundled data is extracted again */
	private String installStamp()
	{
		try
		{
			return Long.toString(getPackageManager().getPackageInfo(getPackageName(), 0).lastUpdateTime);
		}
		catch (final PackageManager.NameNotFoundException e)
		{
			throw new IllegalStateException("Own package not found", e);
		}
	}

	private File markerFile()
	{
		return new File(Storage.getVcmiDataDir(this), MARKER_FILE);
	}

	private String readMarker()
	{
		final File marker = markerFile();
		if (!marker.isFile())
			return "";

		try (InputStream in = new FileInputStream(marker))
		{
			final byte[] content = new byte[(int) marker.length()];
			final int read = in.read(content);
			return new String(content, 0, Math.max(read, 0), StandardCharsets.UTF_8);
		}
		catch (final IOException e)
		{
			Log.w(this, "Failed to read bundled data marker: " + e);
			return "";
		}
	}

	private void writeMarker(final String stamp) throws IOException
	{
		try (OutputStream out = new FileOutputStream(markerFile(), false))
		{
			out.write(stamp.getBytes(StandardCharsets.UTF_8));
		}
	}

	private void extract() throws IOException
	{
		final List<AssetEntry> entries = new ArrayList<>();
		collectEntries(getAssets(), Const.VCMI_DATA_ROOT_FOLDER_NAME, Storage.getVcmiDataDir(this), entries);

		long totalBytes = 0;
		for (final AssetEntry entry : entries)
			totalBytes += Math.max(entry.length, 0);

		final long progressTotal = totalBytes;
		runOnUiThread(() -> progressBar.setMax(1000));

		long copiedBytes = 0;
		final byte[] buffer = new byte[BUFFER_SIZE];
		for (final AssetEntry entry : entries)
		{
			// a previous extraction may have been interrupted, so only files with matching size are skipped
			if (entry.length >= 0 && entry.target.isFile() && entry.target.length() == entry.length)
			{
				copiedBytes += entry.length;
				continue;
			}

			final File parent = entry.target.getParentFile();
			if (parent != null && !parent.isDirectory() && !parent.mkdirs())
				throw new IOException("Cannot create directory " + parent);

			try (InputStream in = getAssets().open(entry.assetPath, AssetManager.ACCESS_STREAMING);
				 FileOutputStream out = new FileOutputStream(entry.target, false))
			{
				int read;
				while ((read = in.read(buffer)) != -1)
				{
					out.write(buffer, 0, read);
					copiedBytes += read;
					updateProgress(copiedBytes, progressTotal);
				}
				out.getFD().sync();
			}
		}
	}

	private static void collectEntries(final AssetManager assets, final String assetPath, final File target, final List<AssetEntry> entries) throws IOException
	{
		final String[] children = assets.list(assetPath);

		// AssetManager reports files as directories without children
		if (children == null || children.length == 0)
		{
			entries.add(new AssetEntry(assetPath, target, assetLength(assets, assetPath)));
			return;
		}

		for (final String child : children)
			collectEntries(assets, assetPath + "/" + child, new File(target, child), entries);
	}

	private static long assetLength(final AssetManager assets, final String assetPath) throws IOException
	{
		try (AssetFileDescriptor fd = assets.openFd(assetPath))
		{
			return fd.getLength();
		}
		catch (final FileNotFoundException e)
		{
			// openFd fails for compressed assets
			return -1;
		}
	}

	private void updateProgress(final long copied, final long total)
	{
		if (total <= 0)
			return;

		final int value = (int) Math.min(1000, copied * 1000 / total);
		if (value != progressBar.getProgress())
			runOnUiThread(() -> progressBar.setProgress(value));
	}

	private void showProgressView()
	{
		final int padding = (int) (32 * getResources().getDisplayMetrics().density);

		final LinearLayout layout = new LinearLayout(this);
		layout.setOrientation(LinearLayout.VERTICAL);
		layout.setGravity(Gravity.CENTER);
		layout.setPadding(padding, padding, padding, padding);

		final TextView text = new TextView(this);
		text.setText(R.string.bundled_data_extracting);
		text.setGravity(Gravity.CENTER);
		layout.addView(text);

		progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
		layout.addView(progressBar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

		setContentView(layout);
	}

	private void showError()
	{
		new AlertDialog.Builder(this)
			.setTitle(R.string.bundled_data_error_title)
			.setMessage(R.string.bundled_data_error_message)
			.setCancelable(false)
			.setPositiveButton(android.R.string.ok, (dialog, which) -> openLauncher())
			.show();
	}

	private void openLauncher()
	{
		final Intent intent = new Intent(this, ActivityLauncher.class);
		startActivity(intent);
		finish();
	}
}
