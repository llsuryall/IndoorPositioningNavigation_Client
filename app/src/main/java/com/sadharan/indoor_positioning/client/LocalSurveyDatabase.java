package com.sadharan.indoor_positioning.client;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class LocalSurveyDatabase { //extends SQLiteOpenHelper {
    private static final String BASE_URL = "https://indoor-positioning-navigation.herokuapp.com/api/v1.0";

    public ArrayList<BlockElement> getBlockList(long building_id) throws JSONException {
        if (building_id <= 0) {
            return null;
        }
        JSONObject request = new JSONObject();
        request.put("BuildingID", building_id);
        JSONObject response = sendPost(request, "/blocks/all_from");
        ArrayList<BlockElement> blocks = new ArrayList<>();
        if (response.getBoolean("successful")) {
            JSONArray blocks_data = response.getJSONArray("data");
            JSONObject block_data;
            for (int i = 0; i < blocks_data.length(); i++) {
                block_data = blocks_data.getJSONObject(i);
                blocks.add(new BlockElement(block_data.getLong("BlockID"),block_data.getLong("BuildingID"), block_data.getString("BlockName"), (float) block_data.getDouble("BlockLatitude"), (float) block_data.getDouble("BlockLongitude")));
            }
            return blocks;
        } else {
            return null;
        }
    }

    public ArrayList<BuildingElement> searchBuildings(String searchString) throws JSONException {
        if (searchString == null) {
            return null;
        }
        JSONObject request = new JSONObject();
        request.put("SearchString", searchString);
        JSONObject response = sendPost(request, "/buildings/search");
        ArrayList<BuildingElement> buildings = new ArrayList<>();
        if (response.getBoolean("successful")) {
            JSONArray buildings_data = response.getJSONArray("data");
            JSONObject building_data;
            for (int i = 0; i < buildings_data.length(); i++) {
                building_data = buildings_data.getJSONObject(i);
                buildings.add(new BuildingElement(building_data.getLong("BuildingID"), building_data.getString("BuildingName"), building_data.getString("BuildingAddress")));
            }
            return buildings;
        } else {
            return null;
        }
    }

    public JSONArray getDatapoints(long block_id) throws JSONException {
        if (block_id <= 0) {
            return null;
        }
        JSONObject request = new JSONObject();
        request.put("BlockID", block_id);
        JSONObject response = sendPost(request, "/datapoints/all_from");
        if (response.getBoolean("successful")) {
            return response.getJSONArray("data");
        } else {
            return null;
        }
    }

    private static class RequestThread extends Thread {
        private final JSONObject requestData;
        private final String url;
        public JSONObject response;

        RequestThread(JSONObject requestData, String url) {
            this.requestData = requestData;
            this.url = url;
        }

        @Override
        public void run() {
            try {
                this.sendPost();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void sendPost() throws JSONException {
            HttpsURLConnection HttpsURLConnection;
            OutputStream requestStream;
            InputStream responseStream;
            try {
                HttpsURLConnection = (HttpsURLConnection) new URL(BASE_URL + url).openConnection();
                byte[] requestDataBytes = requestData.toString().getBytes(StandardCharsets.UTF_8);
                HttpsURLConnection.setChunkedStreamingMode(0);
                HttpsURLConnection.setRequestMethod("POST");
                HttpsURLConnection.setRequestProperty("Content-Type", "application/json");
                requestStream = new BufferedOutputStream(HttpsURLConnection.getOutputStream());
                requestStream.write(requestDataBytes);
                requestStream.close();
                responseStream = new BufferedInputStream(HttpsURLConnection.getInputStream());
                StringBuilder responseStringBuilder = new StringBuilder(1024);
                byte[] buf = new byte[8];
                while (responseStream.read(buf)> 0) {
                    responseStringBuilder.append(new String(buf));
                }
                String resp_string=responseStringBuilder.toString();
                response = new JSONObject(resp_string);
                responseStream.close();
            } catch (Exception e) {
                e.printStackTrace();
                response = new JSONObject();
                response.put("Error", e.toString());
            }
        }
    }

    public JSONObject sendPost(JSONObject requestData, String url) {
        RequestThread requestThread = new RequestThread(requestData, url);
        requestThread.start();
        JSONObject response = null;
        try {
            requestThread.join();
            response = requestThread.response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }
}
