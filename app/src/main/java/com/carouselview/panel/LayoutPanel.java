package com.carouselview.panel;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.carouselview.R;

/**
 * Created by CarouselView on 7/7/16.
 */
public class LayoutPanel extends BasePanel {

    //Constructors
    public LayoutPanel(Context context) {
        this(context, null);
    }

    public LayoutPanel(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LayoutPanel(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initLayoutPanel();
    }

    /* ***************************************************************************** */
    /* ******************************** Utility API ******************************** */
    /* ***************************************************************************** */

    private void initLayoutPanel() {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_panel, null);
        view.findViewById(R.id.click_me_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getContext(), "Button click", Toast.LENGTH_SHORT).show();
            }
        });

        mPanelContainer.addView(view);
    }
}
