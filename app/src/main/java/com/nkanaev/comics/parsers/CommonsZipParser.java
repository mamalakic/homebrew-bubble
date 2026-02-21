package com.nkanaev.comics.parsers;

import com.nkanaev.comics.managers.IgnoreCaseComparator;
import com.nkanaev.comics.managers.Utils;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.SeekableByteChannel;
import java.util.*;


public class CommonsZipParser extends AbstractParser {
    private ZipFile mZipFile = null;
    private ArrayList<ZipArchiveEntry> mEntries = null;

    public CommonsZipParser() {
        super(new Class[]{File.class, SeekableByteChannel.class});
    }

    @Override
    public synchronized void parse() throws IOException {
        if (mZipFile != null && mEntries != null)
            return;

        Object o = getSource();
        if (o instanceof File)
            mZipFile = new ZipFile(((File)o).getAbsolutePath());
        else
            mZipFile = new ZipFile((SeekableByteChannel)o);

        mEntries = new ArrayList<>();

        Enumeration<ZipArchiveEntry> e = mZipFile.getEntries();
        while (e.hasMoreElements()) {
            ZipArchiveEntry ze = e.nextElement();
            if (!ze.isDirectory() && Utils.isImage(ze.getName())) {
                mEntries.add(ze);
            }
        }

        //noinspection unchecked
        Collections.sort(mEntries, IgnoreCaseComparator.forFunction(ZipArchiveEntry::getName));
    }

    @Override
    public int numPages() throws IOException {
        // lazy parse
        parse();
        return mEntries.size();
    }

    @Override
    public InputStream getPage(int num) throws IOException {
        parse();
        return mZipFile.getInputStream(mEntries.get(num));
    }

    @Override
    public Map getPageMetaData(int num) throws IOException {
        parse();
        return createPageMetaDataWithNameAndSize(
            mEntries.get(num).getName(),
            mEntries.get(num).getSize()
        );
    }

    @Override
    public String getType() {
        return "CommonsZip";
    }

    @Override
    public void destroy() {
        Utils.close(mZipFile);
        mZipFile = null;
        mEntries = null;
    }

}
