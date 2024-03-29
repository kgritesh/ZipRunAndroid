package com.ziprun.consumer.presenter;

import android.location.Address;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.maps.model.LatLng;
import com.ziprun.consumer.ZipRunApp;
import com.ziprun.consumer.data.model.AddressLocationPair;
import com.ziprun.consumer.event.CurrentLocationEvent;
import com.ziprun.consumer.event.UpdateBookingEvent;
import com.ziprun.consumer.ui.fragment.LocationPickerFragment;
import com.ziprun.consumer.utils.RetryWithDelay;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import pl.charmas.android.reactivelocation.observables.GoogleAPIConnectionException;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public abstract class LocationPickerPresenter extends DeliveryPresenter {
    private static final String TAG = LocationPickerPresenter.class.getCanonicalName();

    private static final LocationRequest locationRequest =
            LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setSmallestDisplacement(10)
                    .setInterval(100);

    @Inject
    ReactiveLocationProvider locationProvider;

    private LocationPickerFragment locationPickerView;

    private Location currentLocation;

    private LatLng currentLatLng;

    private AddressLocationPair selectedLocation;

    Subscription geocodeSubscription;

    private Boolean locationEnabledFlag = null;

    private boolean isMapReady =  false;

    private boolean performGeocode = false;

    private boolean firstLoad = true;


    public LocationPickerPresenter(LocationPickerFragment fragment){
        super(fragment);
        locationPickerView = fragment;
   }

    @Override
    public void initialize() {
        super.initialize();
    }

    @Override
    public void start() {
        super.start();
        Timber.d("Presenter Started");
        selectedLocation = getSelectedLocaion();
        if(selectedLocation == null){
            selectedLocation = new AddressLocationPair();
        }
        checkLocationSettings();
        setUpLocationUpdates();
    }

    @Override
    public void pause() {
        super.pause();
    }

    @Override
    public void stop() {
        super.stop();
        currentLatLng = null;
        currentLocation = null;
        isMapReady = false;
        locationEnabledFlag = null;
    }

    @Override
    public void destroy() {
        super.destroy();
    }


    public abstract AddressLocationPair getSelectedLocaion();

    private Action1<LocationSettingsResult> locationSettingsManager =
        new Action1<LocationSettingsResult>() {
            @Override
            public void call(LocationSettingsResult locationSettingsResult) {
                Status status = locationSettingsResult.getStatus();
                Timber.d("Location Settings Status " + status
                        .getStatusMessage());

                if (status.isSuccess()) {
                    enableLocationFlag(true);
                } else if (status.hasResolution()) {
                    locationPickerView.enableLocationServices(status);
                } else {
                    enableLocationFlag(false);
                    currentLocation = null;
                }
            }
    };

    private Action1<Location> locationSetter = new Action1<Location>() {
        @Override
        public void call(Location location) {
            currentLocation = location;
            currentLatLng = utils.getLatLngFromLocation(currentLocation);
            Timber.d("New Location Updated " + location.toString());

            if(isMapReady){
               locationPickerView.setCurrentLocationMarker(currentLatLng);
            }

            if(selectedLocation.latLng == null)
                setSelectedLocation();

            locationPickerView.showCurrentLocationBtn(true);
            bus.post(new CurrentLocationEvent(currentLocation));
        }
    };

    private Action1<Throwable> locationProviderError = new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
            Timber.e(throwable, "Unable to Check Location Settings ");
            if (throwable instanceof GoogleAPIConnectionException) {
                ConnectionResult connectionResult =
                        ((GoogleAPIConnectionException) throwable).getConnectionResult();
                try {
                    view.resolveGoogleAPIConnectionError(connectionResult);
                } catch (Exception e) {
                    Timber.e(e, "Unable to connect to google api");
                    enableLocationFlag(false);
                }

            }else{
                Log.e(TAG, "Unable to provide location updates");
                enableLocationFlag(false);
            }
        }
    };


    public void checkLocationSettings(){
        compositeSubscription.add(
            locationProvider.checkLocationSettings(
                    new LocationSettingsRequest.Builder()
                            .addLocationRequest(locationRequest)
                            .build()
            ).subscribeOn(Schedulers.newThread())
             .observeOn(AndroidSchedulers.mainThread())
             .subscribe(locationSettingsManager, locationProviderError)
        );

    }

    public void setUpLocationUpdates(){

        compositeSubscription.add(locationProvider.getLastKnownLocation()
                .concatWith(locationProvider.getUpdatedLocation
                        (locationRequest))
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(locationSetter, locationProviderError));
    }

    public void onMapReady(){
        Timber.d("Map Ready");
        isMapReady = true;
        setSelectedLocation();
    }


    public void enableLocationFlag(boolean enabled){
        locationEnabledFlag = enabled;
        if(!enabled) {
            currentLatLng = null;
            currentLocation = null;
            locationPickerView.showCurrentLocationBtn(false);
            bus.post(new CurrentLocationEvent(currentLocation));
        }else{
            locationPickerView.showCurrentLocationBtn(true);
        }
        setSelectedLocation();
    }

    private void setSelectedLocation(){

        if(!isMapReady || locationEnabledFlag == null)
            return;

        else if(selectedLocation.latLng != null && selectedLocation.address != null){
            // If Selected Location is already set, we don't care about current
            // Location
            Timber.d("Move Camera to Existing Selected Address");
            moveToSelectedAddress();
        }

        else if(!locationEnabledFlag){
            // GPS not enabled, hence we are setting current location as some
            // default latlng
            currentLatLng = null;
            currentLocation = null;
            locationPickerView.showCurrentLocationBtn(false);
            selectedLocation.latLng = ZipRunApp.Constants
                    .DEFAULT_CAMERA_POSITION;
            locationPickerView.setInitialPosition(ZipRunApp.Constants.DEFAULT_CAMERA_POSITION);

        }
        else if(currentLocation != null) {
            //Current Position Found. Setting it as selectedLocation
            Timber.d("Move Camera to Current Location");
            selectedLocation.latLng = currentLatLng;
            locationPickerView.setInitialPosition(currentLatLng);

            //locationPickerView.moveCameraAndDisableListener(currentLatLng);
        }
    }

    public void enableGeocode(boolean enabled){
        performGeocode = enabled;
    }

    public void onPlaceSelected(Observable<Place> placeObservable){
        placeObservable.subscribe(new Action1<Place>() {
            @Override
            public void call(Place place) {
                performGeocode = false;
                String formattedAddr = String.format("%s, %s",
                        place.getName(), place.getAddress());


                if(verifyDeliveryArea(formattedAddr)){
                    selectedLocation.latLng = place.getLatLng();
                    selectedLocation.address = formattedAddr;
                    moveToSelectedAddress();
                }else{
                    locationPickerView.showNotServicedError();
                }
            }
        });
    }

    public void moveToSelectedAddress(){
        performGeocode = false;
        locationPickerView.moveCamera(selectedLocation.latLng, false);
        Timber.d("Selected Location Address: " + selectedLocation.address);
        locationPickerView.updateAddress(utils.formatAddressAsHtml(selectedLocation.address));
        locationPickerView.enableCameraListener(true);
        performGeocode = true;
    }

    public void moveToCurrentPosition(){
        performGeocode = true;
        locationPickerView.moveCamera(currentLatLng, true);
    }

    public void onCameraChanged(LatLng camPos){
        Timber.d("Camera Changed. Update Selected Location " + camPos.toString()
                + " "  + performGeocode + "  " + selectedLocation.latLng );

        if(shouldDoGeocoding(camPos)) {
            Timber.d("Performing Geocoding");
            updateSelectedLocation(camPos);
            performReverseGeocode();
        }else{
            Timber.d("Shouldnt do geocoding, distance is too less");
            updateSelectedLocation(camPos);
            if(selectedLocation.address != null ){
                locationPickerView.updateAddress(utils.formatAddressAsHtml(selectedLocation.address));
            }
            performGeocode = true;
        }
    }

    private void updateSelectedLocation(LatLng pos) {
        selectedLocation.latLng = pos;
        bus.post(new UpdateBookingEvent(booking));
    }

    private boolean shouldDoGeocoding(LatLng pos){
        if(!performGeocode)
            return  false;
        else if(selectedLocation.latLng != null &&
                selectedLocation.address != null) {

            float meters = utils.calculateDistance
                    (selectedLocation.latLng, pos);

            Timber.d("Ditance btn point in meters " + meters);


            return utils.calculateDistance(selectedLocation.latLng, pos) > 10;
        }
        return true;
    }

    private void performReverseGeocode(){
        selectedLocation.address = "";
        locationPickerView.startGeocoding();
        if(geocodeSubscription != null){
            compositeSubscription.remove(geocodeSubscription);
        }
        geocodeSubscription = locationProvider
            .getGeocodeObservable(selectedLocation.latLng.latitude,
                    selectedLocation.latLng.longitude, 1)
            .retryWhen(new RetryWithDelay(3, 2000))
            .subscribeOn(Schedulers.newThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<List<Address>>() {
                @Override
                public void call(List<Address> addresses) {
                    if (addresses.size() == 0)
                        return;

                    selectedLocation.address = utils.addressToString(
                            addresses.get(0), ", ");

                    if(verifyDeliveryArea(selectedLocation.address)){
                        String address = utils.formatAddressAsHtml(selectedLocation.address);
                        Timber.d("Reverse Geocoded Address " + address);
                        locationPickerView.updateAddress(address);
                    }else{
                        locationPickerView.showNotServicedError();
                    }

                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    Timber.e(throwable, throwable.getMessage());
                    locationPickerView.updateAddress(null);
                    selectedLocation.address = "";
                }
            });

        compositeSubscription.add(geocodeSubscription);
    }

    public boolean verifyDeliveryArea(String address){
        String city = utils.getCityFromAddress(address).toLowerCase();
        return Arrays.asList(ZipRunApp.Constants.CITIES_SERVED).contains(city);
    }


    public Location getCurrentLocation() {
        return currentLocation;
    }

    public LatLng getCurrentLatLng() {
        return currentLatLng;
    }


    public abstract void moveForward();
}

