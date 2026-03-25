package com.nkanaev.comics.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;
import com.nkanaev.comics.managers.IgnoreCaseComparator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import static android.database.DatabaseUtils.sqlEscapeString;


public class Storage {
    public static abstract class Book implements BaseColumns {
        public static final String TABLE_NAME = "book";

        public static final String COLUMN_NAME_ID = "id";
        public static final String COLUMN_NAME_FILEPATH = "path";
        public static final String COLUMN_NAME_FILENAME = "name";
        public static final String COLUMN_NAME_NUM_PAGES = "num_pages";
        public static final String COLUMN_NAME_CURRENT_PAGE = "cur_page";
        public static final String COLUMN_NAME_TYPE = "type";
        public static final String COLUMN_NAME_UPDATED_AT = "updated_at";

        public static final String[] columns = {
                Book.COLUMN_NAME_ID,
                Book.COLUMN_NAME_FILEPATH,
                Book.COLUMN_NAME_FILENAME,
                Book.COLUMN_NAME_NUM_PAGES,
                Book.COLUMN_NAME_CURRENT_PAGE,
                Book.COLUMN_NAME_TYPE,
                Book.COLUMN_NAME_UPDATED_AT
        };
    }

    public class ComicDbHelper extends SQLiteOpenHelper {
        public static final int DATABASE_VERSION = 2;
        public static final String DATABASE_NAME = "comics.db";

