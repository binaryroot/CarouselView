package com.carouselview;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.os.Bundle;


/**
 * Created by binary on 3/8/16.
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.container, CarouselFragment.newInstance());
        fragmentTransaction.commit();
    }
}
