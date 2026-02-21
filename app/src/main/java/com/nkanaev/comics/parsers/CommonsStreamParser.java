package com.nkanaev.comics.parsers;

import com.nkanaev.comics.managers.IgnoreCaseComparator;
import com.nkanaev.comics.managers.Utils;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;

import java.io.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommonsStreamParser extends AbstractParser {
    private List<StreamArchiveEntry> mEntries;

    private class StreamArchiveEntry {
        final ArchiveEntry entry;
        final byte[] bytes;

        public StreamArchiveEntry(ArchiveEntry entry, byte[] bytes) {
            this.entry = entry;
            this.bytes = bytes;
        }
    }

    public CommonsStreamParser() {
        super(new Class[]{InputStream.class});
    }

    public void parse() throws IOException {
        InputStream is = (InputStream) getSource();

        BufferedInputStream bis = null;
        ArchiveInputStream ais = null;
        try {
            bis = new BufferedInputStream(is);
            ais = new ArchiveStreamFactory().createArchiveInputStream(bis);
            ArchiveEntry entry = null;
            mEntries = new ArrayList<>();
            while ((entry = ais.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                if (Utils.isImage(entry.getName())) {
                    // read to memory
                    mEntries.add(new StreamArchiveEntry(entry, Utils.toByteArray(ais)));
                }
            }

            Collections.sort(mEntries, IgnoreCaseComparator.forFunction(
                (StreamArchiveEntry e) -> e.entry.getName()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            Utils.close(ais);
            Utils.close(bis);
            Utils.close(is);
        }
    }

    @Override
    public void destroy() {
        mEntries.clear();
    }

    @Override
    public String getType() {
        return "CommonsStream";
    }

    @Override
    public int numPages() {
        return mEntries.size();
    }

    @Override
    public InputStream getPage(int num) throws IOException {
        return new ByteArrayInputStream(mEntries.get(num).bytes);
    }

}
