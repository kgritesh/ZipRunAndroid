/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.ziprun.maputils.models;

import com.google.android.gms.maps.model.LatLng;
import com.ziprun.maputils.GoogleDirectionAPI;


/**
 * Each element in the steps of a {@link DirectionsLeg} defines a single step of the calculated
 * directions. A step is the most atomic unit of a direction's route, containing a single step
 * describing a specific, single instruction on the journey. E.g. "Turn left at W. 4th St."
 * The step not only describes the instruction but also contains distance and duration information
 * relating to how this step relates to the following step. For example, a step denoted as
 * "Merge onto I-80 West" may contain a duration of "37 miles" and "40 minutes," indicating
 * that the next step is 37 miles/40 minutes from this step.
 * <p/>
 * <p>When using the Directions API to search for transit directions, the steps array will include
 * additional <a href="https://developers.google.com/maps/documentation/directions/#TransitDetails">
 * Transit Details</a> in the form of a {@code transitDetails} array. If the directions include
 * multiple modes of transportation, detailed directions will be provided for walking or driving
 * steps in a {@code steps} array. For example, a walking step will include directions from
 * the start and end locations: "Walk to Innes Ave & Fitch St". That step will include detailed
 * walking directions for that route in the {@code steps} array, such as: "Head north-west",
 * "Turn left onto Arelious Walker", and "Turn left onto Innes Ave".
 */
public class DirectionsStep {

    /**
     * {@code htmlInstructions} contains formatted instructions for this step, presented as an
     * HTML text string.
     */
    public String htmlInstructions;

    /**
     * {@code distance} contains the distance covered by this step until the next step.
     */
    public Distance distance;

    /**
     * {@code duration} contains the typical time required to perform the step, until the next step.
     */
    public Duration duration;


    /**
     * {@code startLocation} contains the location of the starting point of this step.
     */
    public LatLng startLocation;

    /**
     * {@code endLocation} contains the location of the last point of this step.
     */
    public LatLng endLocation;

    /**
     * {@code steps} contains detailed directions for walking or driving steps in transit
     * directions. Substeps are only available when travelMode is set to "transit".
     */
    public DirectionsStep[] steps;

    /**
     * {@code polyline} is the path of this step.
     */
    public EncodedPolyline polyline;

    /**
     * {@code travelMode} is the travel mode of this step. See
     * <a href="https://developers.google.com/maps/documentation/directions/#TravelModes">Travel
     * Modes</a> for more detail.
     */
    public GoogleDirectionAPI.TravelMode travelMode;


}
