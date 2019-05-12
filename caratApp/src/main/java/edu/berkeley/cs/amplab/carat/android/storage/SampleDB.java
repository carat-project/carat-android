package edu.berkeley.cs.amplab.carat.android.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.HashMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;
import android.util.Log;

import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TMemoryBuffer;

import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.android.utils.Util;
import edu.berkeley.cs.amplab.carat.thrift.Sample;

/**
 * Stores samples in a SQLite database until sent. If you add COLUMNS in the
 * database, check all the TODO:s for places you need to update.
 * 
 * @author Eemil Lagerspetz
 * 
 */
public class SampleDB {

    private static final String TAG = "SampleDB";

    public static final String COLUMN_TIMESTAMP = "timestamp";
    public static final String COLUMN_SAMPLE = "sample";

    public static final String DATABASE_NAME = "caratdata";
    public static final String SAMPLES_VIRTUAL_TABLE = "sampleobjects";
    // TODO: Bump version here when changing the protocol, new one incompatible with old
    private static final int DATABASE_VERSION = 5;

    private static final HashMap<String, String> mColumnMap = buildColumnMap();

    private Sample lastSample = null;

    private SQLiteDatabase db = null;

    private SampleDbOpenHelper helper = null;

    private static SampleDB instance = null;

    private static Object dbLock = new Object();

    public static SampleDB getInstance(Context c) {
        if (instance == null) {
            instance = new SampleDB(c);
//            Logger.d("SampleDB", "new instance created and returned");
        }
        return instance;
    }

    private SampleDB(Context context) {
        synchronized (dbLock) {
            helper = new SampleDbOpenHelper(context);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        synchronized (dbLock) {
            if (db != null)
                db.close();
        }
        super.finalize();
    }

    /**
     * 
     * Builds a map for all columns that may be requested, which will be given
     * to the SQLiteQueryBuilder. This is a good way to define aliases for
     * column names, but must include all columns, even if the value is the key.
     * This allows the ContentProvider to request columns w/o the need to know
     * real column names and create the alias itself.
     * 
     * TODO: Needs to be updated when fields update.
     */
    private static HashMap<String, String> buildColumnMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(COLUMN_TIMESTAMP, COLUMN_TIMESTAMP);
        map.put(COLUMN_SAMPLE, COLUMN_SAMPLE);
        map.put(BaseColumns._ID, "rowid AS " + BaseColumns._ID);
        return map;
    }

    /**
     * Performs a database query.
     * 
     * @param selection
     *            The selection clause
     * @param selectionArgs
     *            Selection arguments for "?" components in the selection
     * @param columns
     *            The columns to return
     * @return A Cursor over all rows matching the query
     */
    private Cursor query(String selection, String[] selectionArgs,
            String[] columns, String groupBy, String having, String sortOrder) {
        /*
         * The SQLiteBuilder provides a map for all possible columns requested
         * to actual columns in the database, creating a simple column alias
         * mechanism by which the ContentProvider does not need to know the real
         * column names
         */
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(SAMPLES_VIRTUAL_TABLE);
        builder.setProjectionMap(mColumnMap);

        Cursor cursor = builder.query(db, columns, selection, selectionArgs,
                groupBy, having, sortOrder);
        
        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }
    