        public ComicDbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            final String sql = "CREATE TABLE " + Book.TABLE_NAME + " ("
                    + Book.COLUMN_NAME_ID + " INTEGER PRIMARY KEY,"
                    + Book.COLUMN_NAME_FILEPATH + " TEXT,"
                    + Book.COLUMN_NAME_FILENAME + " TEXT,"
                    + Book.COLUMN_NAME_NUM_PAGES + " INTEGER,"
                    + Book.COLUMN_NAME_CURRENT_PAGE + " INTEGER DEFAULT 0,"
                    + Book.COLUMN_NAME_TYPE + " TEXT,"
                    + Book.COLUMN_NAME_UPDATED_AT + " INTEGER"
                    + ")";
            db.execSQL(sql);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                db.execSQL("ALTER TABLE " + Book.TABLE_NAME + " ADD COLUMN " + Book.COLUMN_NAME_UPDATED_AT + " INTEGER");
            }
        }
    }

    private ComicDbHelper mDbHelper;
    private static Storage mSharedInstance;

    private static final String SORT_ORDER = "lower(" + Book.COLUMN_NAME_FILEPATH + "|| '/' || " + Book.COLUMN_NAME_FILENAME + ") ASC";

    private Storage(Context context) {
        mDbHelper = new ComicDbHelper(context);
    }

    public static Storage getStorage(Context context) {
        if (mSharedInstance == null) {
            synchronized (Storage.class) {
                if (mSharedInstance == null) {
                    mSharedInstance = new Storage(context);
                }
            }
        }
        return mSharedInstance;
    }

    public void clearStorage() {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.delete(Book.TABLE_NAME, null, null);
    }

    private ContentValues buildContentValues(File filepath, String type, Integer numPages){
        ContentValues cv = new ContentValues();
        if (filepath!=null) {
            cv.put(Book.COLUMN_NAME_FILEPATH, filepath.getParentFile().getAbsolutePath());
            cv.put(Book.COLUMN_NAME_FILENAME, filepath.getName());
        }

        if (type != null)
            cv.put(Book.COLUMN_NAME_TYPE, type);

        if (numPages != null)
            cv.put(Book.COLUMN_NAME_NUM_PAGES, String.valueOf(numPages));

        return cv;
    }

    public void addBook(File filepath, String type, int numPages) {
        ContentValues cv = buildContentValues(filepath,type,numPages);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.insert(Book.TABLE_NAME, "null", cv);
    }

    public void updateBook(int comicId, String type, int numPages) {
        ContentValues cv = buildContentValues(null,type,numPages);
        // protect from empty changesets, db.update() would throw exception
        if (cv == null || cv.size()<1)
            return;
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        String whereClause = Book.COLUMN_NAME_ID + '=' + sqlEscapeString(Integer.toString(comicId));
        db.update(Book.TABLE_NAME,cv,whereClause,null);
    }

    public void resetBook(int comicId) {
        Comic c = getComic(comicId);
        // prevent NPE if id does not exist
        if (c == null)
            return;

        removeComic(comicId);
        addBook(c.getFile(), c.getType(), c.getTotalPages());
    }

    public void markBookAsRead(int comicId) {
        Comic c = getComic(comicId);
        // prevent NPE if id does not exist
        if (c == null)
            return;

        bookmarkPage(comicId, c.getTotalPages());
    }

    public void removeComic(int comicId) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        String whereClause = Book.COLUMN_NAME_ID + '=' + Integer.toString(comicId);
        int i = db.delete(Book.TABLE_NAME, whereClause, null);
    }

    public ArrayList<Comic> listDirectoryComics() {
        return listDirectoryComics(null);
    }

    public ArrayList<Comic> listDirectoryComics(String libraryPath) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        Cursor c = db.query(
                Book.TABLE_NAME, Book.columns, null, null,
                null, null,
                SORT_ORDER);


        if (c.getCount() == 0)
            return new ArrayList<>();

        HashMap<String,Comic> dirComics = new HashMap<>();
        Comparator comparator = new IgnoreCaseComparator() {
            @Override
            public String stringValue(Object o) {
                return o.toString();
            }
        };
        c.moveToFirst();
        do {
            String filepath = c.getString(c.getColumnIndex(Book.COLUMN_NAME_FILEPATH));
            
            // Filter by library path if provided
            if (libraryPath != null && !libraryPath.isEmpty()) {
                File comicFile = new File(filepath);
                File libFile = new File(libraryPath);
                boolean isInLibrary = false;
                File folder = comicFile.getAbsoluteFile();
                File absLibFolder = libFile.getAbsoluteFile();
                do {
                    if (folder.equals(absLibFolder)) {
                        isInLibrary = true;
                        break;
                    }
                } while ((folder = folder.getParentFile()) != null);
                
                if (!isInLibrary) {
                    continue;
                }
            }
            
            // initial entry
            if (!dirComics.containsKey(filepath)) {
                dirComics.put(filepath,comicFromCursor(c));
                continue;
            }
;
            // find first with more accurate natural sort comparator
            String fileNameOld = dirComics.get(filepath).getFile().getName();
            String fileNameNew = c.getString(c.getColumnIndex(Book.COLUMN_NAME_FILENAME));
            if (comparator.compare(fileNameOld,fileNameNew) > 0)
                dirComics.put(filepath,comicFromCursor(c));
        } while (c.moveToNext());

        c.close();

        return new ArrayList(dirComics.values());
    }

    public ArrayList<Comic> listComics() {
        return listComics(null,null);
    }

    public ArrayList<Comic> listComics(String path) {
        return listComics(path,null);
    }

    public ArrayList<Comic> listComics(String path, String fileName) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        String selection = "";
        if (path != null) {
            selection = Book.COLUMN_NAME_FILEPATH + "=\"" + path +  "\"";
        }
        if (fileName != null) {
            selection += (!selection.isEmpty()?" AND ":"")+Book.COLUMN_NAME_FILENAME+ "=\"" + fileName +  "\"";
        }

        Cursor c = db.query(Book.TABLE_NAME, Book.columns, selection, null, null, null, SORT_ORDER);
        ArrayList<Comic> comics = new ArrayList<>();

        c.moveToFirst();
        if (c.getCount() > 0) {
            do {
                comics.add(comicFromCursor(c));
            } while (c.moveToNext());
        }

        c.close();

        return comics;
    }

    public long getPathLatestUpdatedAt(String path){

        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String selection = Book.COLUMN_NAME_FILEPATH + "=\"" + path +  "\"";
        Cursor c = db.query(Book.TABLE_NAME, Book.columns, selection, null,
                null, null, Book.COLUMN_NAME_UPDATED_AT + " DESC", "0,1");
        c.moveToFirst();

        long time = 0;
        if (c.getCount() > 0) {
            time = c.getLong(c.getColumnIndex(Book.COLUMN_NAME_UPDATED_AT));
            Comic comic = comicFromCursor(c);
            Log.d("",""+time);
        }
        c.close();

        return time;
    }

    public Comic getComic(int comicId) {
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String order = Book.COLUMN_NAME_FILEPATH + " DESC";
        String selection = Book.COLUMN_NAME_ID + "=" + Integer.toString(comicId);
        Cursor c = db.query(Book.TABLE_NAME, Book.columns, selection, null, null, null, order);

        if (c.getCount() != 1) {
            return null;
        }

        c.moveToFirst();

        Comic comic = comicFromCursor(c);
        c.close();
        return comic;
    }

    private Comic comicFromCursor(Cursor c) {
        int id = c.getInt(c.getColumnIndex(Book.COLUMN_NAME_ID));
        String path = c.getString(c.getColumnIndex(Book.COLUMN_NAME_FILEPATH));
        String name = c.getString(c.getColumnIndex(Book.COLUMN_NAME_FILENAME));
        int numPages = c.getInt(c.getColumnIndex(Book.COLUMN_NAME_NUM_PAGES));
        int currentPage = c.getInt(c.getColumnIndex(Book.COLUMN_NAME_CURRENT_PAGE));
        String type = c.getString(c.getColumnIndex(Book.COLUMN_NAME_TYPE));
        long updatedAt = c.getLong(c.getColumnIndex(Book.COLUMN_NAME_UPDATED_AT));

        return new Comic(this, id, path, name, type, numPages, currentPage, updatedAt);
    }

    public void bookmarkPage(int comicId, int page) {
        ContentValues values = new ContentValues();
        values.put(Book.COLUMN_NAME_CURRENT_PAGE, page);
        values.put(Book.COLUMN_NAME_UPDATED_AT, System.currentTimeMillis() / 1000);
        String filter = Book.COLUMN_NAME_ID + "=" + Integer.toString(comicId);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        db.update(Book.TABLE_NAME, values, filter, null);
    }

    public Comic getPrevComic(Comic comic) {
        return getNextComic(comic, new IgnoreCaseComparator.Reverse() {
            @Override
            public String stringValue(Object o) {
                return ((Comic) o).getFile().getName();
            }
        });
    }

    public Comic getNextComic(Comic comic) {
        return getNextComic(comic, new IgnoreCaseComparator() {
            @Override
            public String stringValue(Object o) {
                return ((Comic) o).getFile().getName();
            }
        });
    }

    // return next Comic or null, if comic was last
    // according to Comparator order
    private Comic getNextComic(Comic comic, Comparator comparator) {
        Comic next = null;
        ArrayList<Comic> comics = listComics(comic.getFile().getParent());
        for (Comic candidate : comics) {
            if (comparator.compare(candidate,comic ) > 0 &&
                    (next == null || comparator.compare(candidate, next) < 0))
                next = candidate;
        }
        return next;
    }

    public boolean exportToJson(File exportFile) {
        try {
            SQLiteDatabase db = mDbHelper.getReadableDatabase();
            Cursor cursor = db.query(Book.TABLE_NAME, Book.columns, null, null, null, null, null);
            
            JSONArray jsonArray = new JSONArray();
            
            if (cursor.moveToFirst()) {
                do {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put(Book.COLUMN_NAME_FILEPATH, cursor.getString(cursor.getColumnIndex(Book.COLUMN_NAME_FILEPATH)));
                    jsonObject.put(Book.COLUMN_NAME_FILENAME, cursor.getString(cursor.getColumnIndex(Book.COLUMN_NAME_FILENAME)));
                    jsonObject.put(Book.COLUMN_NAME_NUM_PAGES, cursor.getInt(cursor.getColumnIndex(Book.COLUMN_NAME_NUM_PAGES)));
                    jsonObject.put(Book.COLUMN_NAME_CURRENT_PAGE, cursor.getInt(cursor.getColumnIndex(Book.COLUMN_NAME_CURRENT_PAGE)));
                    jsonObject.put(Book.COLUMN_NAME_TYPE, cursor.getString(cursor.getColumnIndex(Book.COLUMN_NAME_TYPE)));
                    jsonObject.put(Book.COLUMN_NAME_UPDATED_AT, cursor.getLong(cursor.getColumnIndex(Book.COLUMN_NAME_UPDATED_AT)));
                    jsonArray.put(jsonObject);
                } while (cursor.moveToNext());
            }
            cursor.close();
            
            FileOutputStream fos = new FileOutputStream(exportFile);
            fos.write(jsonArray.toString(2).getBytes());
            fos.close();
            
            return true;
        } catch (Exception e) {
            Log.e("Storage", "Export failed: " + e.getMessage());
            return false;
        }
    }

    public int importFromJson(File importFile, boolean merge) {
        int importedCount = 0;
        try {
            FileInputStream fis = new FileInputStream(importFile);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            
            JSONArray jsonArray = new JSONArray(sb.toString());
            SQLiteDatabase db = mDbHelper.getWritableDatabase();
            
            if (!merge) {
                db.delete(Book.TABLE_NAME, null, null);
            }
            
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                
                String filepath = jsonObject.getString(Book.COLUMN_NAME_FILEPATH);
                String filename = jsonObject.getString(Book.COLUMN_NAME_FILENAME);
                
                if (merge) {
                    ArrayList<Comic> existing = listComics(filepath, filename);
                    if (!existing.isEmpty()) {
                        int existingId = existing.get(0).getId();
                        ContentValues values = new ContentValues();
                        values.put(Book.COLUMN_NAME_NUM_PAGES, jsonObject.getInt(Book.COLUMN_NAME_NUM_PAGES));
                        values.put(Book.COLUMN_NAME_CURRENT_PAGE, jsonObject.getInt(Book.COLUMN_NAME_CURRENT_PAGE));
                        values.put(Book.COLUMN_NAME_TYPE, jsonObject.getString(Book.COLUMN_NAME_TYPE));
                        values.put(Book.COLUMN_NAME_UPDATED_AT, jsonObject.getLong(Book.COLUMN_NAME_UPDATED_AT));
                        String whereClause = Book.COLUMN_NAME_ID + '=' + existingId;
                        db.update(Book.TABLE_NAME, values, whereClause, null);
                        importedCount++;
                        continue;
                    }
                }
                
                ContentValues values = new ContentValues();
                values.put(Book.COLUMN_NAME_FILEPATH, filepath);
                values.put(Book.COLUMN_NAME_FILENAME, filename);
                values.put(Book.COLUMN_NAME_NUM_PAGES, jsonObject.getInt(Book.COLUMN_NAME_NUM_PAGES));
                values.put(Book.COLUMN_NAME_CURRENT_PAGE, jsonObject.getInt(Book.COLUMN_NAME_CURRENT_PAGE));
                values.put(Book.COLUMN_NAME_TYPE, jsonObject.getString(Book.COLUMN_NAME_TYPE));
                values.put(Book.COLUMN_NAME_UPDATED_AT, jsonObject.getLong(Book.COLUMN_NAME_UPDATED_AT));
                
                db.insert(Book.TABLE_NAME, null, values);
                importedCount++;
            }
            
            return importedCount;
        } catch (Exception e) {
            Log.e("Storage", "Import failed: " + e.getMessage());
            return -1;
        }
    }
}
