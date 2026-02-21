package com.nkanaev.comics.managers;

import com.nkanaev.comics.MainApplication;
import com.nkanaev.comics.model.Comic;
import com.nkanaev.comics.model.Storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class DirectoryListingManager {
    private final List<Comic> mComics;
    private final File mLibraryDir;

    public DirectoryListingManager(List<Comic> comics, String libraryDir) {
        mLibraryDir = new File(libraryDir != null ? libraryDir : "/");
        mComics = comics;
    }

    private static String getBaseFolder(Comic c) {
        File folder = getBaseFolderFile(c);
        return folder == null ? "" : folder.getAbsolutePath();
    }

    private static File getBaseFolderFile(Comic c) {
        File folder = c.getFile().getParentFile();
        return folder;
    }

    public void sort(Comparator<Comic> comparator) {
        Collections.sort(mComics, comparator);
    }

    public String getDirectoryDisplayAtIndex(int idx) {
        File comicDir = getComicAtIndex(idx).getFile().getParentFile();

        if (comicDir.equals(mLibraryDir)) {
            return "~ (" + comicDir.getName() + ")";
        } else {
            return comicDir.getName();
        }
    }

    public Comic getComicAtIndex(int idx) {
        return mComics.get(idx);
    }

    public String getDirectoryAtIndex(int idx) {
        return mComics.get(idx).getFile().getParent();
    }

    public int getCount() {
        return mComics.size();
    }

    public static class NameComparator implements Comparator<Comic> {
        Comparator comparator = new IgnoreCaseComparator() {
            @Override
            public String stringValue(Object o) {
                return getBaseFolder((Comic) o);
            }
        };

        @Override
        public int compare(Comic o1, Comic o2) {
            return comparator.compare(o1, o2);
        }

        public static class Reverse extends NameComparator {
            public int compare(Comic a, Comic b) {
                return super.compare(b, a);
            }
        }
    }

    public static class SizeComparator implements Comparator<Comic> {
        HashMap<String, Long> sizeCache = new HashMap();

        @Override
        public int compare(Comic cA, Comic cB) {
            String pathA = getBaseFolder(cA);
            String pathB = getBaseFolder(cB);
            return Long.compare(computeSize(pathA), computeSize(pathB));
        }

        private long computeSize(String path) {
            if (sizeCache.containsKey(path))
                return sizeCache.get(path);

            List<Comic> comics = Storage.getStorage(MainApplication.getAppContext()).listComics(path);
            long size = 0;
            for (Comic comic : comics) {
                size += comic.getFile().length();
            }

            sizeCache.put(path, Long.valueOf(size));
            return size;
        }

        public static class Reverse extends SizeComparator {
            public int compare(Comic a, Comic b) {
                return super.compare(b, a);
            }
        }
    }

    public static class AccessedComparator implements Comparator<Comic> {
        HashMap<Comic, Long> stampCache = new HashMap();
        @Override
        public int compare(Comic a, Comic b) {
            return Long.compare(lastAccessed(a), lastAccessed(b));
        }

        public static class Reverse extends AccessedComparator {
            public int compare(Comic a, Comic b) {
                return super.compare(b, a);
            }
        }

        private long lastAccessed(Comic c){
            if (stampCache.containsKey(c)) {
                return stampCache.get(c);
            }

            String path = getBaseFolder(c);
            long stamp = Storage.getStorage(MainApplication.getAppContext()).getPathLatestUpdatedAt(path);
            stampCache.put(c,stamp);
            return stamp;
        }
    }

    public static class ModifiedComparator implements Comparator<Comic> {
        @Override
        public int compare(Comic a, Comic b) {
            return Long.compare(modified(a),modified(b));
        }

        private long modified(Comic c) {
            File f = getBaseFolderFile(c);
            return f == null ? 0 : f.lastModified();
        }

        public static class Reverse extends ModifiedComparator {
            public int compare(Comic a, Comic b) {
                return super.compare(b, a);
            }
        }
    }

    public static class CreationComparator implements Comparator<Comic> {
        @Override
        public int compare(Comic a, Comic b) {
            long aTime = creationTime(a);
            long bTime = creationTime(b);
            return Long.compare(aTime, bTime);
        }

        private long creationTime(Comic c){
            try {
                File f = getBaseFolderFile(c);
                // menu entry will only be added on API26+
                FileTime creationTime = (FileTime) Files.getAttribute(f.toPath(), "creationTime");
                long cTime = creationTime.toMillis();
                long mTime = f.lastModified();
                return cTime;
            } catch (IOException ex) {
                return 0;
            }
        }

        public static class Reverse extends CreationComparator {
            public int compare(Comic a, Comic b) {
                return super.compare(b, a);
            }
        }
    }
}
