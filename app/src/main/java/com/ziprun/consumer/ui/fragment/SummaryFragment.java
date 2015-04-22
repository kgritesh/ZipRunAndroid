package com.ziprun.consumer.ui.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ziprun.consumer.R;
import com.ziprun.consumer.ZipRunApp;
import com.ziprun.consumer.data.model.RideType;
import com.ziprun.consumer.presenter.SummaryPresenter;
import com.ziprun.consumer.utils.Utils;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;

public class SummaryFragment extends  DeliveryFragment{
    private static final String TAG = SummaryFragment.class.getCanonicalName();

    @Inject
    Utils utils;

    @InjectView(R.id.status)
    TextView statusUpdate;

    @InjectView(R.id.txt_estimate_distance)
    TextView txtEstimateDistance;

    @InjectView(R.id.estimate_cost)
    TextView estimateCost;

    @InjectView(R.id.txt_transaction_charge)
    TextView txtTransactionCharge;

    @InjectView(R.id.source_address)
    TextView sourceAddress;

    @InjectView(R.id.dest_address)
    TextView destinationAddress;

    @InjectView(R.id.txt_source_prefix)
    TextView sourcePrefix;

    @InjectView(R.id.txt_dest_prefix)
    TextView destinationPrefix;

    @InjectView(R.id.instructions)
    TextView instructions;
    
    SummaryPresenter summaryPresenter; 


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_summary,
                container, false);

        ButterKnife.inject(this, view);

        return view;
    }

    @Override
    protected void processArguments(Bundle args) {
        super.processArguments(args);
        summaryPresenter = (SummaryPresenter) presenter;
    }

    @Override
    public void onStart() {
        super.onStart();
        setupSummary();

    }

    @Override
    protected Object getCurrentModule() {
        return new SummaryModule(this);
    }

    @Override
    protected void setActionBar(ActionBar actionBar) {
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle("Order In Progress");
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setLogo(R.drawable.icon_refresh);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(false);
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    private void setupSummary() {

        instructions.setText(summaryPresenter.getInstruction());

        sourcePrefix.setText(
                summaryPresenter.getBookingType() == RideType.BUY
                        ? getString(R.string.txt_buy_from)
                        : getString(R.string.txt_pickup_from));

        String srcAddress =  utils.formatAddressAsHtml
                (summaryPresenter.getSourceAddress());

        String destAddress = utils.formatAddressAsHtml(
                summaryPresenter.getDestinationAddress());

        statusUpdate.setText(R.string.msg_status_pending);

        sourceAddress.setText(Html.fromHtml(srcAddress));

        destinationAddress.setText(Html.fromHtml(destAddress));

        txtEstimateDistance.setText(
            String.format(getString(R.string.txt_estimate_distance),
                    (int)summaryPresenter.getEstimateDistance()));

        estimateCost.setText(summaryPresenter.getEstimatedCost() + ".00");

        txtTransactionCharge.setText(
                String.format(getString(R.string.txt_transaction_cost),
                        summaryPresenter.getTransactionCost()));

    }

    @OnClick(R.id.callBtn)
    public void onCallBtnClicked(View view){


        startDialActivity(ZipRunApp.Constants.CONTACT_NO);
    }

    private void startDialActivity(String phone){
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phone));
        startActivity(intent);
    }


}