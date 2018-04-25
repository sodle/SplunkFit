package com.sjodle.splunkfit;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.LongSparseArray;

import com.google.android.gms.fitness.data.DataType;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Calendar;
import java.util.Date;

public class DataIngestionService extends JobService {

    SharedPreferences appData;
    GoogleFitClient fitClient;
    HecClient hecClient;

    @Override
    public boolean onStartJob(final JobParameters params) {
        appData = this.getSharedPreferences(Constants.PREFS_FILE_KEY, Context.MODE_PRIVATE);
        fitClient = new GoogleFitClient(this);
        hecClient = new HecClient(this);
        Thread ingestTask = new Thread() {
            public void run() {
                Log.d("DataIngestionService", "Service started!");
                if (!fitClient.checkPermissions()) {
                    Log.e("DataIngestionService", "Not logged in!");
                    jobFinished(params, true);
                }
                ingestFitData();
                jobFinished(params, false);
            }
        };
        ingestTask.start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        jobFinished(params, true);
        return true;
    }

    private String getDataTypeCheckpointKey(DataType dataType) {
        return Constants.PREFS_CHECKPOINT_PREFIX + dataType.getName();
    }

    private void ingestFitDataType(DataType dataType) {
        String checkpointKey = getDataTypeCheckpointKey(dataType);
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        long latest = cal.getTimeInMillis();
        long earliest = appData.getLong(checkpointKey, -1);
        if (earliest == -1) {
            cal.add(Calendar.HOUR, -24);
            earliest = cal.getTimeInMillis();
        }
        LongSparseArray<JSONArray> dataPoints = fitClient.getDataPoints(dataType, earliest, latest);
        long newCheckpoint = earliest;
        for (int i = 0; i < dataPoints.size(); i++) {
            long key = dataPoints.keyAt(i);
            JSONArray dataPoint = dataPoints.get(key);
            boolean success = hecClient.ingest(dataPoint);
            if (!success)
                break;
            newCheckpoint = key;
        }
        SharedPreferences.Editor editor = appData.edit();
        editor.putLong(checkpointKey, newCheckpoint + 1); // TODO move to response/error handlers
        editor.apply();
        Log.d("DataIngestionService", "Ingested " + dataType.getName() + " up to " + newCheckpoint);
    }

    private void ingestFitData() {
        for (DataType dataType : Constants.FIT_DATA_TYPES) {
            ingestFitDataType(dataType);
        }
    }
}
