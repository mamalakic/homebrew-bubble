package com.nkanaev.comics.view;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AppCompatDialog;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.nkanaev.comics.R;
import com.nkanaev.comics.managers.IgnoreCaseComparator;
import com.nkanaev.comics.managers.Utils;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;


public class DirectorySelectDialog
        extends AppCompatDialog
        implements View.OnClickListener, AdapterView.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {
    private Button mSetButton;
    private Button mCancelButton;
    private ImageButton mRefreshButton;
    private OnDirectorySelectListener mListener;
    private ListView mListView;
    private TextView mTitleTextView;
    private File mRootDir = new File("/");

    private File[] mValidFolders = new File[0];
    private File mCurrentDir;
    private File[] mSubdirs;
    private FileFilter mDirectoryFilter;

    private SwipeRefreshLayout mRefreshLayout;

    public interface OnDirectorySelectListener {
        void onDirectorySelect(File file);
    }

    public DirectorySelectDialog(Context context, File[] validStorages) {
        super(context);
        if (validStorages != null) mValidFolders = validStorages;
        setContentView(R.layout.dialog_directorypicker);
        mSetButton = (Button) findViewById(R.id.directory_picker_confirm);
        mCancelButton = (Button) findViewById(R.id.directory_picker_cancel);
        mRefreshButton = findViewById(R.id.directory_picker_refresh);
        mListView = (ListView) findViewById(R.id.directory_listview);
        mTitleTextView = (TextView) findViewById(R.id.directory_current_text);

        mSetButton.setOnClickListener(this);
        mCancelButton.setOnClickListener(this);
        mRefreshButton.setOnClickListener(this);

        mListView.setAdapter(new DirectoryListAdapter());
        mListView.setOnItemClickListener(this);

        mDirectoryFilter = new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory();
            }
        };

        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.SwipeRefreshLayout);
        if (mRefreshLayout!=null) {
            mRefreshLayout.setColorSchemeResources(R.color.primary);
            mRefreshLayout.setOnRefreshListener(this);
            mRefreshLayout.setEnabled(true);
        }
    }

    public void setCurrentDirectory(File path) {
        mCurrentDir = path;

        File[] subs = mCurrentDir.listFiles(mDirectoryFilter);
        ArrayList<File> subDirs = null;
        if (subs != null) {
            subDirs = new ArrayList<>(Arrays.asList(subs));
        } else {
            subDirs = new ArrayList<>();
        }

        // ensure paths to storages are listed, even if not browsable
        if (Utils.isOreoOrLater()) {
            Path parent = mCurrentDir.toPath();
            for (File validPath : mValidFolders) {
                if (!validPath.toPath().startsWith(parent))
                    continue;

                Path relPath = parent.relativize(validPath.toPath());
                if (relPath.getNameCount() < 1)
                    continue;

                File entry = new File(mCurrentDir, relPath.getName(0).toString());
                if (!subDirs.contains(entry))
                    subDirs.add(entry);
            }
        }

        // sort alphabetically ignore-case
        Collections.sort(subDirs, IgnoreCaseComparator.forFunction(File::getName));

        // add '..' to top
        if (!mCurrentDir.getAbsolutePath().equals(mRootDir.getAbsolutePath())) {
            subDirs.add(0, mCurrentDir.getParentFile());
        }

        mSubdirs = subDirs.toArray(new File[subDirs.size()]);

        mTitleTextView.setText(mCurrentDir.getPath());
        ((BaseAdapter) mListView.getAdapter()).notifyDataSetChanged();
    }

    public void setOnDirectorySelectListener(OnDirectorySelectListener l) {
        mListener = l;
    }

    @Override
    public void show() {
        // refresh on show
        if (mCurrentDir!=null)
            setCurrentDirectory(mCurrentDir);
        super.show();
    }

    @Override
    public void onRefresh() {
        // refresh on show
        if (mCurrentDir!=null) {
            mRefreshLayout.setRefreshing(true);
            setCurrentDirectory(mCurrentDir);
        }
        mRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onClick(View v) {
        // refresh
        if (v == mRefreshButton) {
            onRefresh();
            return;
        }

        // ok, set current dir
        if (v == mSetButton) {
            if (mListener != null) {
                mListener.onDirectorySelect(mCurrentDir);
            }
        }
        // dismiss dialog on ok/cancel
        dismiss();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        File dir = mSubdirs[position];
        setCurrentDirectory(dir);
    }

    private class DirectoryListAdapter extends BaseAdapter {
        @Override
        public Object getItem(int position) {
            return mSubdirs[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public int getCount() {
            return (mSubdirs != null) ? mSubdirs.length : 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                        .inflate(R.layout.row_directory, parent, false);
            }

            File dir = mSubdirs[position];
            TextView textView = (TextView) convertView.findViewById(R.id.directory_row_text);

            if (position == 0 && !mRootDir.getPath().equals(mCurrentDir.getPath())) {
                textView.setText("..");
            } else {
                textView.setText(dir.getName());
            }

            setIcon(position, convertView, dir);

            return convertView;
        }

        private void setIcon(int position, View convertView, File file) {
            ImageView view = (ImageView) convertView.findViewById(R.id.directory_row_icon);
            ImageView circle = (ImageView) convertView.findViewById(R.id.directory_row_circle);

            // default bg color is grey
            int colorRes = R.color.circle_grey;
            GradientDrawable circleDrawable = (GradientDrawable) circle.getDrawable();
            circleDrawable.setColor(getContext().getResources().getColor(colorRes));
            circle.setVisibility(View.VISIBLE);
            view.setImageResource(R.drawable.ic_folder_24);

            // top '..' is always grey
            if (position==0)
                return;

            File[] listing = file.listFiles();
            if (listing != null)
                for (File listFile : listing) {
                    if (listFile.isFile() && Utils.isArchive(listFile.getName())) {
                        circle.setVisibility(View.INVISIBLE);
                        break;
                    }
                }

        }
    }
}
