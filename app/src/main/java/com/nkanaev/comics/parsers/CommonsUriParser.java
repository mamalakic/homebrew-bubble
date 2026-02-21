package com.nkanaev.comics.parsers;

import android.net.Uri;
import com.nkanaev.comics.MainApplication;
import com.nkanaev.comics.managers.IgnoreCaseComparator;
import com.nkanaev.comics.managers.Utils;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommonsUriParser extends AbstractParser {
    private List<ArchiveEntry> mEntries = new ArrayList<>();

    public CommonsUriParser(Uri mUri) {
        super(new Class[]{Uri.class});
    }

    public void parse() throws IOException {
        Uri uri = (Uri) getSource();
        InputStream is = MainApplication.getAppContext().getContentResolver().openInputStream(uri);

        BufferedInputStream bis = null;
        ArchiveInputStream ais = null;
        try {
            bis = new BufferedInputStream(is);
            ais = new ArchiveStreamFactory().createArchiveInputStream(bis);
            ArchiveEntry entry = null;

            while ((entry = ais.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                if (Utils.isImage(entry.getName())) {
                    // read to memory
                    mEntries.add(entry);
                }
            }

            Collections.sort(mEntries, IgnoreCaseComparator.forFunction(ArchiveEntry::getName));
        } catch (ArchiveException e) {
            throw new IOException(e);
        } finally {
            Utils.close(ais);
        }
    }

    @Override
    public void destroy() {
        mEntries.clear();
    }

    @Override
    public String getType() {
        return "CommonsUri";
    }

    @Override
    public int numPages() {
        return mEntries.size();
    }

    @Override
    public InputStream getPage(int num) throws IOException {
        ArchiveEntry neededEntry = mEntries.get(num);

        Uri uri = (Uri) getSource();
        InputStream is = MainApplication.getAppContext().getContentResolver().openInputStream(uri);
        BufferedInputStream bis = null;
        ArchiveInputStream ais = null;
        try {
            bis = new BufferedInputStream(is);
            ais = new ArchiveStreamFactory().createArchiveInputStream(bis);
            ArchiveEntry entry = null;
            byte[] byteArray = new byte[0];
            while ((entry = ais.getNextEntry()) != null) {
                if (entry.equals(neededEntry)) {
                    // read to memory
                    byteArray = Utils.toByteArray(ais);
                    break;
                }
            }
            return new ByteArrayInputStream(byteArray);
        } catch (ArchiveException e) {
            throw new IOException(e);
        } finally {
            Utils.close(ais);
        }
    }

}
