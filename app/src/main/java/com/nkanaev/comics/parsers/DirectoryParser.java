package com.nkanaev.comics.parsers;


import com.nkanaev.comics.managers.IgnoreCaseComparator;
import com.nkanaev.comics.managers.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DirectoryParser extends AbstractParser {
    private ArrayList<File> mFiles = null;
    public static final String TYPE = "folder";

    public DirectoryParser() {
        super(new Class[]{File.class});
    }

    @Override
    public synchronized void parse() throws IOException {
        if (mFiles != null) return;

        File dir = (File) getSource();

        if (!dir.isDirectory()) {
            throw new IOException("Not a directory: " + dir.getAbsolutePath());
        }

        File[] files = dir.listFiles();
        mFiles = new ArrayList<>();
        if (files != null) {
            for (File f : dir.listFiles()) {
                if (f.isFile() && Utils.isImage(f.getName())) {
                    mFiles.add(f);
                }
            }
        }

        Collections.sort(mFiles, IgnoreCaseComparator.forFunction(File::getName));
    }

    @Override
    public int numPages() throws IOException {
        parse();
        return mFiles.size();
    }

    @Override
    public InputStream getPage(int num) throws IOException {
        parse();
        return new FileInputStream(mFiles.get(num));
    }

    @Override
    public Map getPageMetaData(int num) throws IOException {
        parse();
        return createPageMetaDataWithName(mFiles.get(num).getName());
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
