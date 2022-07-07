package com.sadharan.indoor_positioning.client;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class BuildingSelectActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText buildingSearchString;
    private BuildingSelectViewModel buildingSelectViewModel;
    private LinearLayout buildingListHolder;
    private LocalSurveyDatabase localSurveyDatabase;
    private LayoutInflater layoutInflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
    }

    public void initialize() {
        setContentView(R.layout.building_select_activity);
        this.localSurveyDatabase = new LocalSurveyDatabase();
        this.layoutInflater = LayoutInflater.from(BuildingSelectActivity.this);
        this.buildingSelectViewModel = new ViewModelProvider(this).get(BuildingSelectViewModel.class);
        this.buildingListHolder = findViewById(R.id.buildingListHolder);
        this.buildingSearchString = findViewById(R.id.buildingSearchString);
        findViewById(R.id.buildingSearchButton).setOnClickListener(this);
        refreshCatalogueView();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.buildingSearchButton) {
            String searchString = this.buildingSearchString.getText().toString();
            if (searchString.length() > 0) {
                try {
                    this.buildingSelectViewModel.buildingElements = this.localSurveyDatabase.searchBuildings(searchString);
                    refreshCatalogueView();
                    Toast.makeText(getApplicationContext(), R.string.search_successful, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), R.string.search_failure, Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            long building_id = Long.parseLong(((TextView) view.findViewById(R.id.building_id)).getText().toString());
            Intent blockCatalogueActivity = new Intent(this, BlockSelectActivity.class);
            blockCatalogueActivity.putExtra(getString(R.string.building_id_field), building_id);
            startActivity(blockCatalogueActivity);
        }
    }

    private void refreshCatalogueView() {
        this.buildingListHolder.removeAllViews();
        if (this.buildingSelectViewModel.buildingElements != null) {
            for (BuildingElement buildingElement : this.buildingSelectViewModel.buildingElements) {
                addBuildingElement(buildingElement);
            }
        }
    }

    private void addBuildingElement(BuildingElement buildingElement) {
        LinearLayout buildingElementView = (LinearLayout) this.layoutInflater.inflate(R.layout.building_element, buildingListHolder, false);
        buildingElementView.setOnClickListener(this);
        String building_id_string = Long.toString(buildingElement.id);
        ((TextView) buildingElementView.findViewById(R.id.building_id)).setText(building_id_string);
        ((TextView) buildingElementView.findViewById(R.id.building_name)).setText(buildingElement.name);
        ((TextView) buildingElementView.findViewById(R.id.building_address)).setText(buildingElement.address);
        buildingListHolder.addView(buildingElementView);
    }
}
