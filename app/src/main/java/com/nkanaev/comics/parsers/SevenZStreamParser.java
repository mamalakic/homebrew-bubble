package com.nkanaev.comics.parsers;


import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.util.Log;
import com.nkanaev.comics.MainApplication;
import com.nkanaev.comics.managers.IgnoreCaseComparator;
import com.nkanaev.comics.managers.Utils;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.util.*;

public class SevenZStreamParser extends AbstractParser {
    private List<SevenZArchiveEntry> mEntries = null;
    private File mCacheDir = null;
    private boolean mCachePopulated = false;

    public SevenZStreamParser() {
        super(new Class[]{File.class});
    }

    @Override
    public void parse() throws IOException {
        Object source = getSource();
        if (source instanceof File)
            parse((File) source);
        else
            throw new UnsupportedOperationException();
    }

    protected void parse(File file) throws IOException {
        SevenZFile sevenZFile = new SevenZFile(file);
        parse(sevenZFile);
    }

    protected void parse(SevenZFile sevenZFile) throws IOException {
        if (sevenZFile != null && mEntries != null)
            return;

        sevenZFile = sevenZFile;
        mEntries = new ArrayList<>();

        for (SevenZArchiveEntry entry : sevenZFile.getEntries()) {
            if (entry.isDirectory()) {
                continue;
            }
            if (Utils.isImage(entry.getName())) {
                mEntries.add(entry);
            }
        }

        Collections.sort(mEntries, IgnoreCaseComparator.forFunction(SevenZArchiveEntry::getName));
    }

    @Override
    public int numPages() throws IOException {
        parse();
        return mEntries.size();
    }

    private SevenZFile from(Uri uri) throws IOException {
        AssetFileDescriptor afd = MainApplication.getAppContext().getContentResolver().openAssetFileDescriptor(uri, "r");
        FileChannel fc = new FileInputStream(afd.getFileDescriptor()).getChannel();
        return from(fc);
    }

    private SevenZFile from(SeekableByteChannel channel) throws IOException {
        SevenZFile sevenZFile = new SevenZFile(channel);
        return sevenZFile;
    }

    private SevenZFile from(File file) throws IOException {
        SevenZFile sevenZFile = new SevenZFile(file);
        return sevenZFile;
    }

    @Override
    public InputStream getPage(int num) throws IOException {
        try {
            // lazy parse
            parse();

            SevenZArchiveEntry needle = mEntries.get(num);
            // repeated reads throw "org.tukaani.xz.CorruptedInputException: Compressed data is corrupt"
            //return mSevenZFile.getInputStream(entry);
            // as a workaround we reopen and read each entry by tself
            //return getInputStream(entry);
            // or not, too slow by far, so extract'n'cache

            // populate cache (unless cover is requested, as in first parsing run)
            synchronized (this) {
                if (!mCachePopulated && num != 0) {
                    SevenZFile sevenZFile;
                    Object source = getSource();
                    if (source instanceof Uri)
                        sevenZFile = from((Uri) source);
                    else
                        sevenZFile = from((File) source);

                    initCacheDirectory(MainApplication.getAppContext().getExternalCacheDir());
                    // populate file cache
                    SevenZArchiveEntry entry;
                    while ((entry = sevenZFile.getNextEntry()) != null) {
                        if (Utils.isImage(entry.getName())) {
                            File cacheFile = new File(mCacheDir, Utils.MD5(entry.getName()));
                            byte[] content = new byte[(int) entry.getSize()];
                            sevenZFile.read(content);
                            Utils.copyToFile(new ByteArrayInputStream(content), cacheFile);
                            //Utils.copyToFile(sevenZFile.getInputStream(entry),cacheFile);
                        }
                    }
                    mCachePopulated = true;
                    Utils.close(sevenZFile);
                }
            }

            // try cache
            if (mCacheDir != null && mCachePopulated) {
                String name = needle.getName();
                File cacheFile = new File(mCacheDir, Utils.MD5(name));

                if (cacheFile.exists()) {
                    return new FileInputStream(cacheFile);
                }
            }

            // last chance, just read thru archive (very slow for file towards the end)
            return getInputStream(needle);
        } catch (Exception e) {
            Log.e("SevebZParser", "getPage", e);
            throw new IOException(e);
        }
    }

    private InputStream getInputStream(SevenZArchiveEntry needle) throws IOException {
        synchronized (this) {
            SevenZFile sevenZFile;
            Object source = getSource();
            if (source instanceof Uri)
                sevenZFile = from((Uri) source);
            else
                sevenZFile = from((File) source);

            try {
                // slow need to populate file cache
                SevenZArchiveEntry entry;
                while ((entry = sevenZFile.getNextEntry()) != null) {
                    if (entry.getName().equals(needle.getName())) {
                        byte[] content = new byte[(int) entry.getSize()];
                        sevenZFile.read(content);
                        return new ByteArrayInputStream(content);
                    }
                }
            } finally {
                Utils.close(sevenZFile);
            }
        }
        throw new IOException(needle.getName() + " not found in archive.");
    }

    @Override
    public Map getPageMetaData(int num) throws IOException {
        parse();
        return createPageMetaDataWithName(mEntries.get(num).getName());
    }

    @Override
    public String getType() {
        return "7zStream";
    }

    @Override
    public void destroy() {
        mEntries = null;
        Utils.rmDir(mCacheDir);
        mCacheDir = null;
        mCachePopulated = false;
        mCacheDir = null;
    }

    private void initCacheDirectory(File cacheDirectory) {
        mCacheDir = Utils.initCacheDirectory("sev");
    }
}
