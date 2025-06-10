package com.example.trackerwydatkow;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.example.trackerwydatkow.wydatki.Wydatki;
import com.example.trackerwydatkow.wydatki.WydatkiBaza;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ReceiptPreviewActivity extends AppCompatActivity {
    private static final String TAG = "ReceiptPreviewActivity";

    private ImageView imageReceipt;
    private TextInputEditText editMerchantName, editAmount, editDate;
    private MaterialAutoCompleteTextView dropdownCategory;
    private TextView textExtracted, textExtractedHeader, textConfidence;
    private Button btnSave, btnCancel;

    private String imageUriString;
    private String extractedText;
    private WydatkiBaza database;
    private boolean isExtractedTextVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_receipt_preview);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layoutButtons), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        setupCategoryDropdown();
        loadDataFromIntent();
        setupClickListeners();

        database = WydatkiBaza.getInstance(this);
    }

    private void initializeViews() {
        imageReceipt = findViewById(R.id.imageReceipt);
        editMerchantName = findViewById(R.id.editMerchantName);
        editAmount = findViewById(R.id.editAmount);
        editDate = findViewById(R.id.editDate);
        dropdownCategory = findViewById(R.id.dropdownCategory);
        textExtracted = findViewById(R.id.textExtracted);
        textExtractedHeader = findViewById(R.id.textExtractedHeader);
        textConfidence = findViewById(R.id.textConfidence);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void setupCategoryDropdown() {
        String[] kategorie = new String[]{
                "Jedzenie",
                "Rachunki",
                "Paliwo",
                "Rozrywki",
                "Zakupy",
                "Zdrowie"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.dropdown_item,
                kategorie
        );
        dropdownCategory.setAdapter(adapter);

        // Kategoria defaultowa
        dropdownCategory.setText("Zakupy", false);
    }

    private void loadDataFromIntent() {
        Intent intent = getIntent();

        imageUriString = intent.getStringExtra("image_uri");
        extractedText = intent.getStringExtra("extracted_text");
        double parsedAmount = intent.getDoubleExtra("parsed_amount", 0.0);
        String parsedMerchant = intent.getStringExtra("parsed_merchant");
        String parsedDate = intent.getStringExtra("parsed_date");

        // Załaduj obrazek
        if (imageUriString != null) {
            Uri imageUri = Uri.parse(imageUriString);
            Glide.with(this)
                    .load(imageUri)
                    .into(imageReceipt);
        }

        // Wypełnij formularz
        if (parsedMerchant != null && !parsedMerchant.trim().isEmpty()) {
            editMerchantName.setText(parsedMerchant.trim());

            // Auto-select kategorii na bazie sklepu
            String category = getCategoryFromMerchant(parsedMerchant);
            dropdownCategory.setText(category, false);
        }

        if (parsedAmount > 0) {
            editAmount.setText(String.format(Locale.getDefault(), "%.2f", parsedAmount));
        }

        if (parsedDate != null && !parsedDate.trim().isEmpty()) {
            editDate.setText(parsedDate);
        } else {
            // Dzisiaj = default
            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            editDate.setText(currentDate);
        }

        // Pokaż tekst wyciągnięty
        if (extractedText != null) {
            textExtracted.setText(extractedText);

            // Pewność kalkulacji
            float confidence = calculateConfidence(extractedText, parsedAmount);
            textConfidence.setText(String.format(Locale.getDefault(),
                    "Dokładność rozpoznawania: %.0f%%", confidence * 100));
        }
    }

    private String getCategoryFromMerchant(String merchant) {
        String merchantLower = merchant.toLowerCase();

        if (merchantLower.contains("biedronka") || merchantLower.contains("żabka") ||
                merchantLower.contains("tesco") || merchantLower.contains("carrefour") ||
                merchantLower.contains("lidl") || merchantLower.contains("auchan") ||
                merchantLower.contains("market") || merchantLower.contains("sklep")) {
            return "Jedzenie";
        } else if (merchantLower.contains("orlen") || merchantLower.contains("bp") ||
                merchantLower.contains("shell") || merchantLower.contains("lotos") ||
                merchantLower.contains("circle") || merchantLower.contains("paliw")) {
            return "Paliwo";
        } else if (merchantLower.contains("apteka") || merchantLower.contains("pharmacy") ||
                merchantLower.contains("medyk") || merchantLower.contains("centrum zdrowia")) {
            return "Zdrowie";
        } else if (merchantLower.contains("kino") || merchantLower.contains("cinema") ||
                merchantLower.contains("teatr") || merchantLower.contains("restaurant") ||
                merchantLower.contains("restauracja") || merchantLower.contains("klub")) {
            return "Rozrywki";
        } else if (merchantLower.contains("enea") || merchantLower.contains("pge") ||
                merchantLower.contains("tauron") || merchantLower.contains("orange") ||
                merchantLower.contains("play") || merchantLower.contains("plus") ||
                merchantLower.contains("t-mobile")) {
            return "Rachunki";
        }

        return "Zakupy"; // Default category
    }

    private float calculateConfidence(String text, double amount) {
        float confidence = 0.5f; // Bazowa

        // dokładność+ gdy kwota
        if (amount > 0) {
            confidence += 0.3f;
        }

        // dokładność+ gdy dlugość tekstu
        if (text.length() > 50) {
            confidence += 0.1f;
        }
        if (text.length() > 100) {
            confidence += 0.1f;
        }

        //Częste hasła
        String textLower = text.toLowerCase();
        if (textLower.contains("razem") || textLower.contains("suma") ||
                textLower.contains("total") || textLower.contains("paragon")) {
            confidence += 0.1f;
        }

        return Math.min(confidence, 1.0f);
    }

    private void setupClickListeners() {
        btnCancel.setOnClickListener(v -> {
            // Skasuj tymczasowy plik
            deleteTemporaryImage();
            finish();
        });

        btnSave.setOnClickListener(v -> saveExpense());

        editDate.setOnClickListener(v -> showDatePicker());

        textExtractedHeader.setOnClickListener(v -> toggleExtractedText());
    }

    private void showDatePicker() {
        String currentDateStr = editDate.getText().toString();
        Calendar calendar = Calendar.getInstance();

        // Parsuj datę gdy znajdzie ją
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date currentDate = sdf.parse(currentDateStr);
            if (currentDate != null) {
                calendar.setTime(currentDate);
            }
        } catch (ParseException e) {
            // Użyj aktualnej gdy się nie uda
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = String.format(Locale.getDefault(),
                            "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    editDate.setText(selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private void toggleExtractedText() {
        isExtractedTextVisible = !isExtractedTextVisible;
        textExtracted.setVisibility(isExtractedTextVisible ? View.VISIBLE : View.GONE);
        textExtractedHeader.setText(isExtractedTextVisible
                ? "📄 Rozpoznany tekst (kliknij aby zwinąć)"
                : "📄 Rozpoznany tekst (kliknij aby rozwinąć)");
    }

    private void saveExpense() {
        // Walidacja
        String merchantName = editMerchantName.getText().toString().trim();
        String amountStr = editAmount.getText().toString().trim();
        String category = dropdownCategory.getText().toString().trim();
        String date = editDate.getText().toString().trim();

        if (merchantName.isEmpty()) {
            editMerchantName.setError("Wprowadź nazwę sklepu");
            editMerchantName.requestFocus();
            return;
        }

        if (amountStr.isEmpty()) {
            editAmount.setError("Wprowadź kwotę");
            editAmount.requestFocus();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr.replace(",", "."));
            if (amount <= 0) {
                editAmount.setError("Kwota musi być większa od 0");
                editAmount.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            editAmount.setError("Nieprawidłowa kwota");
            editAmount.requestFocus();
            return;
        }

        if (category.isEmpty()) {
            Toast.makeText(this, "Wybierz kategorię", Toast.LENGTH_SHORT).show();
            return;
        }

        // Upload do Firebase'a
        uploadImageAndSaveExpense(merchantName, amount, category, date);
    }

    private void uploadImageAndSaveExpense(String merchantName, double amount, String category, String date) {
        if (imageUriString == null) {
            saveExpenseToDatabase(merchantName, amount, category, date, null);
            return;
        }

        // Pokaż stan ładowania
        btnSave.setEnabled(false);
        btnSave.setText("Zapisywanie...");

        Uri imageUri = Uri.parse(imageUriString);
        File imageFile = new File(imageUri.getPath());

        // Ref do Firebase Storage
        FirebaseStorage storage = FirebaseStorage.getInstance();
        String fileName = "receipts/" + System.currentTimeMillis() + "_receipt.jpg";
        StorageReference storageRef = storage.getReference().child(fileName);

        // Upload
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // URL
                    storageRef.getDownloadUrl()
                            .addOnSuccessListener(downloadUri -> {
                                Log.d(TAG, "Upload powiodl sie: " + downloadUri.toString());
                                saveExpenseToDatabase(merchantName, amount, category, date, downloadUri.toString());
                                deleteTemporaryImage();
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Nie udalo sie uzyskac URL", e);
                                // Zapisz bez url
                                saveExpenseToDatabase(merchantName, amount, category, date, null);
                                deleteTemporaryImage();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Upload nieudany", e);
                    Toast.makeText(this, "Błąd uploadu zdjęcia, zapisuję bez zdjęcia", Toast.LENGTH_SHORT).show();
                    // Zapisz bez url
                    saveExpenseToDatabase(merchantName, amount, category, date, null);
                    deleteTemporaryImage();
                });
    }

    private void saveExpenseToDatabase(String merchantName, double amount, String category, String date, String imageUrl) {
        Wydatki expense = new Wydatki(merchantName, amount, category, date, "PLN");

        expense.setReceiptImageUrl(imageUrl);
        expense.setFromReceipt(true);

        new Thread(() -> {
            try {
                database.DAOWydatki().insert(expense);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Wydatek z paragonu zapisany!", Toast.LENGTH_SHORT).show();

                    // Wróc
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                });

            } catch (Exception e) {
                Log.e(TAG, "Blad zapisu", e);
                runOnUiThread(() -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("💾 Zapisz wydatek");
                    Toast.makeText(this, "Błąd zapisywania wydatku", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void deleteTemporaryImage() {
        if (imageUriString != null) {
            try {
                Uri imageUri = Uri.parse(imageUriString);
                File imageFile = new File(imageUri.getPath());
                if (imageFile.exists()) {
                    boolean deleted = imageFile.delete();
                    Log.d(TAG, "Skasowano temp image: " + deleted);
                }
            } catch (Exception e) {
                Log.w(TAG, "Nie mozna bylo skasowac tymczasowego", e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up
        if (isFinishing()) {
            deleteTemporaryImage();
        }
    }
}