package com.sjodle.splunkfit;

import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;

import java.util.concurrent.TimeUnit;

public class Constants {
    public static final int INGEST_JOB_ID = 504; // DO NOT CHANGE
    public static final int GOOGLE_FIT_PERMISSIONS_REQUEST_CODE = 42;
    public static final long INGEST_JOB_INTERVAL = TimeUnit.MINUTES.toMillis(15);
    public static final String PREFS_FILE_KEY = "com.sjodle.splunkfit.APPDATA";
    public static final String PREFS_CHECKPOINT_PREFIX = "checkpoint_";
    public static final String PREFS_HEC_URL_KEY = "hec_url";
    public static final String PREFS_HEC_TOKEN_KEY = "hec_token";
    public static final boolean ALLOW_UNTRUSTED_SSL = true;
    public static final DataType[] FIT_DATA_TYPES = {
            DataType.TYPE_STEP_COUNT_DELTA,
            DataType.TYPE_CALORIES_EXPENDED,
            DataType.TYPE_DISTANCE_DELTA,
            DataType.TYPE_ACTIVITY_SEGMENT
    };
    public static FitnessOptions getFitnessOptions() {
        FitnessOptions.Builder builder = FitnessOptions.builder();
        for (DataType dt : FIT_DATA_TYPES)
            builder.addDataType(dt);
        return builder.build();
    }
}
