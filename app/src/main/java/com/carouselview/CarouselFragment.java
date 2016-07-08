package com.carouselview;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.carousel.CarouselView;
import com.carouselview.panel.ImagePanel;
import com.carouselview.panel.LayoutPanel;
import com.carouselview.panel.ListLayoutPanel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by binary on 3/8/16.
 */
public class CarouselFragment extends Fragment implements ListLayoutPanel.OnScrollListener {

    public static CarouselFragment newInstance() {
        Bundle args = new Bundle();

        CarouselFragment fragment = new CarouselFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private CarouselView mCarouselView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_carousel, null, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCarouselView = (CarouselView) view.findViewById(R.id.carouselView);
        for (View stubItem : initStubItems()) {
            mCarouselView.addView(stubItem);
        }

        mCarouselView.notifyDataSetChanged();

        view.findViewById(R.id.prev).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = mCarouselView.getSelectedItemPosition() == 0 ? mCarouselView.getCount() - 1 : mCarouselView.getSelectedItemPosition() - 1;
                mCarouselView.scrollToChild(position);
                mCarouselView.invalidate();
            }
        });

        view.findViewById(R.id.next).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = mCarouselView.getSelectedItemPosition() == mCarouselView.getCount() - 1 ? 0 : mCarouselView.getSelectedItemPosition() + 1;
                mCarouselView.scrollToChild(position);
                mCarouselView.invalidate();
            }
        });
    }

    // Stub items
    private List<View> initStubItems() {
        List<View> result = new ArrayList<>();

        ImagePanel imagePanel = new ImagePanel(getActivity());
        imagePanel.setImageResId(R.drawable.iron_man);
        result.add(imagePanel);

        imagePanel = new ImagePanel(getActivity());
        imagePanel.setImageResId(R.drawable.natasha);
        result.add(imagePanel);

        imagePanel = new ImagePanel(getActivity());
        imagePanel.setImageResId(R.drawable.tor);
        result.add(imagePanel);

        LayoutPanel layoutPanel = new LayoutPanel(getActivity());
        result.add(layoutPanel);

        ListLayoutPanel listLayoutPanel = new ListLayoutPanel(getActivity());
        listLayoutPanel.setOnScrollListener(this);
        result.add(listLayoutPanel);

        return result;
    }

    @Override
    public void onScroll() {
        mCarouselView.invalidate();
    }
}
