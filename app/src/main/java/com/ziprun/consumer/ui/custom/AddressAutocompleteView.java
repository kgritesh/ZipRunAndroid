package com.ziprun.consumer.ui.custom;

import android.content.Context;
import android.location.Location;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.ziprun.consumer.R;
import com.ziprun.consumer.ui.activity.ZipBaseActivity;
import com.ziprun.consumer.utils.TextObservable;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import pl.charmas.android.reactivelocation.DataBufferObservable;
import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import rx.Observable;
import rx.functions.Action1;
import rx.functions.Func1;

public class AddressAutocompleteView extends RelativeLayout {
    private static final String TAG = AddressAutocompleteView.class.getCanonicalName();

    private ArrayAdapter<String> listAdapter ;

    @InjectView(R.id.addressSearch)
    EditText addressSearch;

    @InjectView(R.id.autocompleteList)
    ListView autocompleteListView;

    @Inject
    ReactiveLocationProvider locationProvider;

    @Inject InputMethodManager inputMethodManager;

    Location currentLocation;

    Observable<List<PlaceAutocomplete>> autoCompleteObservable;

    PlaceAutocompleteAdapter autocompleteAdapter;

    OnAddressSelectedListener onAddressSelectedListener;

    public AddressAutocompleteView(Context context) {
        super(context);
        init();
    }

    public AddressAutocompleteView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AddressAutocompleteView(Context context, AttributeSet attrs,
                                   int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.address_autocomplete_view, this);
        if (!isInEditMode()) {
            ((ZipBaseActivity) getContext()).getActivityGraph().inject(this);
            locationProvider.getLastKnownLocation().subscribe(new Action1<Location>() {
                @Override
                public void call(Location location) {
                    currentLocation = location;
                }
            });
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        ButterKnife.inject(this);
        createAutoCompleteObservable();
        autoCompleteObservable.subscribe(new Action1<List<PlaceAutocomplete>>() {
            @Override
            public void call(List<PlaceAutocomplete> placeAutocompletes) {
                if(autocompleteAdapter == null){
                    autocompleteAdapter = new PlaceAutocompleteAdapter(getContext(),
                            placeAutocompletes);

                    autocompleteListView.setAdapter(autocompleteAdapter);

                }else{
                    autocompleteAdapter.update(placeAutocompletes);
                }
            }
        });

        autocompleteListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view,
                                    int position, long id) {
                PlaceAutocomplete autocomplete =
                        autocompleteAdapter.getItem(position);

                Observable<Place> placeObservable = getPlaceObservable
                        (autocomplete.getPlaceID());

                onAddressSelectedListener.onAddressSelected(placeObservable);
            }
        });
    }

    private Observable<Place> getPlaceObservable(final String placeID){
        return locationProvider.getGoogleApiClientObservable(Places.GEO_DATA_API)
            .flatMap(new Func1<GoogleApiClient, Observable<PlaceBuffer>>() {
                @Override
                public Observable<PlaceBuffer> call(GoogleApiClient apiClient) {
                    return ReactiveLocationProvider.fromPendingResult(
                            Places.GeoDataApi.getPlaceById(apiClient,
                                    placeID));
                }
            })
            .flatMap(new Func1<PlaceBuffer, Observable<Place>>() {
                @Override
                public Observable<Place> call(PlaceBuffer places) {
                    return DataBufferObservable.from(places);
                }
            });
    }

    private void createAutoCompleteObservable() {
        autoCompleteObservable = TextObservable.create(addressSearch)
            .debounce(1, TimeUnit.SECONDS)
            .filter(new Func1<String, Boolean>() {
                @Override
                public Boolean call(String s) {
                    return !TextUtils.isEmpty(s);
                }
        })
        .flatMap(new Func1<String,
                Observable<AutocompletePredictionBuffer>>() {
            @Override
            public Observable<AutocompletePredictionBuffer> call(String query) {
                LatLngBounds bounds = null;
                if (currentLocation != null) {
                    double latitude = currentLocation.getLatitude();
                    double longitude = currentLocation.getLongitude();
                    bounds = new LatLngBounds(
                            new LatLng(latitude - 0.05,
                                    longitude - 0.05),
                            new LatLng(latitude + 0.05, longitude + 0.05)
                    );
                }
                return locationProvider.getPlaceAutocompletePredictions
                        (query, bounds, null);
            }
        }).flatMap(new Func1<AutocompletePredictionBuffer, Observable<List<PlaceAutocomplete>>>() {
            @Override
            public Observable<List<PlaceAutocomplete>> call(AutocompletePredictionBuffer predBuffer) {

                return DataBufferObservable.from(predBuffer)
                    .map(new Func1<AutocompletePrediction, PlaceAutocomplete>() {
                        @Override
                        public PlaceAutocomplete call(AutocompletePrediction
                                                              prediction) {
                            return new PlaceAutocomplete(prediction);
                        }
                    }).toList();
            }
        });
    }

    public void setOnAddressSelectedListener(OnAddressSelectedListener
                                                     listener){
        this.onAddressSelectedListener = listener;
    }

    public void reset(){
        addressSearch.setText("");
        autocompleteAdapter.clear();

        inputMethodManager.hideSoftInputFromWindow(addressSearch
                .getWindowToken(), 0);
    }



    public static class PlaceAutocomplete {

        public CharSequence placeId;
        public CharSequence description;

        PlaceAutocomplete(CharSequence placeId, CharSequence description) {
            this.placeId = placeId;
            this.description = description;
        }

        PlaceAutocomplete(AutocompletePrediction prediction) {
            this(prediction.getPlaceId(), prediction.getDescription());
        }

        @Override
        public String toString() {
            return description.toString();
        }

        public String getPlaceID(){
            return String.valueOf(placeId);
        }
    }

    public interface OnAddressSelectedListener {
        public void onAddressSelected(Observable<Place> place);
    }
}

class PlaceAutocompleteAdapter extends ArrayAdapter<
        AddressAutocompleteView.PlaceAutocomplete>{

    List<AddressAutocompleteView.PlaceAutocomplete> autocompleteList;
    private LayoutInflater inflater;

    private static int LAYOUT_RES_ID = android.R.layout.simple_list_item_1;

    public PlaceAutocompleteAdapter(Context context,
                                    List<AddressAutocompleteView.PlaceAutocomplete>
                                            autocompletes) {
        super(context, LAYOUT_RES_ID, autocompletes);
        inflater = LayoutInflater.from(context);
    }

    public void update(List<AddressAutocompleteView.PlaceAutocomplete>
                               autocompletes){

        this.clear();
        this.addAll(autocompletes);
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ViewHolder holder;
        if (view != null) {
            holder = (ViewHolder) view.getTag();
        } else {
            view = inflater.inflate(LAYOUT_RES_ID, parent, false);
            holder = new ViewHolder(view);
            view.setTag(holder);
        }
        AddressAutocompleteView.PlaceAutocomplete autocomplete =
                getItem(position);
        holder.name.setText(autocomplete.toString());

        return view;
    }

    static class ViewHolder {
        @InjectView(android.R.id.text1)
        TextView name;

        public ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }
}





