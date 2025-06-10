package com.example.trackerwydatkow;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class ScanReceiptActivity extends AppCompatActivity {
    private static final String TAG = "ScanReceiptActivity";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA
            //Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private PreviewView previewView;
    private Button btnCapture, btnBack, btnFlash;
    private LinearLayout layoutLoading;
    private TextView textLoadingMessage;

    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private Camera camera;
    private boolean isFlashOn = false;

    private TextRecognizer textRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scan_receipt);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.layoutControls), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Log.d(TAG, "ScanReceiptActivity onCreate start");

        initializeViews();
        initializeMLKit();

        /*
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }*/
        checkAndRequestPermissions();
    }

    private void initializeViews() {
        previewView = findViewById(R.id.previewView);
        btnCapture = findViewById(R.id.btnCapture);
        btnBack = findViewById(R.id.btnBack);
        btnFlash = findViewById(R.id.btnFlash);
        layoutLoading = findViewById(R.id.layoutLoading);
        textLoadingMessage = findViewById(R.id.textLoadingMessage);

        btnCapture.setOnClickListener(v -> takePhoto());
        btnBack.setOnClickListener(v -> finish());
        btnFlash.setOnClickListener(v -> toggleFlash());
    }

    private void initializeMLKit() {
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        Log.d(TAG, "ML Kit initialized");
    }

    /*
    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;

     */
    private void checkAndRequestPermissions() {
        Log.d(TAG, "Checking permissions...");

        for (String permission : REQUIRED_PERMISSIONS) {
            int permissionStatus = ContextCompat.checkSelfPermission(this, permission);
            Log.d(TAG, "Permission " + permission + " status: " +
                    (permissionStatus == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED"));
        }

        if (allPermissionsGranted()) {
            Log.d(TAG, "All permissions granted, starting camera");
            startCamera();
        } else {
            Log.d(TAG, "Requesting permissions");
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission not granted: " + permission);
                return false;
            }
        }
        Log.d(TAG, "All permissions are granted");
        return true;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Inicjacja kamery - blad", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build();

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture);
        } catch (Exception e) {
            Log.e(TAG, "Kamera niepowiązana", e);
        }
    }

    private void takePhoto() {
        if (imageCapture == null) {
            Log.e(TAG, "ImageCapture jest null");
            return;
        }

        showLoading(true, "Robienie zdjęcia...");

        // Twórz plik
        File photoFile = new File(
                getExternalFilesDir(null),
                "receipt_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        .format(new Date()) + ".jpg"
        );

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Log.d(TAG, "Photo zapisane: " + photoFile.getAbsolutePath());
                        processReceiptImage(Uri.fromFile(photoFile));
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Foto nie dzziała", exception);
                        showLoading(false, "");
                        Toast.makeText(ScanReceiptActivity.this,
                                "Błąd podczas robienia zdjęcia", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void processReceiptImage(Uri imageUri) {
        textLoadingMessage.setText("Rozpoznawanie tekstu...");

        try {
            InputImage image = InputImage.fromFilePath(this, imageUri);

            textRecognizer.process(image)
                    .addOnSuccessListener(text -> {
                        showLoading(false, "");
                        handleOCRResult(text, imageUri);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Blad rozpoznawania tekstu", e);
                        showLoading(false, "");
                        Toast.makeText(this, "Błąd rozpoznawania tekstu", Toast.LENGTH_SHORT).show();
                    });

        } catch (IOException e) {
            Log.e(TAG, "Błąd procesowania obrazu", e);
            showLoading(false, "");
            Toast.makeText(this, "Błąd przetwarzania zdjęcia", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleOCRResult(Text result, Uri imageUri) {
        String extractedText = result.getText();
        Log.d(TAG, "Extracted text: " + extractedText);

        if (extractedText.trim().isEmpty()) {
            Toast.makeText(this, "Nie znaleziono tekstu na paragonie", Toast.LENGTH_LONG).show();
            return;
        }

        // Parsuj dane z dokumentu
        ReceiptData receiptData = parseReceiptText(extractedText);

        // Powrót
        Intent intent = new Intent(this, ReceiptPreviewActivity.class);
        intent.putExtra("image_uri", imageUri.toString());
        intent.putExtra("extracted_text", extractedText);
        intent.putExtra("parsed_amount", receiptData.amount);
        intent.putExtra("parsed_merchant", receiptData.merchantName);
        intent.putExtra("parsed_date", receiptData.date);
        startActivity(intent);
    }

    private ReceiptData parseReceiptText(String text) {
        ReceiptData data = new ReceiptData();
        String[] lines = text.split("\n");

        // Szukaj wzorców: "23,45", "123.45", "RAZEM 45,67"
        for (String line : lines) {
            String cleanLine = line.trim().toUpperCase();

            // SZukaj pełnej kwoty
            if (cleanLine.contains("RAZEM") || cleanLine.contains("SUMA") ||
                    cleanLine.contains("TOTAL") || cleanLine.contains("DO ZAPŁATY")) {

                String amount = extractAmountFromLine(line);
                if (amount != null && !amount.isEmpty()) {
                    try {
                        data.amount = Double.parseDouble(amount.replace(",", "."));
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Nie można sparsować kwoty: " + amount);
                    }
                    break;
                }
            }
        }

        // Jeśli nie ma całkowitej kwoty, to szukaj najbliższej
        if (data.amount == 0.0) {
            String amount = extractAmountFromText(text);
            if (amount != null) {
                try {
                    data.amount = Double.parseDouble(amount.replace(",", "."));
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Nie można sparsować - fallback: " + amount);
                }
            }
        }

        // Parsowanie nazw firm
        for (int i = 0; i < Math.min(lines.length, 5); i++) {
            String line = lines[i].trim();
            if (line.length() > 3 && !line.matches(".*\\d{4}-\\d{2}-\\d{2}.*") &&
                    !line.matches(".*\\d{2}[:\\.]\\d{2}.*")) {
                data.merchantName = line;
                break;
            }
        }

        // Pars dat
        data.date = extractDateFromText(text);

        Log.d(TAG, "Parsed receipt data: " + data.toString());
        return data;
    }

    private String extractAmountFromLine(String line) {
        // Regex do wyników
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+[,\\.]\\d{2})");
        java.util.regex.Matcher matcher = pattern.matcher(line);

        String lastAmount = null;
        while (matcher.find()) {
            lastAmount = matcher.group(1);
        }
        return lastAmount;
    }

    private String extractAmountFromText(String text) {
        // Regex do wzorców
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+[,\\.]\\d{2})");
        java.util.regex.Matcher matcher = pattern.matcher(text);

        double maxAmount = 0.0;
        String result = null;

        while (matcher.find()) {
            String amountStr = matcher.group(1);
            try {
                double amount = Double.parseDouble(amountStr.replace(",", "."));
                if (amount > maxAmount) {
                    maxAmount = amount;
                    result = amountStr;
                }
            } catch (NumberFormatException e) {
                // Ignoruj błędne
            }
        }
        return result;
    }

    private String extractDateFromText(String text) {
        // Wzorce dat 2025-01-15, 15.01.2025, 15/01/2025
        java.util.regex.Pattern[] patterns = {
                java.util.regex.Pattern.compile("(\\d{4}-\\d{2}-\\d{2})"),
                java.util.regex.Pattern.compile("(\\d{2}[\\./]\\d{2}[\\./]\\d{4})"),
                java.util.regex.Pattern.compile("(\\d{2}[\\./]\\d{2}[\\./]\\d{2})")
        };

        for (java.util.regex.Pattern pattern : patterns) {
            java.util.regex.Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return formatDate(matcher.group(1));
            }
        }

        // Domyślnie dzisiejsza
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private String formatDate(String dateStr) {
        // Konwersja do yyyy-MM-dd
        try {
            if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                return dateStr; // Already in correct format
            } else if (dateStr.matches("\\d{2}[\\./]\\d{2}[\\./]\\d{4}")) {
                String[] parts = dateStr.split("[\\./]");
                return parts[2] + "-" + parts[1] + "-" + parts[0];
            } else if (dateStr.matches("\\d{2}[\\./]\\d{2}[\\./]\\d{2}")) {
                String[] parts = dateStr.split("[\\./]");
                return "20" + parts[2] + "-" + parts[1] + "-" + parts[0];
            }
        } catch (Exception e) {
            Log.w(TAG, "Date parsing failed for: " + dateStr);
        }

        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private void toggleFlash() {
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            isFlashOn = !isFlashOn;
            camera.getCameraControl().enableTorch(isFlashOn);
            btnFlash.setText(isFlashOn ? "🔆" : "💡");
        }
    }

    private void showLoading(boolean show, String message) {
        runOnUiThread(() -> {
            layoutLoading.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show && !message.isEmpty()) {
                textLoadingMessage.setText(message);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Aplikacja potrzebuje uprawnień do aparatu", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (textRecognizer != null) {
            textRecognizer.close();
        }
    }

    // Zapis
    public static class ReceiptData {
        public double amount = 0.0;
        public String merchantName = "";
        public String date = "";

        @Override
        public String toString() {
            return "ReceiptData{amount=" + amount + ", merchant='" + merchantName +
                    "', date='" + date + "'}";
        }
    }
}