    public int countSamples() {
        try {
            synchronized (dbLock) {
                if (db == null || !db.isOpen()) {
                    try{
                        db = helper.getWritableDatabase();
                    }catch (android.database.sqlite.SQLiteException ex){
                        Logger.e(TAG, "Could not open database", ex);
                        return -1;
                    }
                }
                
                Cursor cursor = db.rawQuery("select count(timestamp) FROM "+SAMPLES_VIRTUAL_TABLE, null);

                if (cursor == null) {
                    // There are no results
                    return -1;
                } else {
                    int ret = -1;
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        ret = cursor.getInt(0);
                        cursor.moveToNext();
                    }
                    cursor.close();
                    return ret;
                }
            }
        } catch (Throwable th) {
            Logger.e(TAG, "Failed to query oldest samples!", th);
        }
        return -1;
    }

    public SortedMap<Long, Sample> querySamples(int howmany, boolean ascending) {
        SortedMap<Long, Sample> results = new TreeMap<Long, Sample>();
        try {
            Logger.d(TAG, "Awaiting for sampleDB lock at queryOldestSamples");
            synchronized (dbLock) {
                Logger.d(TAG, "Got sampleDB lock at queryOldestSamples");
                if (db == null || !db.isOpen()) {
                	try{
                		db = helper.getWritableDatabase();
                	}catch (android.database.sqlite.SQLiteException ex){
                		Logger.e(TAG, "Could not open database", ex);
                		return results;
                	}
                }
                String[] columns = mColumnMap.keySet().toArray(
                        new String[mColumnMap.size()]);

                String order = ascending ? "ASC" : "DESC";
                Cursor cursor = query(null, null, columns, null, null,
                        COLUMN_TIMESTAMP + " " + order + " LIMIT " + howmany);

                if (cursor == null) {
                	// Logger.d("SampleDB", "query returned null");
                    // There are no results
                    return results;
                } else {
                	// Logger.d("SampleDB", "query is successfull!");
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast()) {
                        Sample s = fillSample(cursor);
                        if (s != null) {
                            results.put(cursor.getLong(cursor
                                    .getColumnIndex(BaseColumns._ID)), s);
                        }
                        // TODO: Delete null samples
                        cursor.moveToNext();
                    }
                    cursor.close();

                }
            }
        } catch (Throwable th) {
            Logger.e(TAG, "Failed to query oldest samples!", th);
        }
        return results;
    }

    private int delete(String whereClause, String[] whereArgs) {
        int deleted = db.delete(SAMPLES_VIRTUAL_TABLE, whereClause, whereArgs);
        return deleted;
    }

    public int deleteSamples(Set<Long> rowids) {
        int ret = 0;
        try {
            synchronized (dbLock) {
                if (db == null || !db.isOpen()) {
                    db = helper.getWritableDatabase();
                }
                StringBuilder sb = new StringBuilder();
                int i = 0;
                sb.append("(");
                for (Long rowid : rowids) {
                    sb.append("" + rowid);
                    i++;
                    if (i != rowids.size()) {
                        sb.append(", ");
                    }
                }
                sb.append(")");
                if (Constants.DEBUG)
                    Logger.d("SampleDB",
                        "Deleting where rowid in " + sb.toString());
                ret = delete("rowid in " + sb.toString(), null);

                if (db != null && db.isOpen()) {
                    db.close();
                }
            }
        } catch (Throwable th) {
            Logger.e(TAG, "Failed to delete samples!", th);
        }
        return ret;
    }

    private Sample queryLastSample() {
        String[] columns = mColumnMap.keySet().toArray(
                new String[mColumnMap.size()]);

        Cursor cursor = query(null, null, columns, null, null, COLUMN_TIMESTAMP
                + " DESC LIMIT 1");

        if (cursor == null) {
            // There are no results
            return null;
        } else {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                Sample s = fillSample(cursor);
                cursor.close();
                lastSample = s;
                return s;
            }
            cursor.close();
            return null;
        }
    }

    public long recentSampleTimestamp(Context context) {
        SampleDB sampleDB = SampleDB.getInstance(context);
        long lastTimestamp = Long.MAX_VALUE;
        if (sampleDB != null) {
            // Traverse last 10 samples and try to find the most recent one
            Logger.d(TAG, "Querying last 10 samples");
            SortedMap<Long, Sample> sampleMap = sampleDB.querySamples(10, false);
            if (!Util.isNullOrEmpty(sampleMap)) {
                // Go back 5 minutes in case a new sample was taken after Carat was launched
                long now = System.currentTimeMillis();
                long momentAgo = now - TimeUnit.MINUTES.toMillis(5);
                Logger.d(TAG, "Now: " + now + " Moment ago: " + momentAgo);

                for (Sample sample : sampleMap.values()) {
                    long timestamp = (long) sample.timestamp * 1000; // Database uses seconds
                    Logger.d(TAG, "Sample has timestamp " + timestamp);
                    if (timestamp < momentAgo) {
                        Logger.d(TAG, "Smaller than " + momentAgo + " returning!");
                        lastTimestamp = timestamp;
                        break;
                    }
                }
            }
        }
        return lastTimestamp;
    }

    /*
     * Read a sample from the current position of the cursor. TODO: Needs to be
     * updated when fields update.
     */
    private Sample fillSample(Cursor cursor) {
        Sample s = new Sample();

        byte[] sampleB = cursor.getBlob(cursor.getColumnIndex(SampleDB.COLUMN_SAMPLE));
        if (sampleB != null) {
            try {
                TDeserializer deserializer = new TDeserializer(new TCompactProtocol.Factory());
                deserializer.deserialize(s, sampleB);
                return s;
            } catch (TException e) {
                Util.printStackTrace(TAG, e);
            }
        }
        return null;
    }

    public Sample getLastSample(Context c) {
        try {
            synchronized (dbLock) {
                if (db == null || !db.isOpen()) {
                	try{
                		db = helper.getWritableDatabase();
                	}catch (android.database.sqlite.SQLiteException ex){
                		Logger.e(TAG, "Could not open database", ex);
                		return lastSample;
                	}
                }
                if (lastSample == null)
                    queryLastSample();
            }
        } catch (Throwable th) {
            Logger.e(TAG, "Failed to get last sample!", th);
        }
        return lastSample;
    }

    /**
     * Store the sample into the database
     * @param s the sample to be saved
     * @return positive int if the operation is successful, otherwise zero
     */
    public long putSample(Sample s) {
        long id = 0;
        try {
            synchronized (dbLock) {
                if (db == null || !db.isOpen()) {
                    db = helper.getWritableDatabase();
                }
                if (Constants.DEBUG)
                    Logger.d(TAG, "SampleDB.putSample(). About to save a sample to the DB. "
                		+ "uuid=" + s.uuId
                		+ ", timestamp=" + s.timestamp
                		+ ", timezone=" + s.timeZone
                		+ ", batteryLevel=" + s.batteryLevel
                		+ ", batteryState=" + s.batteryState
                		+ ", developerMode=" + s.developerMode
                		+ ", distanceTraveled=" + s.distanceTraveled
                		+ ", memoryActive=" + s.memoryActive
                		+ ", memoryFree=" + s.memoryFree
                		+ ", memoryInactive=" + s.memoryInactive
                		+ ", memoryWired=" + s.memoryWired
                		+ ", screenBrightness=" + s.screenBrightness
                		+ ", networkStatus=" + s.networkStatus
                		+ ", screenOn=" + s.screenOn
                		+ ", unknownSources=" + s.unknownSources
                		+ ", batteryCapacity=" + s.batteryDetails.batteryCapacity
                		+ ", batteryCharger=" + s.batteryDetails.batteryCharger
                		+ ", batteryHealth=" + s.batteryDetails.batteryHealth
                		+ ", batteryTechnology=" + s.batteryDetails.batteryTechnology
                		+ ", batteryVoltage=" + s.batteryDetails.batteryVoltage
//                		+ ", callStatus=" + s.callInfo.callStatus
//                		+ ", incomingCallTime=" + s.callInfo.incomingCallTime
//                		+ ", nonCallTime=" + s.callInfo.nonCallTime
//                		+ ", outgoingCallTime=" + s.callInfo.outgoingCallTime
                		+ ", cpuStatus=" + s.cpuStatus
                		+ ", mobileDataActivity=" + s.getNetworkDetails().mobileDataActivity
                		+ ", mobileDataStatus=" + s.getNetworkDetails().mobileDataStatus
                		+ ", mobileNetworkType=" + s.getNetworkDetails().mobileNetworkType
                		+ ", networkType=" + s.getNetworkDetails().networkType
                		+ ", wifiLinkSpeed=" + s.getNetworkDetails().wifiLinkSpeed
                		+ ", wifiSignalStrength=" + s.getNetworkDetails().wifiSignalStrength
                		+ ", wifiStatus=" + s.getNetworkDetails().wifiStatus
                        + ", simOperator=" + s.getNetworkDetails().simOperator
                        + ", networkOperator=" + s.getNetworkDetails().networkOperator
                		+ ", PiListSize=" + s.getPiListSize()
                		);
                
                // force init
                id = addSample(s);
                if (id >= 0) {
                    lastSample = s;
                }
                if (db != null && db.isOpen()) {
                    db.close();
                }
            }
        } catch (Throwable th) {
            Logger.e(TAG, "Failed to add a sample! Have you accepted EULA?", th);
        }
        return id;
    }

    /**
     * Add a sample to the database.
     * 
     * @return rowId or -1 if failed
     */
    private long addSample(Sample s) {
        if (Constants.DEBUG)
            Logger.d("SampleDB.addSample()", "The sample's battery level=" + s.getBatteryLevel());
    	
        ContentValues initialValues = new ContentValues();
        initialValues.put(COLUMN_TIMESTAMP, s.timestamp);
        // Write the sample hashmap as a blob
        try {
            TSerializer serializer = new TSerializer(new TCompactProtocol.Factory());
            byte[] sampleData = serializer.serialize(s);
            initialValues.put(COLUMN_SAMPLE, sampleData);
            return db.insert(SAMPLES_VIRTUAL_TABLE, null, initialValues);
        } catch (Exception e) {
            Util.printStackTrace(TAG, e, "Unable to serialize or put serialized sample to DB!");
            return -1;
        }
    }

    /**
     * This creates/opens the database.
     */
    private static class SampleDbOpenHelper extends SQLiteOpenHelper {
        private SQLiteDatabase mDatabase;

        /*
         * Note that FTS3 does not support column constraints and thus, you
         * cannot declare a primary key. However, "rowid" is automatically used
         * as a unique identifier, so when making requests, we will use "_id" as
         * an alias for "rowid"
         */
        private static final String FTS_TABLE_CREATE = "CREATE VIRTUAL TABLE "
                + SAMPLES_VIRTUAL_TABLE + " USING fts3 (" + createStatement()
                + ");";

        private static final String createStatement() {
            Set<String> set = mColumnMap.keySet();
            StringBuilder b = new StringBuilder();
            int i = 0;
            int size = set.size() - 1;
            for (String s : set) {
                if (s.equals(BaseColumns._ID))
                    continue;
                if (i + 1 == size)
                    b.append(s);
                else
                    b.append(s + ", ");
                i++;
            }
            return b.toString();
        }

        SampleDbOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            mDatabase = db;
            try {
                mDatabase.execSQL(FTS_TABLE_CREATE);
                /**
                 * Compact database here
                 */
                mDatabase.execSQL("PRAGMA auto_vacuum = 1;");
            } catch (Throwable th) {
                // Already created
                Logger.e(TAG, "DB create failed!", th);
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see
         * android.database.sqlite.SQLiteOpenHelper#onOpen(android.database.
         * sqlite.SQLiteDatabase)
         */
        @Override
        public void onOpen(SQLiteDatabase db) {
            mDatabase = db;
            super.onOpen(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + SAMPLES_VIRTUAL_TABLE);
            onCreate(db);
        }
    }
}
