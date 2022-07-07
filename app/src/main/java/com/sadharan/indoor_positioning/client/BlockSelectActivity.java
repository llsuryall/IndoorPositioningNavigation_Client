package com.sadharan.indoor_positioning.client;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class BlockSelectActivity extends AppCompatActivity implements View.OnClickListener {
    private BlockSelectViewModel blockSelectViewModel;
    private LayoutInflater layoutInflater;
    private LinearLayout blockElementsHolder;

    @SuppressLint("InflateParams")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initialize();
    }

    private void initialize() {
        setContentView(R.layout.block_select_activity);
        LocalSurveyDatabase localSurveyDatabase = new LocalSurveyDatabase();
        this.blockSelectViewModel = new ViewModelProvider(this).get(BlockSelectViewModel.class);
        this.layoutInflater = LayoutInflater.from(BlockSelectActivity.this);
        this.blockElementsHolder = findViewById(R.id.block_elements_holder);
        this.blockSelectViewModel.setLocalSurveyDatabase(localSurveyDatabase);
        if (getIntent().getLongExtra(getString(R.string.building_id_field), -1) >= 0) {
            this.blockSelectViewModel.building_id = getIntent().getLongExtra(getString(R.string.building_id_field), -1);
        }
        if (blockSelectViewModel.blockElements == null) {
            try {
                blockSelectViewModel.updateCatalogue();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        refreshCatalogueView();
    }

    @Override
    public void onClick(View view) {
        long block_id = Long.parseLong(((TextView) view.findViewById(R.id.block_id)).getText().toString());
        Intent positioningNavigationActivity = new Intent(this, PositioningNavigationActivity.class);
        positioningNavigationActivity.putExtra(getString(R.string.block_id_field), block_id);
        startActivity(positioningNavigationActivity);
    }

    private void refreshCatalogueView() {
        blockElementsHolder.removeAllViews();
        for (BlockElement blockElement : this.blockSelectViewModel.blockElements) {
            addBlockElement(blockElement);
        }
    }

    private void addBlockElement(BlockElement blockElement) {
        LinearLayout blockElementView = (LinearLayout) this.layoutInflater.inflate(R.layout.block_element, blockElementsHolder, false);
        String block_id_string = Long.toString(blockElement.id);
        String block_coordinates = blockElement.latitude + "," + blockElement.longitude;
        blockElementView.setOnClickListener(this);
        ((TextView) blockElementView.findViewById(R.id.block_id)).setText(block_id_string);
        ((TextView) blockElementView.findViewById(R.id.block_name)).setText(blockElement.name);
        ((TextView) blockElementView.findViewById(R.id.block_coordinates)).setText(block_coordinates);
        blockElementsHolder.addView(blockElementView);
    }
}
