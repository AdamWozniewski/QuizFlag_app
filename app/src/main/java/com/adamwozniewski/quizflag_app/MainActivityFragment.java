package com.adamwozniewski.quizflag_app;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    // znacznik używany przy zapisie błędów w dzienniku LOG
    private static final String TAG = "QuizWithFlags Activity";

    // Liczba flag biorących udziuał w quizie
    private static final int FLAG_IN_QUIZ = 10;

    // Nazwy plików z obrazami flag
    private List<String> fileNameList;

    // Lista plików z obrazami flag biorących udział w bieżącym quizie
    private List<String> quizCountriesList;

    // Wybrane obszary biorące udział w quizie
    private Set<String> regionSet;

    // Poprawna nazwa kraju przypisana do bieżącej flagi
    private String correctAnswer;

    // Całkowita liczba odpowiedzi
    private int totalGuesses;

    // Liczba pooprawnych odpowiedzi
    private int correctAnswers;

    // Liczba wierszy przycisków odpowiedzi wyswietlanych na ekranie
    private int guessRows;

    //Obiekt służący do losowania
    private SecureRandom random;

    // Obiekt używany podczas opóźniania procesu ładowania kolejnej flagi w quizie
    private Handler handler;

    // Animacja błędnej odpowiedzi
    private Animation shakeAnimation;

    // Główny rozkład aplikacji
    private LinearLayout questionLinearLayout;

    // Widok wyświetlający number bieżacego pytania
    private TextView questionNumberTextView;

    // Widok wyświetlający bieżącą flagę
    private ImageView flagImageView;

    // Tablica zawierająca wiersze przycisków odpowiedzi
    private LinearLayout[] guessLineraLayout;

    // Widok wyświetlający poprawną odpowiedz
    private TextView answerTextView;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Zainicjowanie graficznego interfejsu użytkownika dla fragmentów
        super.onCreateView(inflater, container, savedInstanceState);

        // Pobranie rozkłądu dla fragmentu
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        // Inicjalizacja wybranych pól
        this.fileNameList = new ArrayList<>();
        this.quizCountriesList = new ArrayList<>();
        this.random = new SecureRandom();
        this.handler = new Handler();

        // Inicjalizacja animacji
        this.shakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.incorrect_shake);
        this.shakeAnimation.setRepeatCount(3);

        // Inicjalizacja komponentów graficznego interfejsu użytkownika
        this.questionLinearLayout = (LinearLayout) view.findViewById(R.id.quizLinearLayout);
        this.questionNumberTextView = (TextView) view.findViewById(R.id.questionNumberTextView);
        this.flagImageView = (ImageView) view.findViewById(R.id.flagImageView);
        this.guessLineraLayout = new LinearLayout[4];
        this.guessLineraLayout[0] = (LinearLayout) view.findViewById(R.id.row1LinearLayout);
        this.guessLineraLayout[1] = (LinearLayout) view.findViewById(R.id.row2LinearLayout);
        this.guessLineraLayout[2] = (LinearLayout) view.findViewById(R.id.row3LinearLayout);
        this.guessLineraLayout[3] = (LinearLayout) view.findViewById(R.id.row4LinearLayout);
        this.answerTextView = (TextView) view.findViewById(R.id.answerTextView);

        // Konfiguracja nasłuchiwania zdarzeń w przyciskach odpowiedzi

        for (LinearLayout row: this.guessLineraLayout) {
            for (int column = 0; column < row.getChildCount(); column++) {
                Button button = (Button) row.getChildAt(column);
                button.setOnClickListener(this.guessButtonListener);

            }
        }

        // Wyświetlenie formatowanego tekstu w widoku textView
        this.questionNumberTextView.setText(getString(R.string.question, 1, FLAG_IN_QUIZ));

        // Zwróc widok fragmentu do wyświetlenia
        return view;
    }

    public void updateGuessRows(SharedPreferences sharedPreferences) {
        // Pobranie informacji o ilości przycisków odpowiedzi do wyświetlenia
        String choices = sharedPreferences.getString(MainActivity.CHOICES, null);

        // Liczba wierszy z przyciskami odpowiedzi do wyśwwietlenia
        this.guessRows = Integer.parseInt(choices) / 2;
        
        // Ukrycie wszystkich wierszy z przyciskami
        for (LinearLayout linearLayout: this.guessLineraLayout) {
            linearLayout.setVisibility(View.GONE);
        }

        // Wyświetlenie okręslonej liczby wierszy z przyciskami odpowiedzi
        for (int row = 0; row < this.guessRows; row++) {
            this.guessLineraLayout[row].setVisibility(View.VISIBLE);
        }
    }

    public void updateRegions(SharedPreferences sharedPreferences) {
        // Pobranie informacji na temat wybranych przez użytkownika obszarów
        this.regionSet = sharedPreferences.getStringSet(MainActivity.REGIONS, null);
    }

    public void resetQuiz() {
        // Uzyskaj dostęp do folderu Assets
        AssetManager assetManager = getActivity().getAssets();

        // Wyczyść listę z nazwami flag
        this.fileNameList.clear();

        // Pobierz nazwy plików obrazów flag z wybranych przez uzytkownika obszarów
        try {
            // Pętla przechodząca przz każdy obszar - czyli przez każdy folder w folderze Assets
            for (String region: this.regionSet) {
                // Pobranie nazw wszytskich plików znajdujądych się w folderze danego pbszaru
                String[] paths = assetManager.list(region);

                // Usunięcie z nazw plików rozszerzen

                for (String path: paths) {
                    this.fileNameList.add(path.replace(".png", ""));
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Błąd podczas ładowania plików z obrazami flag", e);
        }

        // Zresetowanie liczby poprawnych i wszystkich udzielonych odpowiedzi
        this.correctAnswers = 0;
        this.totalGuesses = 0;

        // Wyczyszczenie listy krajów
        this.quizCountriesList.clear();

        // Inicjalizacja zmiennych wykorzystywanych przy losowaniu flag
        int flagCounter = 1;
        int numberOfFlags = this.fileNameList.size();

        // Losowanie flag
        while (flagCounter <= FLAG_IN_QUIZ) {
            // Wybierz losową wartosc z zakresu od 0 do liczby flag biorących udział w quizie
            int randomIndex = this.random.nextInt(numberOfFlags);

            // Pobierz nazwę pliku o wylosowanych indexie
            String fileName = this.fileNameList.get(randomIndex);

            // jeżeli plik o tej nazwie nie zsotał jeszcze wylosowany, to dodaj go do listy wybranych krajów
            if (!this.quizCountriesList.contains(fileName)) {
                this.quizCountriesList.add(fileName);
                ++ flagCounter;
            }
        }

        // Załaduj flagę
        this.loadNextFlag();
    }

    private void loadNextFlag() {
        // Ustalenie nazwy pliku bieżącej flagi
        String nextImage = this.quizCountriesList.remove(0); // nazwaObszaru-nazwaKraju

        // Zaktualizowanie poprawnej odpowiedzi
        this.correctAnswer = nextImage;

        // Wyczyszczenie widoku TextView
        this.answerTextView.setText("");

        // Wyświetlenie numeru bieżacego pytania
        this.questionNumberTextView.setText(getString(R.string.question, (this.correctAnswers + 1), FLAG_IN_QUIZ));

        // Pobieranie nazwy obszaru bieżącej flagi
        String region = nextImage.substring(0, nextImage.indexOf("-"));

        // Uzyskanie dostępu do folderu asstes
        AssetManager assetManager = getActivity().getAssets();

        //Otwarcie załądowanie oraz obsadzeinie widoku flagi w ImageView
        try (InputStream inputStream = assetManager.open(region + "/" + nextImage + ".png")) {

            // Załadowanie obrazu flagi jako obiekt Drawable
            Drawable drawable = Drawable.createFromStream(inputStream, nextImage);

            // Obsadzeinie obiektu drawable (flagi) w ImageView
            this.flagImageView.setImageDrawable(drawable);

            // Animacja wejścia flagi na ekran
            this.animate(false);
        } catch (IOException e) {
            Log.e(TAG, "błąd podczas ładowania" + nextImage, e);
        }

        // Przemieszanie nazw plików
        Collections.shuffle(this.fileNameList);

        // Umieszczenie prawidłowej odpowiedzi na koncu listy
        int correct = this.fileNameList.indexOf(this.correctAnswer);
        this.fileNameList.add(this.fileNameList.remove(correct));

        // Dodanie tekstu do przycisków odpowiedzi
        for (int row = 0; row < this.guessRows; row++) {
            for (int col = 0; col < 2; col++) {
                // Uzyskanie dostępu do przycisku i zmienienie jego stanu na włączony
                Button button = (Button) this.guessLineraLayout[row].getChildAt(col);
                button.setEnabled(true);

                // Pobierz nazwe kraju i ustaw ją w widoku Button
                String fileName = this.fileNameList.get((row * 2) + col);
                button.setText(this.getCountryName(fileName));
            }
        }

        // Dodanie poprawnej odpowiedzi do losowo wybranego przycisku
        int row = this.random.nextInt(this.guessRows);
        int col = this.random.nextInt(2);
        LinearLayout randomRow = this.guessLineraLayout[row];
        String countryName = this.getCountryName(this.correctAnswer);
        ((Button) randomRow.getChildAt(col)).setText(countryName);

    }

    public void animate(boolean animateOut) {
        // Nie tworzymy animacji przy wyświetlaniu pierwszej flagi
        if (this.correctAnswers == 0) return;

        // Obliczanie współrzędnych środka rozkładu
        int centerX = (this.questionLinearLayout.getLeft() + this.questionLinearLayout.getRight()) / 2;
        int centerY = (this.questionLinearLayout.getTop() + this.questionLinearLayout.getBottom()) / 2;

        // Obliczanie promienia animacji
        int radius = Math.max(this.questionLinearLayout.getWidth(), this.questionLinearLayout.getHeight());

        // Zdefiniowanie obiektu animacji
        Animator animator;

        // Wariant animacji zakrywająca flagę
        if (animateOut) {
            animator = ViewAnimationUtils.createCircularReveal(this.questionLinearLayout, centerX, centerY, radius, 0);

            // Gdy animacja się skończy
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    MainActivityFragment.this.loadNextFlag();
                }
            });

        } else {
            // Wariant animacji odkrywająca flagę
            animator = ViewAnimationUtils.createCircularReveal(this.questionLinearLayout, centerX,centerY, 0, radius);
        }

        // Określenie czasu trwania animacji
        animator.setDuration(500);

        // Start animacji
        animator.start();
    }

    private String getCountryName(String name) {
        return name.substring(name.indexOf("-") + 1).replace("_", " ");
    }

    private View.OnClickListener guessButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            // Pobranie naciśniętego przycisku oraz wyświetlanego przez niego tekstu
            Button guessButton = (Button) v;
            String guess = guessButton.getText().toString();
            String answer = MainActivityFragment.this.getCountryName(MainActivityFragment.this.correctAnswer);

            // Inkrementacjac liczby odpowiedzi udzielonych przez użytkownika w quizie
            ++MainActivityFragment.this.totalGuesses;

            // Jeżei udzielona odpowiedz jest poprawna to:
            if (guess.equals(answer)) {
                // Inkrementacja liczby poprawnych odpowiedzi
                ++ MainActivityFragment.this.correctAnswers;

                // Wyświelenie informacji zwrotnej dla użytkowniak o udzieleniu poporawnej odpowiedzi
                MainActivityFragment.this.answerTextView.setText(answer + "!");
                MainActivityFragment.this.answerTextView.setTextColor(getResources().getColor(R.color.correct_answer, getContext().getTheme()));

                // Dezaktywacja wszytskich przycisków odpowiedzi
                MainActivityFragment.this.disabledButtons();

                // Jeżeli użytkownik udzielił odpowiedzi na wsyztskie pytania
                if (MainActivityFragment.this.correctAnswers == FLAG_IN_QUIZ) {
                    // Utworzenie obiektu alertDialog z spersonalizowanym tekstem oraz przyciskiem
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Quiz results");
                    builder.setMessage(getString(R.string.results, MainActivityFragment.this.totalGuesses, (1000 / (double) MainActivityFragment.this.totalGuesses)));
                    builder.setPositiveButton(R.string.reset_quiz, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            MainActivityFragment.this.resetQuiz();
                        }
                    });
                    builder.setCancelable(false);
                    builder.show();
                } else {
                    // Odczekaj dwie sekundy i załaduj kolejną flagę
                    MainActivityFragment.this.handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            MainActivityFragment.this.animate(true);
                        }
                    }, 2000);
                }
            } else {
                // Odtworzenie animacji trzęsącej się flagi
                MainActivityFragment.this.flagImageView.startAnimation(MainActivityFragment.this.shakeAnimation);

                // Wyświetlenie informacji zwrotnej dla użytkownika o błedenj odpowiedzi
                MainActivityFragment.this.answerTextView.setText(R.string.incorrect_answer);
                MainActivityFragment.this.answerTextView.setTextColor(getResources().getColor(R.color.incorrect_answer, getContext().getTheme()));

                // Dezaktywacja przycisku z błędną odpowiedzią
                guessButton.setEnabled(false);
            }
        }
    };

    private void disabledButtons() {
        for (int row = 0; row < this.guessRows; row++) {
            LinearLayout linearLayout = this.guessLineraLayout[row];
            for (int column = 0; column < 2; column++) {
                linearLayout.getChildAt(column).setEnabled(false);
            }
        }
    }
}


