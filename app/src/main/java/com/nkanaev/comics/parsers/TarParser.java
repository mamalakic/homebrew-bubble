package com.nkanaev.comics.parsers;


import com.nkanaev.comics.managers.IgnoreCaseComparator;
import com.nkanaev.comics.managers.Utils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TarParser extends AbstractParser {
    private List<TarEntry> mEntries = null;

    private boolean mParsedAlready = false;

    private class TarEntry {
        final TarArchiveEntry entry;
        byte[] bytes;

        public TarEntry(TarArchiveEntry entry, byte[] bytes) {
            this.entry = entry;
            this.bytes = bytes;
        }
    }

    public TarParser() {
        super(new Class[]{File.class});
    }

    private TarArchiveInputStream initStream(File file) throws IOException {
        String fileName = file.getName();
        InputStream is = null;

        is = new BufferedInputStream(new FileInputStream(file));
        try {
            if (Utils.isTGZ(fileName))
                is = new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.GZIP, is);
            else if (Utils.isTBZ(fileName))
                is = new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.BZIP2, is);
            else if (Utils.isTXZ(fileName))
                is = new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.LZMA, is);
            else if (Utils.isTZST(fileName))
                is = new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.ZSTANDARD, is);
            else if (Utils.isTBR(fileName))
                is = new CompressorStreamFactory().createCompressorInputStream(CompressorStreamFactory.BROTLI, is);
        } catch (CompressorException e) {
            throw new IOException(e);
        }

        return new TarArchiveInputStream(is, true);
    }

    public void parse() throws IOException {
        parse(1);
    }

    private void parse(int limit) throws IOException {
        if (mParsedAlready)
            return;

        File file = (File) getSource();

        TarArchiveInputStream tis = null;
        try {
            tis = initStream(file);
            TarArchiveEntry entry;
            ArrayList entries = new ArrayList<>();
            while ((entry = tis.getNextTarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                // TODO: cache to file system instead
                if (Utils.isImage(entry.getName())) {
                    boolean read = limit < 0 || entries.size() <= limit;
                    entries.add(new TarEntry(entry, read ? Utils.toByteArray(tis) : null));
                }
            }

            Collections.sort(entries, IgnoreCaseComparator.forFunction(
                (TarEntry e) -> e.entry.getName()));

            mEntries = entries;
            mParsedAlready = true;
        } finally {
            Utils.close(tis);
        }

    }

    @Override
    public int numPages() throws IOException {
        parse(0);
        return mEntries != null ? mEntries.size() : 0;
    }

    @Override
    public synchronized InputStream getPage(int num) throws IOException {
        synchronized (mEntries) {
            byte[] data = mEntries.get(num).bytes;
            // (re)read archive data to memory
            if (data == null) {
                mParsedAlready = false;
                parse(-1);
            }
        }
        return new ByteArrayInputStream(mEntries.get(num).bytes);
    }

    @Override
    public String getType() {
        return "tar";
    }

    @Override
    public void destroy() {
        // delete all cached but cover page (0)
        for (int i = 1; i < mEntries.size(); i++) {
            mEntries.get(i).bytes = null;
        }
        mParsedAlready = false;
    }

}
