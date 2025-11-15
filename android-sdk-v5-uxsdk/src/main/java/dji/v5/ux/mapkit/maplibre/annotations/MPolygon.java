package dji.v5.ux.mapkit.maplibre.annotations;


import androidx.annotation.ColorInt;

import dji.v5.ux.mapkit.core.models.DJILatLng;
import dji.v5.ux.mapkit.core.models.annotations.DJIPolygon;
import dji.v5.ux.mapkit.core.models.annotations.DJIPolygonOptions;
import dji.v5.ux.mapkit.maplibre.utils.MaplibreUtils;
import org.maplibre.android.annotations.Polygon;
import org.maplibre.android.annotations.Polyline;
import org.maplibre.android.annotations.PolylineOptions;
import org.maplibre.android.geometry.LatLng;
import org.maplibre.android.maps.MapLibreMap;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by joeyang on 11/2/17.
 * Mapbox的多边形代理类
 */
public class MPolygon implements DJIPolygon {

    private static final float NO_ALPHA = 0.0F;

    private MapLibreMap mapLibreMap;
    private Polygon polygon;
    private DJIPolygonOptions options;
    private Polyline border;
    private float borderAlpha;


    public MPolygon(Polygon polygon, MapLibreMap mapLibreMap, DJIPolygonOptions options) {
        this.polygon = polygon;
        this.mapLibreMap = mapLibreMap;
        this.options = options;

        PolylineOptions polylineOptions = new PolylineOptions();
        polylineOptions.addAll(polygon.getPoints())
                .add(polygon.getPoints().get(0))
                .color(options.getStrokeColor())
                .width(options.getStrokeWidth() / 5f);
        borderAlpha = polylineOptions.getAlpha();
        border = mapLibreMap.addPolyline(polylineOptions);
    }


    /**
     * delete the line we add
     */
    @Override
    public void remove() {
        mapLibreMap.removePolygon(polygon);
        if (border != null) {
            mapLibreMap.removePolyline(border);
        }
    }

    @Override
    public boolean isVisible() {
        boolean visible;
        if (polygon.getAlpha() != NO_ALPHA) {
            visible = true;
        } else {
            visible = false;
        }
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            polygon.setAlpha(options.getAlpha());
            if (border != null) {
                border.setAlpha(borderAlpha);
            }
        } else {
            polygon.setAlpha(NO_ALPHA);
            if (border != null) {
                border.setAlpha(NO_ALPHA);
            }
        }
    }

    @Override
    public void setPoints(List<DJILatLng> points) {
        List<LatLng> mPoints = new ArrayList<>(points.size());
        for (DJILatLng latLng : points) {
            mPoints.add(MaplibreUtils.fromDJILatLng(latLng));
        }
        polygon.setPoints(mPoints);
    }

    @Override
    public List<DJILatLng> getPoints() {
        List<DJILatLng> djiPoints = new ArrayList<>(polygon.getPoints().size());
        for (LatLng latLng : polygon.getPoints()) {
            djiPoints.add(MaplibreUtils.fromLatLng(latLng));
        }
        return djiPoints;
    }

    @Override
    public void setFillColor(@ColorInt int color) {
        polygon.setFillColor(color);
    }

    @Override
    public int getFillColor() {
        return polygon.getFillColor();
    }

    @Override
    public void setStrokeColor(@ColorInt int color) {
        polygon.setStrokeColor(color);
        if (border != null) {
            border.setColor(color);
        }
    }

    @Override
    public int getStrokeColor() {
        if (border != null) {
            return border.getColor();
        }
        return polygon.getStrokeColor();
    }

    @Override
    public void setStrokeWidth(float strokeWidth) {
        if (border != null) {
            border.setWidth(strokeWidth);
        }
    }

    @Override
    public float getStrokeWidth() {
        if (border != null) {
            return border.getWidth();
        }
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MPolygon mPolygon1 = (MPolygon) o;

        if (mapLibreMap != null ? !mapLibreMap.equals(mPolygon1.mapLibreMap) : mPolygon1.mapLibreMap != null)
            return false;
        return polygon != null ? polygon.equals(mPolygon1.polygon) : mPolygon1.polygon == null;
    }

    @Override
    public int hashCode() {
        int result = mapLibreMap != null ? mapLibreMap.hashCode() : 0;
        result = 31 * result + (polygon != null ? polygon.hashCode() : 0);
        return result;
    }
}
