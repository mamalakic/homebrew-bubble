package com.nkanaev.comics.parsers;

import java.io.*;
import java.util.*;

import android.util.Log;
import com.nkanaev.comics.managers.IgnoreCaseComparator;
import com.nkanaev.comics.managers.Utils;
import net.sf.sevenzipjbinding.*;
import net.sf.sevenzipjbinding.impl.*;

public class LibSevenZParser extends AbstractParser {
    private static final String TAG = "LibSevenZParser";
    private List<ArchiveEntry> mEntries = null;
    private String mArchiveFormat = null;
    private File mUncompressedFile = null;

    public LibSevenZParser() {
        super(new Class[]{File.class});
    }

    @Override
    public synchronized void parse() throws IOException {
        if (mEntries != null)
            return;

        File file = (File) getSource();
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
        RandomAccessFileInStream stream = new RandomAccessFileInStream(randomAccessFile);
        IInArchive archive;
        try {
            archive = SevenZip.openInArchive(null, stream);
        } catch (SevenZipException e) {
            throw new IOException( "Parsing file " + file.getPath() + " resulted in an error.", e );
        }

        ArchiveFormat format = archive.getArchiveFormat();
        mArchiveFormat = format.getMethodName();

        if (Arrays.asList(ArchiveFormat.GZIP,ArchiveFormat.BZIP2,ArchiveFormat.LZMA).contains(format)) {
            File folder = Utils.initCacheDirectory("tar");
            File tarFile = new File(folder, "plain.tar");
            toUncompressedFile(archive,tarFile);

            mUncompressedFile = tarFile;
            // close compressed file
            Utils.close(archive);
            Utils.close(stream);
            Utils.close(randomAccessFile);
            // open uncompressed file
            randomAccessFile = new RandomAccessFile(mUncompressedFile, "r");
            stream = new RandomAccessFileInStream(randomAccessFile);
            archive = SevenZip.openInArchive(null, stream);
            // append archive format metadata
            format = archive.getArchiveFormat();
            mArchiveFormat += ","+format.getMethodName();
        }

        int itemCount = archive.getNumberOfItems();
        //Log.d(TAG, "Items in archive: " + itemCount);
        List<ArchiveEntry> entries = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            String path = archive.getStringProperty(i, PropID.PATH);
            boolean isFolder = (boolean) archive.getProperty(i, PropID.IS_FOLDER);
            //Log.d(TAG, "File " + i + ": " + path + " : " + isFolder);
            if (isFolder || !Utils.isImage(path))
                continue;

            // populate comic page entry
            ArchiveEntry entry = new ArchiveEntry();
            entry.path = path;
            entry.index = i;
            entries.add(entry);
        }

        Collections.sort(entries, IgnoreCaseComparator.forFunction((ArchiveEntry e) -> e.path));

        mEntries = entries;

        // cleanup
        Utils.close(archive);
        Utils.close(stream);
        Utils.close(randomAccessFile);
    }

    @Override
    public InputStream getPage(int num) throws IOException {
        parse();
        int i = mEntries.get(num).index;

        ExtractOperationResult result = null;
        Utils.ByteArrayOutputToInputStream bos = new Utils.ByteArrayOutputToInputStream();
        ISequentialOutStream sos = new ISequentialOutStream(){
            @Override
            public int write(byte[] data) throws SevenZipException {
                try {
                    bos.write(data);
                } catch (IOException e) {
                    throw new SevenZipException(e);
                }
                return data.length;
            }
        };
        RandomAccessFileInStream stream = null;
        IInArchive archive = null;
        try {
            File file = mUncompressedFile!=null ? mUncompressedFile : (File) getSource();
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
            stream = new RandomAccessFileInStream(randomAccessFile);
            archive = SevenZip.openInArchive(null, stream);
            result = archive.extractSlow(i, sos);
        } catch (SevenZipException e) {
            Log.e(TAG, "extraction error", e);
        } finally {
            Utils.close(stream);
            Utils.close(archive);
        }
        if (result != ExtractOperationResult.OK) {
            Log.e(TAG, String.valueOf(result));
        }

        return bos.getInputStream();
    }

    @Override
    public int numPages() throws IOException {
        parse();
        return mEntries != null ? mEntries.size() : 0;
    }

    @Override
    public String getType() {
        return "Lib7z"+(mArchiveFormat!=null?"+"+mArchiveFormat:"");
    }

    @Override
    public Map getPageMetaData(int num) throws IOException {
        parse();
        return createPageMetaDataWithName(mEntries.get(num).path);
    }

    @Override
    public void destroy() {
        super.destroy();

        Utils.close(mEntries);
        mEntries = null;
        // delete cached if exists
        if (mUncompressedFile != null) {
            Utils.rmDir(mUncompressedFile.getParentFile());
            mUncompressedFile = null;
        }
    }

    public static boolean isAvailable() {
        try {
            SevenZip.initSevenZipFromPlatformJAR();
        } catch (SevenZipNativeInitializationException e) {
            Log.e(TAG,"cannot init lib7z", e);
        }
        return SevenZip.isInitializedSuccessfully();
    }

    private class ArchiveEntry {
        public int index;
        public String path;
    }

    static private void toUncompressedFile(IInArchive archive, File file) throws IOException {
        OutputStream outStream = new FileOutputStream(file);
        ExtractOperationResult result = null;
        ISequentialOutStream sos = new ISequentialOutStream(){
            @Override
            public int write(byte[] data) throws SevenZipException {
                try {
                    outStream.write(data);
                } catch (IOException e) {
                    throw new SevenZipException(e);
                }
                return data.length;
            }
        };
        try {
            result = archive.extractSlow(0, sos);
        } finally {
            Utils.close(outStream);
            Utils.close(archive);
        }
        if (result != ExtractOperationResult.OK) {
            Log.e(TAG, String.valueOf(result));
        }
    }
}
