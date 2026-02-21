package com.nkanaev.comics.parsers;


import android.util.Log;
import com.nkanaev.comics.managers.IgnoreCaseComparator;
import com.nkanaev.comics.managers.Utils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarFile;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import java.io.*;
import java.util.*;

public class TarFileParser extends AbstractParser {
    private File mUncompressedFile = null;
    private String mCompression = "";
    private TarFile mTarFile = null;
    private List<TarArchiveEntry> mEntries = new ArrayList<>();

    private boolean mParsedAlready = false;

    public TarFileParser() {
        super(new Class[]{File.class});
    }

    public synchronized void parse() throws IOException {
        if (mParsedAlready)
            return;

        File file = (File) getSource();

        // blindly try compression, assume uncompressed if that fails
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        CompressorInputStream cis = null;
        try {
            // brotli has no signature, use file name
            if (Utils.isTBR(file.getName()))
                mCompression = CompressorStreamFactory.getBrotli();
            else
                mCompression = CompressorStreamFactory.detect(bis);
            cis = CompressorStreamFactory.getSingleton().createCompressorInputStream(mCompression,bis);
            // setup cached uncompressed file
            File folder = Utils.initCacheDirectory("tar");
            File tarFile = new File(folder, "plain.tar");
            Utils.copyToFile(cis, tarFile);
            mUncompressedFile = tarFile;
            file = tarFile;
        } catch (CompressorException e) {
            Log.d("TarFileParser","parse()",e);
        } finally {
            // cleanup
            Utils.close(cis);
            Utils.close(bis);
            Utils.close(fis);
        }

        mTarFile = new TarFile(file);
        ArrayList<TarArchiveEntry> entries = new ArrayList<>();
        for (TarArchiveEntry entry : mTarFile.getEntries()) {
            if (entry.isDirectory()) {
                continue;
            }
            // TODO: cache to file system instead
            if (Utils.isImage(entry.getName())) {
                entries.add(entry);
            }
        }

        Collections.sort(entries, IgnoreCaseComparator.forFunction(TarArchiveEntry::getName));

        mEntries = entries;
        mParsedAlready = true;
    }

    @Override
    public int numPages() throws IOException {
        parse();
        return mEntries != null ? mEntries.size() : 0;
    }

    @Override
    public synchronized InputStream getPage(int num) throws IOException {
        parse();
        TarArchiveEntry needle = mEntries.get(num);
        InputStream is = mTarFile.getInputStream(needle);
        // cache to memory to prevent concurrent access via returned
        // input stream to underlying file channel
        byte[] data = Utils.toByteArray(is);
        return new ByteArrayInputStream(data){
            @Override
            public void close() throws IOException {
                // just to be make extra sure
                this.buf = null;
            }
        };
    }

    @Override
    public Map getPageMetaData(int num) throws IOException {
        parse();
        return createPageMetaDataWithName(mEntries.get(num).getName());
    }

    @Override
    public String getType() {
        return "TarFile"+(mCompression.isEmpty()?"":"+"+mCompression);
    }

    @Override
    public void destroy() {
        Utils.close(mTarFile);
        mTarFile = null;
        mEntries.clear();
        mEntries = null;
        if (mUncompressedFile != null) {
            Utils.rmDir(mUncompressedFile.getParentFile());
            mUncompressedFile = null;
        }
        mParsedAlready = false;
    }

}
