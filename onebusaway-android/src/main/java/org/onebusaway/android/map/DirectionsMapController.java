/**
 * Copyright (C) 2016 Cambridge Systematics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.android.map;

import org.onebusaway.android.io.elements.ObaShape;
import org.onebusaway.android.io.elements.ObaShapeElement;
import org.onebusaway.android.util.LocationUtils;
import org.opentripplanner.api.model.EncodedPolylineBean;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.routing.core.TraverseMode;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

/**
 * Used to show trip plan results on the map
 */
public class DirectionsMapController implements MapModeController {

    private static final String TAG = "DirectionsMapController";

    private final Callback mFragment;

    private Itinerary mItinerary;

    private boolean mHasRoute = false;

    private Location mCenter;

    public DirectionsMapController(Callback callback) {
        mFragment = callback;
    }

    @Override
    public void setState(Bundle args) {
        if (args != null) {
            mItinerary = (Itinerary) args.getSerializable(MapParams.ITINERARY);
        }
        onResume();
    }

    /**
     * Clears the current state of the controller, so a new route can be loaded
     */
    private void clearCurrentState() {
        // Clear the existing route and vehicle overlays
        mFragment.getMapView().removeRouteOverlay();
        mFragment.getMapView().removeVehicleOverlay();
        mFragment.getMapView().removeStopOverlay(false);
    }

    @Override
    public String getMode() {
        return MapParams.MODE_DIRECTIONS;
    }

    @Override
    public void destroy() {
        clearCurrentState();
    }

    @Override
    public void onPause() {
        // Don't care
    }

    /**
     * This is called when fm.beginTransaction().hide() or fm.beginTransaction().show() is called
     *
     * @param hidden True if the fragment is now hidden, false if it is not visible.
     */
    @Override
    public void onHidden(boolean hidden) {
        // Don't care
    }

    @Override
    public void onResume() {

        clearCurrentState();
        if (mItinerary == null) {
            return;
        }

        // Set route overlays for map. If there are no routes (ie start and end are same location)
        // zoom to origin.

        Leg firstLeg = mItinerary.legs.get(0);
        mCenter = LocationUtils.makeLocation(firstLeg.from.getLat(), firstLeg.from.getLon());

        for (Leg leg : mItinerary.legs) {
            LegShape shape = new LegShape(leg.legGeometry);

            if (shape.getLength() > 0) {
                mHasRoute = true;
                int color = resolveColor(leg);
                mFragment.getMapView().setRouteOverlay(color, new LegShape[]{shape}, false);
            }
        }

        zoom();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // Don't care
    }

    @Override
    public void onLocation() {
        // Don't care
    }

    @Override
    public void onNoLocation() {
        // Don't care
    }

    @Override
    public void notifyMapChanged() {
        // Don't care
    }

    @Override
    public void onViewStateRestored(Bundle bundle) {
        // Don't care
    }

    private void zoom() {
        ObaMapView view = mFragment.getMapView();

        if (mHasRoute) {
            view.zoomToItinerary();
        } else {
            view.setMapCenter(mCenter, false, false);
            view.setZoom(MapParams.DEFAULT_ZOOM);
        }
    }

    private static int resolveColor(Leg leg) {
        if (leg.routeColor != null) {
            try {
                return Long.decode("0xFF" + leg.routeColor).intValue();
            } catch (Exception ex) {
                Log.e(TAG, "Error parsing color=" + leg.routeColor + ": " + ex.getMessage());
            }
        }

        if (TraverseMode.valueOf(leg.mode).isTransit()) {
            return Color.BLUE;
        }

        return Color.GRAY;
    }

    class LegShape implements ObaShape {

        private EncodedPolylineBean bean;

        LegShape(EncodedPolylineBean bean) {
            this.bean = bean;
        }

        @Override
        public int getLength() {
            return bean.getLength();
        }

        @Override
        public String getRawLevels() {
            return bean.getLevels();
        }

        @Override
        public List<Integer> getLevels() {
            return ObaShapeElement.decodeLevels(bean.getLevels(), bean.getLength());
        }

        @Override
        public List<Location> getPoints() {
            return ObaShapeElement.decodeLine(bean.getPoints(), bean.getLength());
        }

        @Override
        public String getRawPoints() {
            return bean.getPoints();
        }

    }
}
