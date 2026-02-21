package com.nkanaev.comics.parsers;


import com.nkanaev.comics.managers.IgnoreCaseComparator;
import com.nkanaev.comics.managers.Utils;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SevenZFileParser extends AbstractParser {
    private SevenZFile mSevenZFile = null;
    private List<SevenZArchiveEntry> mEntries = null;
    private boolean mParsedAlready = false;

    public SevenZFileParser() {
        super(new Class[]{File.class});
    }

    public synchronized void parse() throws IOException {
        if (mParsedAlready)
            return;

        mSevenZFile = new SevenZFile((File) getSource());
        mEntries = new ArrayList<>();
        for (SevenZArchiveEntry entry : mSevenZFile.getEntries()) {
            if (entry.isDirectory()) {
                continue;
            }
            if (Utils.isImage(entry.getName())) {
                mEntries.add(entry);
            }
        }

        Collections.sort(mEntries, IgnoreCaseComparator.forFunction(SevenZArchiveEntry::getName));

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
        SevenZArchiveEntry needle = mEntries.get(num);
        return mSevenZFile.getInputStream(needle);
    }

    @Override
    public String getType() {
        return "7zFile";
    }

    @Override
    public void destroy() {
        Utils.close(mSevenZFile);
        mSevenZFile = null;
        mEntries.clear();
        mEntries = null;
        mParsedAlready = false;
    }
}
