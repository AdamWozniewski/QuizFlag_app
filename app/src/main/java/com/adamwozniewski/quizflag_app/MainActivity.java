package com.adamwozniewski.quizflag_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

//    Klucze preferencji

    public static final String CHOICES = "pref_numberofChoices";
    public static final String REGIONS = "pref_regionsToInclude";

//    Czy aplikacja została uruchomiona na telefonie?
    private boolean phoneDevice = true;

//    Czy nastąpiła zmiana preferencji
    private boolean preferencesChanged = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

//        Przypisywanie domyslnych ustawień do obiektu SharedPreferences

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

//        Rejestrowanie obiektu nasłuchującego zmian obiektu SharedPreferences
        PreferenceManager
                .getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(preferencesChangedListener);

        // Pobranie rozmiaru ekranu urządzenia
        int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

        // Jezeli rozmiar ekranu jest typowy dla tabletu to:
        if (screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE ||
            screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            this.phoneDevice = false;
        }

        // Jeżeli uruchamiamy aplikację na telefonie to
        if (this.phoneDevice) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (this.preferencesChanged) {
            MainActivityFragment quizFragment = (MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.quizFragment);
            quizFragment.updateQuessRows(PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.updateRegions(PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.resetQuiz();
            this.preferencesChanged = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Pobranie informacji o orientacji urządzenia
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            // Inflate the menu; this adds items to the action bar if it is present.
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent preferencesIntent = new Intent(this, SettingsActivity.class);
        startActivity(preferencesIntent);

        return super.onOptionsItemSelected(item);
    }

//    Obiekt nasłuchujący zmian obiektu SharedPreferences
    private SharedPreferences.OnSharedPreferenceChangeListener preferencesChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            // Użytkownik zmienił ustawienia aplikacji
            MainActivity.this.preferencesChanged = true;

//            Inicjalizacja obiektu MainActivityFragment
            MainActivityFragment quizFragment = (MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.quizFragment);

//            Instrukcja warunkowa dla rodzaju zmienionych ustawień
            if (key.equals(MainActivity.CHOICES)) {

//                Aktualizacja liczby wyświetlanych wierszy z przyciskami odpowiedzi
                quizFragment.updateGuessRows(sharedPreferences);

//                Zresetowanie quizu
                quizFragment.resetQuiz();

            } else if (key.equals(MainActivity.REGIONS)) {
//                Pobranie listy wybranych obszarów
                Set<String> regions = sharedPreferences.getStringSet(MainActivity.REGIONS, null);
//                Jeżeli wybrano więcej niż jeden obszar
                if (regions != null && regions.size() > 0) {
                    quizFragment.updateRegions(sharedPreferences);
                    quizFragment.resetQuiz();
                } else {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    regions.add(getString(R.string.default_region));
                    editor.putStringSet(MainActivity.REGIONS, regions);
                    editor.apply();

                    Toast.makeText(MainActivity.this, R.string.default_region_message, Toast.LENGTH_SHORT).show();
                }

//                Informowanie użytkownika o restarcie quizu
                Toast.makeText(MainActivity.this, R.string.reset_quiz, Toast.LENGTH_SHORT).show();
            }
        }
    };
}
