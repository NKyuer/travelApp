package com.example.daath.travelApp;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.Poi;
import com.example.daath.travelApp.customClass.AppRestClient;
import com.example.daath.travelApp.customClass.FileOperation;
import com.example.daath.travelApp.customClass.Scene;
import com.example.daath.travelApp.customClass.SceneAdapter;
import com.example.daath.travelApp.customClass.User;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.Header;


public class SceneListFragment extends Fragment
        implements AdapterView.OnItemClickListener, PullToRefreshBase.OnRefreshListener2,
        TextView.OnEditorActionListener, View.OnClickListener,
        BDLocationListener{

    private View view;
    private EditText search;
    private ImageView searchBackHome, location;
//    private ListView sceneListView;
    private PullToRefreshListView sceneListView;
    private ProgressDialog progressDialog;

    private List<Scene> sceneList = new ArrayList<Scene>();
    private SceneAdapter sceneAdapter;
    private AppRestClient client = new AppRestClient();
    private int skip, limit;                //??????
    private int displayType = 0;                //??????????????????????????????1???????????????????????????????????????0????????????0

    // ??????????????????
    private LocationClient mLocationClient = null;
    private double latitude;
    private double longitude;


    public SceneListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(R.layout.fragment_scene_list, container, false);
            initViews();
            initPullToRefreshListView();
            Log.d("Tag_sec", "" + sceneList);

        }
