package com.sadharan.indoor_positioning.client;

import androidx.lifecycle.ViewModel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class PositioningNavigationViewModel extends ViewModel {
    public long block_id;
    public JSONArray datapoints;
    private LocalSurveyDatabase localSurveyDatabase;

    public void setLocalSurveyDatabase(LocalSurveyDatabase localSurveyDatabase) {
        this.localSurveyDatabase = localSurveyDatabase;
    }

    public void fetchDatapoints() throws JSONException {
        this.datapoints = localSurveyDatabase.getDatapoints(this.block_id);
        if(this.datapoints==null){
            throw new JSONException("Failed to fetch points");
        }
    }
}