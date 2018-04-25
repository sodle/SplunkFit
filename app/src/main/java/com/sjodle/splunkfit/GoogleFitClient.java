package com.sjodle.splunkfit;

import android.content.Context;
import android.util.LongSparseArray;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.json.JSONArray;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class GoogleFitClient {
    private Context context;

    GoogleFitClient(Context context) {
        this.context = context;
    }

    public boolean checkPermissions() {
        return GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(context), Constants.getFitnessOptions());
    }

    public LongSparseArray<JSONArray> getDataPoints(DataType dataType, long startTime, long endTime) {
        LongSparseArray<JSONArray> dataPoints = new LongSparseArray<>();
        DataReadRequest request = new DataReadRequest.Builder().
                read(dataType).setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
        Task<DataReadResponse> response = Fitness.getHistoryClient(context,
                GoogleSignIn.getLastSignedInAccount(context)).readData(request);
        try {
            DataReadResponse result = Tasks.await(response);
            DataSet dataSet = result.getDataSet(dataType);
            GoogleFitDataPointFormatter.BaseDataPointFormatter formatter = GoogleFitDataPointFormatter.getFormatter(dataType);
            for (DataPoint dataPoint : dataSet.getDataPoints()) {
                dataPoints.put(dataPoint.getTimestamp(TimeUnit.MILLISECONDS), formatter.format(dataPoint));
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return dataPoints;
    }
}