//        if (sceneList.isEmpty()) {
//            sendHttpRequest();
//        }

        return view;
    }

    /**
     * ?????????load??????????????????????????????
     */
    public void getCurrentUserLocation() {
        FileOperation fileOperation = new FileOperation();
        String Data = fileOperation.fileLoad(getActivity());
        Log.d("Tag_Data", "" + Data);
        try {
            JSONObject result = new JSONObject(Data);
            JSONArray location = result.getJSONArray("location");
            longitude = location.getDouble(0);
            latitude = location.getDouble(1);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    public void initViews() {
        search = (EditText) view.findViewById(R.id.sceneList_search);
        searchBackHome = (ImageView) view.findViewById(R.id.sceneList_searchBackHome);
        location = (ImageView) view.findViewById(R.id.sceneList_location);
        search.setOnEditorActionListener(this);
        searchBackHome.setOnClickListener(this);
        location.setOnClickListener(this);
    }

    public void initPullToRefreshListView() {
        sceneListView = (PullToRefreshListView) view.findViewById(R.id.fragment_sceneListView);
        sceneListView.getLoadingLayoutProxy(false, true).setPullLabel("????????????");
        sceneListView.getLoadingLayoutProxy(true, false).setPullLabel("????????????");
        sceneListView.getLoadingLayoutProxy(true, true).setRefreshingLabel("?????????");
        sceneListView.getLoadingLayoutProxy(true, true).setReleaseLabel("????????????");
        sceneListView.setOnRefreshListener(this);
        sceneAdapter = new SceneAdapter(getActivity(), R.layout.fragment_scene_item, sceneList);
        sceneListView.setAdapter(sceneAdapter);
        sceneListView.setOnItemClickListener(this);
    }

    /**
     * ??????Http????????????????????????
     */
    private void sendHttpRequest() {
        client.saveCookie(getActivity());
        sceneList = new ArrayList<Scene>();
        RequestParams params = new RequestParams();
        skip = 0;
        limit = 5;
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("longitude", longitude);
        params.put("latitude", latitude);
        client.post("scene/lists/", params, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                JSONArray result = response.optJSONArray("result");
                for (int i=0; i<result.length(); i++) {
                    try {
                        JSONObject sceneJson = result.getJSONObject(i);
                        Scene scene = new Scene();
                        scene.set_id(sceneJson.getString("_id"));
                        scene.setName(sceneJson.getString("name"));
                        scene.setImage(sceneJson.getString("image"));
                        scene.setDescription(sceneJson.getString("description"));
                        sceneList.add(scene);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                sceneAdapter.setSceneList(sceneList);
                sceneAdapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * ????????????
     * @param context
     * @param view
     */
    private void hideInput(Context context,View view){
        InputMethodManager inputMethodManager =
                (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        getCurrentUserLocation();
        mLocationClient = new LocationClient(activity.getApplicationContext());
        mLocationClient.registerLocationListener(this);
        initLocation();
        initProgressDialog();
        mLocationClient.start();

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Scene scene = sceneList.get(position - 1);
        Log.d("Tag_sceneOnClick", "" + (position - 1) + "||" + scene.get_id());
        SceneDetailActivity.anotherActionStart(getActivity(), scene);
    }

    /**
     * PullToRefreshListView ????????????????????????????????????
     */
    @Override
    public void onPullDownToRefresh(PullToRefreshBase refreshView) {
        Log.d("Tag_refresh", "onPullDownToRefresh");
        RequestParams params = new RequestParams();
        params.put("skip", 0);
        params.put("limit", limit);
        params.put("longitude", longitude);
        params.put("latitude", latitude);
        sceneList = new ArrayList<Scene>();
        client.saveCookie(getActivity());
        switch (displayType) {
            case 0:                 //??????????????????
                client.post("scene/lists/", params, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        Log.d("Tag_onPullDownToRefresh", "" + response);
                        JSONArray result = response.optJSONArray("result");
                        for (int i=0; i<result.length(); i++) {
                            try {
                                JSONObject sceneJson = result.getJSONObject(i);
                                Scene scene = new Scene();
                                scene.set_id(sceneJson.getString("_id"));
                                scene.setName(sceneJson.getString("name"));
                                scene.setImage(sceneJson.getString("image"));
                                scene.setDescription(sceneJson.getString("description"));
                                sceneList.add(scene);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.d("Tag_sceneList", sceneList.toString());
                        sceneAdapter.setSceneList(sceneList);
                        sceneAdapter.notifyDataSetChanged();
                        sceneListView.onRefreshComplete();
                    }
                });
                break;
            case 1:                 //??????????????????
                if (search.length() == 0) {
                    break;
                }
                params.put("searchContent", search.getText().toString());
                client.post("scene/search/", params, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        Log.d("Tag_onPullDownToRefresh", "" + response);
                        JSONArray result = response.optJSONArray("result");
                        for (int i=0; i<result.length(); i++) {
                            try {
                                JSONObject sceneJson = result.getJSONObject(i);
                                Scene scene = new Scene();
                                scene.set_id(sceneJson.getString("_id"));
                                scene.setName(sceneJson.getString("name"));
                                scene.setImage(sceneJson.getString("image"));
                                scene.setDescription(sceneJson.getString("description"));
                                sceneList.add(scene);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.d("Tag_sceneList", sceneList.toString());
                        sceneAdapter.setSceneList(sceneList);
                        sceneAdapter.notifyDataSetChanged();
                        sceneListView.onRefreshComplete();
                    }
                });
                break;
            default: break;
        }

    }

    @Override
    public void onPullUpToRefresh(PullToRefreshBase refreshView) {
        Log.d("Tag_refresh", "onPullUpToRefresh");
        skip += 5;
        limit += 5;
        RequestParams params = new RequestParams();
        params.put("skip", skip);
        params.put("limit", limit);
        params.put("longitude", longitude);
        params.put("latitude", latitude);
        client.saveCookie(getActivity());
        switch (displayType) {
            case 0:
                client.post("scene/lists/", params, new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        Log.d("Tag_onPullUpToRefresh", "" + response);
                        JSONArray result = response.optJSONArray("result");
                        if (result.length() == 0) {
                            Toast.makeText(getActivity(), "??????????????????", Toast.LENGTH_SHORT).show();
                            sceneListView.onRefreshComplete();
                            skip -= 5;
                            limit -= 5;
                            return;
                        }
                        int i;
                        for (i=0; i<result.length(); i++) {
                            try {
                                JSONObject sceneJson = result.getJSONObject(i);
                                Scene scene = new Scene();
                                scene.set_id(sceneJson.getString("_id"));
                                scene.setName(sceneJson.getString("name"));
                                scene.setImage(sceneJson.getString("image"));
                                scene.setDescription(sceneJson.getString("description"));
                                sceneList.add(scene);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.d("Tag_sceneList", sceneList.toString());
                        sceneAdapter.setSceneList(sceneList);
                        sceneAdapter.notifyDataSetChanged();
                        sceneListView.onRefreshComplete();
                        limit = skip + i;
                    }
                });
                break;
            case 1:
                if (search.length() == 0) {
                    break;
                }
                params.put("searchContent", search.getText().toString());
                client.post("scene/search/", params, new JsonHttpResponseHandler() {

                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        Log.d("Tag_onPullUpToRefresh", "" + response);
                        JSONArray result = response.optJSONArray("result");
                        if (result.length() == 0) {
                            Toast.makeText(getActivity(), "??????????????????", Toast.LENGTH_SHORT).show();
                            sceneListView.onRefreshComplete();
                            skip -= 5;
                            limit -= 5;
                            return;
                        }
                        int i;
                        for (i=0; i<result.length(); i++) {
                            try {
                                JSONObject sceneJson = result.getJSONObject(i);
                                Scene scene = new Scene();
                                scene.set_id(sceneJson.getString("_id"));
                                scene.setName(sceneJson.getString("name"));
                                scene.setImage(sceneJson.getString("image"));
                                scene.setDescription(sceneJson.getString("description"));
                                sceneList.add(scene);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        Log.d("Tag_sceneList", sceneList.toString());
                        sceneAdapter.setSceneList(sceneList);
                        sceneAdapter.notifyDataSetChanged();
                        sceneListView.onRefreshComplete();
                        limit = skip + i;
                    }
                });
                break;
            default: break;
        }

    }

    /**
     * ??????????????????????????????
     * @param v
     * @param actionId
     * @param event
     * @return
     */
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (search.length() == 0) {
            return false;
        }
        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            hideInput(getActivity(), search);
            sceneList = new ArrayList<Scene>();
            skip = 0;               //????????????????????????????????????
            limit = 5;
            client.saveCookie(getActivity());
            RequestParams params = new RequestParams();
            params.put("searchContent", search.getText().toString());
            params.put("skip", skip);
            params.put("limit", limit);
            params.put("longitude", longitude);
            params.put("latitude", latitude);
            client.post("scene/search/", params, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    JSONArray result = response.optJSONArray("result");
                    for (int i=0; i<result.length(); i++) {
                        try {
                            JSONObject sceneJson = result.getJSONObject(i);
                            Scene scene = new Scene();
                            scene.set_id(sceneJson.getString("_id"));
                            scene.setName(sceneJson.getString("name"));
                            scene.setImage(sceneJson.getString("image"));
                            scene.setDescription(sceneJson.getString("description"));
                            sceneList.add(scene);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    sceneAdapter.setSceneList(sceneList);
                    sceneAdapter.notifyDataSetChanged();
                    displayType = 1;
                    searchBackHome.setVisibility(View.VISIBLE);
                }
            });
        }
        return false;
    }

    /**
     * ??????????????????????????????????????????
     * @param v
     */
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sceneList_searchBackHome:
                displayType = 0;
                searchBackHome.setVisibility(View.GONE);
                sendHttpRequest();
                search.setText("");
                break;
            case R.id.sceneList_location:
                if (mLocationClient.isStarted()){
                    mLocationClient.stop();
                }
                initProgressDialog();       // ???????????????
                mLocationClient.start();
                break;
            default: break;
        }
    }

    /**
     * ????????????
     */
    public void initProgressDialog() {
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setTitle("???????????????..");
        progressDialog.setMessage("?????????");
        progressDialog.setCancelable(true);
        progressDialog.show();
    }

    /**
     * ??????????????????
     * @param message
     */
    public void initAlertDialog(String message, final Double lat, final Double lon) {
        progressDialog.dismiss();
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
        dialog.setTitle("??????");
        dialog.setMessage(message);
        dialog.setCancelable(false);
        dialog.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                latitude = lat;
                longitude = lon;
                sendHttpRequest();
                AppRestClient client = new AppRestClient();
                client.saveCookie(getActivity());
                RequestParams params = new RequestParams();
                params.put("longitude", longitude);
                params.put("latitude", latitude);
                client.post("user/updateLocation/", params, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                        try {
                            JSONObject result = response.getJSONObject("result");
                            FileOperation fileOperation = new FileOperation();
                            fileOperation.fileSave(getActivity(), result.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                });

            }
        });
        dialog.setNeutralButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendHttpRequest();
            }
        });
        dialog.show();

    }

    public void initLocation(){
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Battery_Saving
        );//?????????????????????????????????????????????????????????????????????????????????
        option.setCoorType("bd09ll");//???????????????gcj02???????????????????????????????????????
        int span=0;
        option.setScanSpan(span);//???????????????0???????????????????????????????????????????????????????????????????????????1000ms???????????????
        option.setIsNeedAddress(true);//?????????????????????????????????????????????????????????
        option.setOpenGps(true);//???????????????false,??????????????????gps
        option.setLocationNotify(true);//???????????????false??????????????????gps???????????????1S1???????????????GPS??????
        option.setIsNeedLocationDescribe(true);//???????????????false??????????????????????????????????????????????????????BDLocation.getLocationDescribe?????????????????????????????????????????????????????????
        option.setIsNeedLocationPoiList(true);//???????????????false?????????????????????POI??????????????????BDLocation.getPoiList?????????
        option.setIgnoreKillProcess(false);//???????????????true?????????SDK???????????????SERVICE?????????????????????????????????????????????stop?????????????????????????????????????????????
        option.SetIgnoreCacheException(false);//???????????????false?????????????????????CRASH?????????????????????
        option.setEnableSimulateGps(false);//???????????????false???????????????????????????gps???????????????????????????
        mLocationClient.setLocOption(option);
    }


    /**
     * ????????????????????????
     * @param bdLocation
     */
    @Override
    public void onReceiveLocation(BDLocation bdLocation) {
        //Receive Location
        initAlertDialog(bdLocation.getLocationDescribe().substring(1), bdLocation.getLatitude(), bdLocation.getLongitude());
    }
}
