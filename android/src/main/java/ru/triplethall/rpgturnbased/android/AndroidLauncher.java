package ru.triplethall.rpgturnbased.android;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import ru.triplethall.rpgturnbased.RPGTurnbased;

/** Launches the Android application. */
public class AndroidLauncher extends AndroidApplication {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
        configuration.useImmersiveMode = true; // Recommended, but not required.

        configuration.useAccelerometer = false; // Отключить акселерометр
        configuration.useGyroscope = false; // Отключить гироскоп
        configuration.useCompass = false; // Отключить компас



        initialize(new RPGTurnbased(), configuration);
    }
}
