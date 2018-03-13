package com.sombright.simultanea;

import com.google.android.gms.nearby.connection.Strategy;

/**
 *  A set of constants used within the app.
 */
class Constants {
    /**
     * A tag for logging. Use 'adb logcat -s Simultanea' to follow the logs.
     */
    static final String TAG = "Simultanea";

    /**
     * This service id lets us find other nearby devices that are interested in the same thing.
     */
    static final String SERVICE_ID = Constants.class.getPackage().getName() + ".SERVICE_ID";

    /**
     * The connection strategy we'll use for Nearby Connections. In this case, we've decided on
     * P2P_STAR, which is a combination of Bluetooth Classic and WiFi Hotspots.
     */
    static final Strategy STRATEGY = Strategy.P2P_STAR;
}