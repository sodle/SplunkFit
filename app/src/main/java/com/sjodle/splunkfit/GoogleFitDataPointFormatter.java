package com.sjodle.splunkfit;

import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class GoogleFitDataPointFormatter {
    interface BaseDataPointFormatter {
        JSONArray format(DataPoint dataPoint);
    }

    public static BaseDataPointFormatter getFormatter(DataType dataType) {
        return formatterMap.get(dataType);
    }

    private static float getGMTTimestamp(long localTimestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(localTimestamp);
        Calendar utc = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        utc.setTimeInMillis(cal.getTimeInMillis());
        return utc.getTimeInMillis();
    }

    private static String formatTimestamp(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US);
        return formatter.format(cal.getTime());
    }

    private static HashMap<DataType, BaseDataPointFormatter> formatterMap = new HashMap<>();
    static {
        formatterMap.put(DataType.TYPE_STEP_COUNT_DELTA, new BaseDataPointFormatter() {
            @Override
            public JSONArray format(DataPoint dataPoint) {
                JSONArray events = new JSONArray();
                float fullDuration = (dataPoint.getEndTime(TimeUnit.MILLISECONDS) - dataPoint.getStartTime(TimeUnit.MILLISECONDS));
                int fullStepCount = dataPoint.getValue(Field.FIELD_STEPS).asInt();
                for (long t = dataPoint.getStartTime(TimeUnit.MILLISECONDS); t < dataPoint.getEndTime(TimeUnit.MILLISECONDS); t += 60000) {
                    try {
                        float duration = ((dataPoint.getEndTime(TimeUnit.MILLISECONDS) - t) < 60000) ? (dataPoint.getEndTime(TimeUnit.MILLISECONDS) - t) : 60000;
                        float endTime = t + duration;
                        float interpolationConstant = duration / fullDuration;
                        int interpolatedSteps = (int) interpolationConstant * fullStepCount;
                        JSONObject event = new JSONObject();
                        event.put("sourcetype", "googlefit:steps");
                        event.put("source", dataPoint.getDataSource());
                        event.put("host", "fit.google.com");
                        event.put("time", getGMTTimestamp((long) endTime / 1000));
                        JSONObject body = new JSONObject();
                        body.put("steps", interpolatedSteps);
                        body.put("startTime", formatTimestamp(t));
                        body.put("endTime", formatTimestamp((long) endTime));
                        body.put("durationSec", duration / 1000);
                        event.put("event", body);
                        events.put(event);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                return events;
            }
        });
        formatterMap.put(DataType.TYPE_CALORIES_EXPENDED, new BaseDataPointFormatter() {
            @Override
            public JSONArray format(DataPoint dataPoint) {
                JSONArray events = new JSONArray();
                float fullDuration = (dataPoint.getEndTime(TimeUnit.MILLISECONDS) - dataPoint.getStartTime(TimeUnit.MILLISECONDS));
                float fullCalCount = dataPoint.getValue(Field.FIELD_CALORIES).asFloat();
                for (long t = dataPoint.getStartTime(TimeUnit.MILLISECONDS); t < dataPoint.getEndTime(TimeUnit.MILLISECONDS); t += 60000) {
                    try {
                        float duration = ((dataPoint.getEndTime(TimeUnit.MILLISECONDS) - t) < 60000) ? (dataPoint.getEndTime(TimeUnit.MILLISECONDS) - t) : 60000;
                        float endTime = t + duration;
                        float interpolationConstant = duration / fullDuration;
                        float interpolatedCals = interpolationConstant * fullCalCount;
                        JSONObject event = new JSONObject();
                        event.put("sourcetype", "googlefit:calories");
                        event.put("source", dataPoint.getDataSource());
                        event.put("host", "fit.google.com");
                        event.put("time", getGMTTimestamp((long) endTime / 1000));
                        JSONObject body = new JSONObject();
                        body.put("kcal", interpolatedCals);
                        body.put("startTime", formatTimestamp(t));
                        body.put("endTime", formatTimestamp((long) endTime));
                        body.put("durationSec", duration / 1000);
                        event.put("event", body);
                        events.put(event);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                return events;
            }
        });
        formatterMap.put(DataType.TYPE_DISTANCE_DELTA, new BaseDataPointFormatter() {
            @Override
            public JSONArray format(DataPoint dataPoint) {
                JSONArray events = new JSONArray();
                float fullDuration = (dataPoint.getEndTime(TimeUnit.MILLISECONDS) - dataPoint.getStartTime(TimeUnit.MILLISECONDS));
                float fullDistance = dataPoint.getValue(Field.FIELD_DISTANCE).asFloat();
                for (long t = dataPoint.getStartTime(TimeUnit.MILLISECONDS); t < dataPoint.getEndTime(TimeUnit.MILLISECONDS); t += 60000) {
                    try {
                        float duration = ((dataPoint.getEndTime(TimeUnit.MILLISECONDS) - t) < 60000) ? (dataPoint.getEndTime(TimeUnit.MILLISECONDS) - t) : 60000;
                        float endTime = t + duration;
                        float interpolationConstant = duration / fullDuration;
                        float interpolatedDistance = interpolationConstant * fullDistance;
                        JSONObject event = new JSONObject();
                        event.put("sourcetype", "googlefit:distance");
                        event.put("source", dataPoint.getDataSource());
                        event.put("host", "fit.google.com");
                        event.put("time", getGMTTimestamp((long) endTime / 1000));
                        JSONObject body = new JSONObject();
                        body.put("meters", interpolatedDistance);
                        body.put("startTime", formatTimestamp(t));
                        body.put("endTime", formatTimestamp((long) endTime));
                        body.put("durationSec", duration / 1000);
                        event.put("event", body);
                        events.put(event);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                return events;
            }
        });
        formatterMap.put(DataType.TYPE_ACTIVITY_SEGMENT, new BaseDataPointFormatter() {
            @Override
            public JSONArray format(DataPoint dataPoint) {
                JSONArray events = new JSONArray();
                String activity = dataPoint.getValue(Field.FIELD_ACTIVITY).asActivity();
                for (long t = dataPoint.getStartTime(TimeUnit.MILLISECONDS); t < dataPoint.getEndTime(TimeUnit.MILLISECONDS); t += 60000) {
                    try {
                        float duration = ((dataPoint.getEndTime(TimeUnit.MILLISECONDS) - t) < 60000) ? (dataPoint.getEndTime(TimeUnit.MILLISECONDS) - t) : 60000;
                        float endTime = t + duration;
                        JSONObject event = new JSONObject();
                        event.put("sourcetype", "googlefit:activity");
                        event.put("source", dataPoint.getDataSource());
                        event.put("host", "fit.google.com");
                        event.put("time", getGMTTimestamp((long) endTime / 1000));
                        JSONObject body = new JSONObject();
                        body.put("activity", activity);
                        body.put("startTime", formatTimestamp(t));
                        body.put("endTime", formatTimestamp((long) endTime));
                        body.put("durationSec", duration / 1000);
                        event.put("event", body);
                        events.put(event);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                return events;
            }
        });
    }
}
