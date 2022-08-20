package com.map.maptest;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

public class MainActivity extends AppCompatActivity {
    private MapView mMapView = null;
    private RadioGroup mapType;
    private RadioButton normalBtn;
    private RadioButton satelliteBtn;
    private BaiduMap mBaiduMap;
    private LocationClient mLocationClient;

    private BitmapDescriptor bitmap;//对于标的点的图标，经纬度和重置按钮
    private ImageButton ibLocation;
    private Marker marker;
    private double markerLatitude = 0;
    private double markerLongitude = 0;
    boolean isFirstLoc = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //获取地图控件引用
        mMapView = (MapView) findViewById(R.id.bmapView);
        mapType = findViewById(R.id.id_rp_mapType);
        normalBtn = findViewById(R.id.id_btn_normal);
        satelliteBtn = findViewById(R.id.id_btn_satellite);
        initEvent();//地图的切换与初始化
        mapOnClick();//地图点击？？这个功能有待商榷
    }

    private void initEvent() {
        mapType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (i == normalBtn.getId()) {
                    mMapView=(MapView)findViewById(R.id.bmapView);
                    mMapView.getMap().setMapType(BaiduMap.MAP_TYPE_NORMAL);
                } else if (i == satelliteBtn.getId()) {
                    mMapView=(MapView)findViewById(R.id.bmapView);
                    mMapView.getMap().setMapType(BaiduMap.MAP_TYPE_SATELLITE);

                }
                mMapView.showScaleControl(true);
                mMapView.showZoomControls(true);
                mMapView.removeViewAt(1);
                mBaiduMap = mMapView.getMap();

                mBaiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        final String info =(String) marker.getExtraInfo().get("info");
                        Toast.makeText(MainActivity.this,info,Toast.LENGTH_SHORT).show();
                        return false;
                    }
                });

            }
        });

    }

    private void mapOnClick(){
        //设置图标
        bitmap = BitmapDescriptorFactory.fromResource(R.drawable.ic_baseline_brightness_1_24);
        mBaiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                //获取经纬度
                markerLatitude = latLng.latitude;
                markerLongitude = latLng.longitude;
                //清楚图层
                mBaiduMap.clear();
                //定义Marker坐标点
                LatLng point = new LatLng(markerLatitude,markerLongitude);
                //建立MarkerOption，在地图上添加并显示Marker
                MarkerOptions options = new MarkerOptions().position(point).icon(bitmap);
                marker = (Marker) mBaiduMap.addOverlay(options);
                Bundle bundle = new Bundle();
                bundle.putSerializable("info","纬度："+markerLatitude+"经度"+markerLongitude);
                marker.setExtraInfo(bundle);

                //地图重新定位
                initLocation();
            }

            @Override
            public void onMapPoiClick(MapPoi mapPoi) {

            }
        });
    }




    private void initLocation() {
        //开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        //定位初始化
        mLocationClient = new LocationClient(getApplicationContext());


        //通过LocationClientOption设置LocationClient相关参数
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);//设置高精度定位
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(1000);
        //设置locationClientOption
        mLocationClient.setLocOption(option);
        //注册LocationListener监听器
        MyLocationListener myLocationListener = new MyLocationListener();
        mLocationClient.registerLocationListener(myLocationListener);
        //开启地图定位图层
        mLocationClient.start();

    }

    public void resetLocation(View view){
        //当点切换位置，重置定位显示，点击回到自动定位
        markerLatitude = 0;
        initLocation();
        marker.remove();
    }

    public class MyLocationListener extends BDAbstractLocationListener {
        @Override
        //定位SDK监听函数
        public void onReceiveLocation(BDLocation location) {
            Toast.makeText(MainActivity.this,location.getAddrStr(),Toast.LENGTH_SHORT).show();
            //mapView 销毁后不在处理新接收的位置
            if (location == null || mMapView == null) {
                return;
            }
            double resultLatitude;
            double resultLongitude;

            if(markerLatitude == 0){
                //自动定位
                resultLongitude = location.getLongitude();
                resultLatitude = location.getLatitude();
                ibLocation.setVisibility(View.VISIBLE);
            }else{
                //标点定位
                resultLongitude = markerLongitude;
                resultLatitude = markerLatitude;
                ibLocation.setVisibility(View.VISIBLE);
            }

            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                    // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(location.getDirection()).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);//设置定位数据，线定位图层之后设置定位数据
            LatLng latLng = new LatLng(resultLatitude,resultLongitude);
            MapStatus.Builder builder = new MapStatus.Builder();
            builder.target(latLng).zoom(20.0f);
            mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mLocationClient.stop();
        //关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
    }
}

