package com.sadharan.indoor_positioning.client;

import androidx.lifecycle.ViewModel;

import org.json.JSONException;

import java.util.ArrayList;

public class BlockSelectViewModel extends ViewModel {
    public long building_id;
    ArrayList<BlockElement> blockElements;
    private LocalSurveyDatabase localSurveyDatabase;

    public void setLocalSurveyDatabase(LocalSurveyDatabase localSurveyDatabase) {
        this.localSurveyDatabase = localSurveyDatabase;
    }

    public void updateCatalogue() throws JSONException {
        this.blockElements = localSurveyDatabase.getBlockList(this.building_id);
    }
}
