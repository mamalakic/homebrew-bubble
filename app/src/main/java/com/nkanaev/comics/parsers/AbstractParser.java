package com.nkanaev.comics.parsers;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import com.nkanaev.comics.MainApplication;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

public abstract class AbstractParser implements Parser {
    // remember source, can be either File or Intent
    protected Object mSource = null;
    protected Class[] mParseables;

    private AbstractParser() {
        // disallowed
    }

    protected AbstractParser(Class[] parseables) {
        if (parseables == null || parseables.length < 1)
            throw new IllegalArgumentException("parseable classes must be given");

        mParseables = parseables;
    }

    protected Object getSource() {
        return mSource;
    }

    protected void setSource(Object source) {
        if (source == null)
            throw new IllegalArgumentException("source must not be null");
        Class sourceClazz = source.getClass();
        if (!canParse(sourceClazz))
            throw new IllegalArgumentException(getClass().getCanonicalName() + " cannot parse " + sourceClazz.getCanonicalName());

        mSource = source;
    }

    public static Uri uriFromIntent(Intent intent) {
        Context context = MainApplication.getAppContext();
        return intent.getData();
    }

    public static FileDescriptor fileDescriptorFromIntent(Intent intent) throws IOException {
        Uri uri = uriFromIntent(intent);
        AssetFileDescriptor afd = MainApplication.getAppContext()
                .getContentResolver().openAssetFileDescriptor(uri, "r");
        return afd.getFileDescriptor();
    }

    public static FileInputStream inputStreamFromIntent(Intent intent) throws IOException {
        return new FileInputStream(fileDescriptorFromIntent(intent));
    }

    public static FileChannel fileChannelFromIntent(Intent intent) throws IOException {
        return inputStreamFromIntent(intent).getChannel();
    }

    public static File fileFromIntent(Intent intent) {
        return new File(uriFromIntent(intent).getPath());
    }

    abstract public void parse() throws IOException;
/*
    protected void parse(Intent intent) throws IOException {
        mSource = intent;
        Uri uri = uriFromIntent(intent);
        String scheme = uri.getScheme();
        if (scheme != null && scheme.equalsIgnoreCase("file"))
            parse(fileFromIntent(intent));
        else if (scheme != null && scheme.equalsIgnoreCase("content"))
            if (canParse(InputStream.class))
                parse(inputStreamFromIntent(intent));
            else if (canParse(FileChannel.class))
                parse(fileChannelFromIntent(intent));
            else
                new IllegalArgumentException("Intent Data parsing not supported.");
        else
            throw new IllegalArgumentException("Intent Data Uri scheme must be file:// or content://");
    }

    protected void parse(FileChannel fileChannel) throws IOException {
        throw new UnsupportedOperationException();
    }

    protected void parse(InputStream inputStream) throws IOException {
        throw new UnsupportedOperationException();
    }

    protected void parse(File file) throws IOException {
        throw new UnsupportedOperationException();
    }
*/
    @Override
    abstract public InputStream getPage(int num) throws IOException;

    // not strictly necessary, implement to provide details about book pages (file name, resolution ...)
    @Override
    public Map getPageMetaData(int num) throws IOException {
        return Collections.emptyMap();
    }

    /**
     * Helper method to create page metadata with just a name.
     * Reduces duplication across parser implementations.
     */
    protected Map<String, String> createPageMetaDataWithName(String name) {
        Map<String, String> m = new java.util.HashMap<>();
        m.put(Parser.PAGEMETADATA_KEY_NAME, name);
        return m;
    }

    /**
     * Helper method to create page metadata with name and size.
     */
    protected Map<String, String> createPageMetaDataWithNameAndSize(String name, long size) {
        Map<String, String> m = createPageMetaDataWithName(name);
        m.put(Parser.PAGEMETADATA_KEY_SIZE, String.valueOf(size));
        return m;
    }

    @Override
    abstract public int numPages() throws IOException;

    @Override
    public void destroy() {
        // implement as needed
    }

    @Override
    abstract public String getType();

    public boolean canParse(Class clazz) {
        // add some valid classes in constructor
        // of your actual implementation
        return Arrays.asList(mParseables).contains(clazz);
    }
}
