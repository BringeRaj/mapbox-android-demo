package com.mapbox.mapboxandroiddemo.labs;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxandroiddemo.R;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.style.layers.Layer;
import com.mapbox.mapboxsdk.style.layers.SymbolLayer;
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource;

import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAllowOverlap;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconAnchor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconImage;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.iconOffset;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;

public class InfoWindowSymbolLayerActivity extends AppCompatActivity implements
  OnMapReadyCallback, MapboxMap.OnMapClickListener {

  private MapView mapView;
  private MapboxMap mapboxMap;
  private boolean markerSelected = false;
  private FeatureCollection mapLocationFeatureCollection;
  private HashMap<String, View> viewMap;
  private GeoJsonSource mapLocationsGeoJsonSource;
  private String GEOJSON_SOURCE_ID = "GEOJSON_SOURCE_ID";
  private String MARKER_ICON_LOCATION_LAYER_ID = "MARKER_ICON_LOCATION_LAYER_ID";
  private String FEATURE_TITLE_PROPERTY_KEY = "FEATURE_TITLE_PROPERTY_KEY";
  private String FEATURE_DESCRIPTION_PROPERTY_KEY = "FEATURE_DESCRIPTION_PROPERTY_KEY";
  private String TAG = "InfoWindowSymbolLayerActivity";
  private AnimatorSet animatorSet;

  private static final long CAMERA_ANIMATION_TIME = 1950;


  private static final String MARKER_IMAGE_ID = "MARKER_IMAGE_ID";
  private static final String MARKER_LAYER_ID = "MARKER_LAYER_ID";
  private static final String LOADING_LAYER_ID = "mapbox.poi.loading";
  private static final String CALLOUT_LAYER_ID = "mapbox.poi.callout";

  private static final String PROPERTY_SELECTED = "selected";
  private static final String PROPERTY_TITLE = "title";
  private static final String PROPERTY_DESCRIPTION = "description";

  private GeoJsonSource source;
  private FeatureCollection featureCollection;

  @ActivityStep
  private int currentStep;

  @Retention(RetentionPolicy.SOURCE)
  @IntDef( {STEP_INITIAL, STEP_LOADING, STEP_READY})
  public @interface ActivityStep {
  }

  private static final int STEP_INITIAL = 0;
  private static final int STEP_LOADING = 1;
  private static final int STEP_READY = 2;

  private static final Map<Integer, Double> stepZoomMap = new HashMap<>();

  static {
    stepZoomMap.put(STEP_INITIAL, 11.0);
    stepZoomMap.put(STEP_LOADING, 13.5);
    stepZoomMap.put(STEP_READY, 18.0);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Mapbox access token is configured here. This needs to be called either in your application
    // object or in the same activity which contains the mapview.
    Mapbox.getInstance(this, getString(R.string.access_token));

    // This contains the MapView in XML and needs to be called after the access token is configured.
    setContentView(R.layout.activity_info_window_symbol_layer);

    // Initialize the map view
    mapView = findViewById(R.id.mapView);
    mapView.onCreate(savedInstanceState);
    mapView.getMapAsync(this);
  }

  @Override
  public void onMapReady(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
/*
    this.mapboxMap = mapboxMap;

    // Create list of Feature objects
    List<Feature> mapLocationCoordinates = new ArrayList<>();

    // Create a single Feature location in Caracas, Venezuela
    Feature singleFeature = Feature.fromGeometry(Point.fromLngLat(
      -66.910519, 10.503250));

    // Add a String property to the Feature to be used in the title of the popup bubble window
    singleFeature.addStringProperty(FEATURE_TITLE_PROPERTY_KEY, "Hello World!");
    singleFeature.addStringProperty(FEATURE_DESCRIPTION_PROPERTY_KEY, "Welcome to my marker");

    // Add the Feature to the List<> of Feature objects
    mapLocationCoordinates.add(singleFeature);

    // Add the list as a parameter to create a FeatureCollection
    mapLocationFeatureCollection = FeatureCollection.fromFeatures(mapLocationCoordinates);

    // Create a GeoJSON source with a unique ID and a FeatureCollection
    mapLocationsGeoJsonSource = new GeoJsonSource(GEOJSON_SOURCE_ID, mapLocationFeatureCollection);

    // Add the GeoJSON source to the map
    mapboxMap.addSource(mapLocationsGeoJsonSource);

    // Create a bitmap that will serve as the visual marker icon image
    Bitmap redMarkerIcon = BitmapFactory.decodeResource(
      InfoWindowSymbolLayerActivity.this.getResources(), R.drawable.red_marker);

    // Add the marker icon image to the map
    mapboxMap.addImage(MARKER_LAYER_ID, redMarkerIcon);

    // Create a SymbolLayer with a unique id and a source. In this case, it's the GeoJSON source
    // that was created above. The red marker icon is added to the layer using run-time styling.
    SymbolLayer mapLocationSymbolLayer = new SymbolLayer(MARKER_ICON_LOCATION_LAYER_ID, GEOJSON_SOURCE_ID)
      .withProperties(iconImage(MARKER_LAYER_ID));
    mapboxMap.addLayer(mapLocationSymbolLayer);

    // Start the async task that creates the actual popup bubble window. This window will appear once a
    // SymbolLayer icon is tapped on.
    new GenerateViewIconTask(this).execute(mapLocationFeatureCollection);

    // Initialize the map click listener
    mapboxMap.addOnMapClickListener(this);*/


    new LoadPoiDataTask(this).execute();
    mapboxMap.addOnMapClickListener(this);
  }

  @Override
  public void onMapClick(@NonNull LatLng point) {
    PointF screenPoint = mapboxMap.getProjection().toScreenLocation(point);

    handleClickIcon(screenPoint);
  }

  public void setupData(final FeatureCollection collection) {
    if (mapboxMap == null) {
      return;
    }

    featureCollection = collection;
    setupSource();
    setUpImage();
    setupMarkerLayer();
    setupCalloutLayer();
    hideLabelLayers();
  }

  private void setupSource() {
    source = new GeoJsonSource(GEOJSON_SOURCE_ID, featureCollection);
    mapboxMap.addSource(source);
  }

  private void setUpImage() {
    Bitmap icon = BitmapFactory.decodeResource(
      this.getResources(), R.drawable.red_marker);
    mapboxMap.addImage(MARKER_IMAGE_ID, icon);
  }

  private void refreshSource() {
    if (source != null && featureCollection != null) {
      source.setGeoJson(featureCollection);
    }
  }

  /**
   * Setup a layer with maki icons, eg. restaurant.
   */
  private void setupMarkerLayer() {
    mapboxMap.addLayer(new SymbolLayer(MARKER_LAYER_ID, GEOJSON_SOURCE_ID)
      .withProperties(
        iconImage(MARKER_IMAGE_ID),
        iconAllowOverlap(true)
      ));
  }

  /**
   * Setup a layer with Android SDK call-outs
   * <p>
   * title of the feature is used as key for the iconImage
   * </p>
   */
  private void setupCalloutLayer() {
    mapboxMap.addLayer(new SymbolLayer(CALLOUT_LAYER_ID, GEOJSON_SOURCE_ID)
      .withProperties(
        /* show image with id title based on the value of the title feature property */
        iconImage("{title}"),

        /* set anchor of icon to bottom-left */
        iconAnchor("bottom-left"),

        iconAllowOverlap(true),

        /* offset icon slightly to match bubble layout */
        iconOffset(new Float[] {-20.0f, -10.0f})
      )

      /* add a filter to show only when selected feature property is true */
      .withFilter(eq((get(PROPERTY_SELECTED)), literal(true))));
  }

  private void hideLabelLayers() {
    String id;
    for (Layer layer : mapboxMap.getLayers()) {
      id = layer.getId();
      if (id.startsWith("place") || id.startsWith("poi") || id.startsWith("marine") || id.startsWith("road-label")) {
        layer.setProperties(visibility("none"));
      }
    }
  }

  /**
   * This method handles click events for maki symbols.
   * <p>
   * When a maki symbol is clicked, we moved that feature to the selected state.
   * </p>
   *
   * @param screenPoint the point on screen clicked
   */
  private void handleClickIcon(PointF screenPoint) {
    Log.d(TAG, "handleClickIcon: ");
    List<Feature> features = mapboxMap.queryRenderedFeatures(screenPoint, MARKER_LAYER_ID);
    if (!features.isEmpty()) {
      Log.d(TAG, "handleClickIcon: onclick !features.isEmpty()");
      String title = features.get(0).getStringProperty(PROPERTY_TITLE);
      Log.d(TAG, "handleClickIcon: title = " + title);
      List<Feature> featureList = featureCollection.features();
      Log.d(TAG, "handleClickIcon: featureList size = " + featureList.size());
      for (int i = 0; i < featureList.size(); i++) {
        Log.d(TAG, "handleClickIcon: i = " + i);
        if (featureList.get(i).getStringProperty(PROPERTY_TITLE).equals(title)) {
          Log.d(TAG, "handleClickIcon:  running setSelected(i);");

          setSelected(i);
        }
      }
    }
  }

  /**
   * Set a feature selected state with the ability to scroll the RecycleViewer to the provided index.
   *
   * @param index the index of selected feature
   */
  private void setSelected(int index) {

    deselectAll(false);

    Feature feature = featureCollection.features().get(index);
    selectFeature(feature);
    refreshSource();
  }

  /**
   * Deselects the state of all the features
   */
  private void deselectAll(boolean hideRecycler) {
    for (Feature feature : featureCollection.features()) {
      feature.properties().addProperty(PROPERTY_SELECTED, false);
    }
  }

  /**
   * Selects the state of a feature
   *
   * @param feature the feature to be selected.
   */
  private void selectFeature(Feature feature) {
    feature.properties().addProperty(PROPERTY_SELECTED, true);
  }

  private Feature getSelectedFeature() {
    if (featureCollection != null) {
      for (Feature feature : featureCollection.features()) {
        if (feature.getBooleanProperty(PROPERTY_SELECTED)) {
          return feature;
        }
      }
    }
    return null;
  }

  /**
   * Invoked when the bitmaps have been generated from a view.
   */
  public void setImageGenResults(HashMap<String, View> viewMap, HashMap<String, Bitmap> imageMap) {
    if (mapboxMap != null) {
      // calling addImages is faster as separate addImage calls for each bitmap.
      mapboxMap.addImages(imageMap);
    }
    // need to store reference to views to be able to use them as hitboxes for click events.
    this.viewMap = viewMap;
  }

  private void setActivityStep(@InfoWindowSymbolLayerActivity.ActivityStep int activityStep) {
    currentStep = activityStep;
  }

  @Override
  protected void onStart() {
    super.onStart();
    mapView.onStart();
  }

  @Override
  public void onResume() {
    super.onResume();
    mapView.onResume();
  }

  @Override
  public void onPause() {
    super.onPause();
    mapView.onPause();
  }

  @Override
  protected void onStop() {
    super.onStop();
    mapView.onStop();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    mapView.onSaveInstanceState(outState);
  }

  @Override
  public void onLowMemory() {
    super.onLowMemory();
    mapView.onLowMemory();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mapboxMap != null) {
      mapboxMap.removeOnMapClickListener(this);
    }
    mapView.onDestroy();
  }

  @Override
  public void onBackPressed() {
    if (currentStep == STEP_LOADING || currentStep == STEP_READY) {
      setActivityStep(STEP_INITIAL);
      deselectAll(true);
      refreshSource();
    } else {
      super.onBackPressed();
    }
  }

  private LatLng convertToLatLng(Feature feature) {
    Point symbolPoint = (Point) feature.geometry();
    return new LatLng(symbolPoint.latitude(), symbolPoint.longitude());
  }

  private Animator createLatLngAnimator(LatLng currentPosition, LatLng targetPosition) {
    ValueAnimator latLngAnimator = ValueAnimator.ofObject(new LatLngEvaluator(), currentPosition, targetPosition);
    latLngAnimator.setDuration(CAMERA_ANIMATION_TIME);
    latLngAnimator.setInterpolator(new FastOutSlowInInterpolator());
    latLngAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        mapboxMap.moveCamera(CameraUpdateFactory.newLatLng((LatLng) animation.getAnimatedValue()));
      }
    });
    return latLngAnimator;
  }

  private Animator createZoomAnimator(double currentZoom, double targetZoom) {
    ValueAnimator zoomAnimator = ValueAnimator.ofFloat((float) currentZoom, (float) targetZoom);
    zoomAnimator.setDuration(CAMERA_ANIMATION_TIME);
    zoomAnimator.setInterpolator(new FastOutSlowInInterpolator());
    zoomAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        mapboxMap.moveCamera(CameraUpdateFactory.zoomTo((Float) animation.getAnimatedValue()));
      }
    });
    return zoomAnimator;
  }

  private Animator createBearingAnimator(double currentBearing, double targetBearing) {
    ValueAnimator bearingAnimator = ValueAnimator.ofFloat((float) currentBearing, (float) targetBearing);
    bearingAnimator.setDuration(CAMERA_ANIMATION_TIME);
    bearingAnimator.setInterpolator(new FastOutSlowInInterpolator());
    bearingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        mapboxMap.moveCamera(CameraUpdateFactory.bearingTo((Float) animation.getAnimatedValue()));
      }
    });
    return bearingAnimator;
  }

  private Animator createTiltAnimator(double currentTilt, double targetTilt) {
    ValueAnimator tiltAnimator = ValueAnimator.ofFloat((float) currentTilt, (float) targetTilt);
    tiltAnimator.setDuration(CAMERA_ANIMATION_TIME);
    tiltAnimator.setInterpolator(new FastOutSlowInInterpolator());
    tiltAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
      @Override
      public void onAnimationUpdate(ValueAnimator animation) {
        mapboxMap.moveCamera(CameraUpdateFactory.tiltTo((Float) animation.getAnimatedValue()));
      }
    });
    return tiltAnimator;
  }

  /**
   * Helper class to evaluate LatLng objects with a ValueAnimator
   */
  private static class LatLngEvaluator implements TypeEvaluator<LatLng> {

    private final LatLng latLng = new LatLng();

    @Override
    public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
      latLng.setLatitude(startValue.getLatitude()
        + ((endValue.getLatitude() - startValue.getLatitude()) * fraction));
      latLng.setLongitude(startValue.getLongitude()
        + ((endValue.getLongitude() - startValue.getLongitude()) * fraction));
      return latLng;
    }
  }

  /**
   * AsyncTask to load data from the assets folder.
   */
  private static class LoadPoiDataTask extends AsyncTask<Void, Void, FeatureCollection> {

    private final WeakReference<InfoWindowSymbolLayerActivity> activityRef;

    LoadPoiDataTask(InfoWindowSymbolLayerActivity activity) {
      this.activityRef = new WeakReference<>(activity);
    }

    @Override
    protected FeatureCollection doInBackground(Void... params) {
      InfoWindowSymbolLayerActivity activity = activityRef.get();

      if (activity == null) {
        return null;
      }

      String geoJson = loadGeoJsonFromAsset(activity, "caracas_info_symbollayer.geojson");
      return FeatureCollection.fromJson(geoJson);
    }

    @Override
    protected void onPostExecute(FeatureCollection featureCollection) {
      super.onPostExecute(featureCollection);
      InfoWindowSymbolLayerActivity activity = activityRef.get();
      if (featureCollection == null || activity == null) {
        return;
      }
      activity.setupData(featureCollection);
      new GenerateViewIconTask(activity).execute(featureCollection);
    }

    static String loadGeoJsonFromAsset(Context context, String filename) {
      try {
        // Load GeoJSON file from local asset folder
        InputStream is = context.getAssets().open(filename);
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        return new String(buffer, "UTF-8");
      } catch (Exception exception) {
        throw new RuntimeException(exception);
      }
    }
  }

  /**
   * AsyncTask to generate Bitmap from Views to be used as iconImage in a SymbolLayer.
   * <p>
   * Call be optionally be called to update the underlying data source after execution.
   * </p>
   * <p>
   * Generating Views on background thread since we are not going to be adding them to the view hierarchy.
   * </p>
   */
  private static class GenerateViewIconTask extends AsyncTask<FeatureCollection, Void, HashMap<String, Bitmap>> {

    private final HashMap<String, View> viewMap = new HashMap<>();
    private final WeakReference<InfoWindowSymbolLayerActivity> activityRef;
    private final boolean refreshSource;

    GenerateViewIconTask(InfoWindowSymbolLayerActivity activity, boolean refreshSource) {
      this.activityRef = new WeakReference<>(activity);
      this.refreshSource = refreshSource;
    }

    GenerateViewIconTask(InfoWindowSymbolLayerActivity activity) {
      this(activity, false);
    }

    @SuppressWarnings("WrongThread")
    @Override
    protected HashMap<String, Bitmap> doInBackground(FeatureCollection... params) {
      InfoWindowSymbolLayerActivity activity = activityRef.get();
      if (activity != null) {
        HashMap<String, Bitmap> imagesMap = new HashMap<>();
        LayoutInflater inflater = LayoutInflater.from(activity);
        FeatureCollection featureCollection = params[0];

        for (Feature feature : featureCollection.features()) {
          View view = inflater.inflate(R.layout.symbol_layer_info_window_layout_callout, null);

          String name = feature.getStringProperty(PROPERTY_TITLE);
          TextView titleTv = view.findViewById(R.id.plain_title);
          titleTv.setText(name);

          String style = feature.getStringProperty(PROPERTY_DESCRIPTION);
          TextView styleTv = view.findViewById(R.id.plain_description);
          styleTv.setText(style);

          Bitmap bitmap = SymbolGenerator.generate(view);
          imagesMap.put(name, bitmap);
          viewMap.put(name, view);
        }

        return imagesMap;
      } else {
        return null;
      }
    }

    @Override
    protected void onPostExecute(HashMap<String, Bitmap> bitmapHashMap) {
      super.onPostExecute(bitmapHashMap);
      InfoWindowSymbolLayerActivity activity = activityRef.get();
      if (activity != null && bitmapHashMap != null) {
        activity.setImageGenResults(viewMap, bitmapHashMap);
        if (refreshSource) {
          activity.refreshSource();
        }
      }
    }
  }

  /**
   * Utility class to generate Bitmaps for Symbol.
   * <p>
   * Bitmaps can be added to the map with {@link com.mapbox.mapboxsdk.maps.MapboxMap#addImage(String, Bitmap)}
   * </p>
   */
  private static class SymbolGenerator {

    /**
     * Generate a Bitmap from an Android SDK View.
     *
     * @param view the View to be drawn to a Bitmap
     * @return the generated bitmap
     */
    static Bitmap generate(@NonNull View view) {
      int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
      view.measure(measureSpec, measureSpec);

      int measuredWidth = view.getMeasuredWidth();
      int measuredHeight = view.getMeasuredHeight();

      view.layout(0, 0, measuredWidth, measuredHeight);
      Bitmap bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888);
      bitmap.eraseColor(Color.TRANSPARENT);
      Canvas canvas = new Canvas(bitmap);
      view.draw(canvas);
      return bitmap;
    }
  }
}