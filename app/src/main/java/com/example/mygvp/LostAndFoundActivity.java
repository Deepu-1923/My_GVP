package com.example.mygvp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class LostAndFoundActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LostItemAdapter adapter;

    private List<LostItem> masterList;
    private List<LostItem> displayList;

    private Uri selectedImageUri = null;
    private ImageView dialogImageViewPreview;
    private DatabaseReference databaseReference;
    private String IMGBB_API_KEY;

    private final ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (dialogImageViewPreview != null) {
                        dialogImageViewPreview.setPadding(0, 0, 0, 0);
                        dialogImageViewPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        Glide.with(this).load(selectedImageUri).into(dialogImageViewPreview);
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    if (imageBitmap != null) {
                        selectedImageUri = getImageUri(imageBitmap);
                        if (dialogImageViewPreview != null) {
                            dialogImageViewPreview.setPadding(0, 0, 0, 0);
                            dialogImageViewPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            Glide.with(this).load(selectedImageUri).into(dialogImageViewPreview);
                        }
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            IMGBB_API_KEY = getString(R.string.imgbb_api_key);
        } catch (Exception e) {
            IMGBB_API_KEY = "68c5b96439066601247065975d048450"; 
        }
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lost_found);

        databaseReference = FirebaseDatabase.getInstance().getReference("LostAndFound");

        recyclerView = findViewById(R.id.recyclerViewLostFound);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);

        masterList = new ArrayList<>();
        displayList = new ArrayList<>();

        adapter = new LostItemAdapter(displayList, item -> showItemBottomSheet(item));
        recyclerView.setAdapter(adapter);

        // Navigation
        View navBack = findViewById(R.id.toolbar);
        if (navBack != null) {
            navBack.setOnClickListener(v -> finish());
        }
        
        ChipGroup chipGroup = findViewById(R.id.chipGroupFilter);
        if (chipGroup != null) {
            chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (!checkedIds.isEmpty()) {
                    int checkedId = checkedIds.get(0);
                    if (checkedId == R.id.chipAll) filterFeed("ALL");
                    else if (checkedId == R.id.chipLost) filterFeed("LOST");
                    else if (checkedId == R.id.chipFound) filterFeed("FOUND");
                }
            });
        }

        findViewById(R.id.fabAddItem).setOnClickListener(v -> showItemBottomSheet(null));

        loadFeedFromFirebase();
    }

    private void loadFeedFromFirebase() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                masterList.clear();
                long currentTime = System.currentTimeMillis();
                long threeDaysInMillis = 3L * 24 * 60 * 60 * 1000;

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    try {
                        LostItem item = dataSnapshot.getValue(LostItem.class);
                        if (item != null && item.getId() != null) {
                            String status = item.getStatus() != null ? item.getStatus() : "LOST";
                            boolean isResolved = status.equals("RESOLVED") || status.equals("CLAIMED");
                            boolean isOlderThan3Days = (currentTime - item.getTimestamp()) > threeDaysInMillis;

                            if (isResolved && isOlderThan3Days) continue;
                            masterList.add(item);
                        }
                    } catch (Exception e) {
                        // Skip corrupted data
                    }
                }
                updateCurrentFilter();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(LostAndFoundActivity.this, "Failed to load feed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCurrentFilter() {
        ChipGroup chipGroup = findViewById(R.id.chipGroupFilter);
        if (chipGroup == null) {
            filterFeed("ALL");
            return;
        }
        int checkedId = chipGroup.getCheckedChipId();
        if (checkedId == R.id.chipAll) filterFeed("ALL");
        else if (checkedId == R.id.chipLost) filterFeed("LOST");
        else if (checkedId == R.id.chipFound) filterFeed("FOUND");
        else filterFeed("ALL");
    }

    private void filterFeed(String filterType) {
        displayList.clear();
        for (LostItem item : masterList) {
            String status = item.getStatus() != null ? item.getStatus() : "";
            if (filterType.equals("ALL")) {
                displayList.add(item);
            } else if (filterType.equals("LOST") && (status.equals("LOST") || status.equals("RESOLVED"))) {
                displayList.add(item);
            } else if (filterType.equals("FOUND") && (status.equals("FOUND") || status.equals("CLAIMED"))) {
                displayList.add(item);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showItemBottomSheet(LostItem existingItem) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_item, null);
        bottomSheetDialog.setContentView(view);

        selectedImageUri = null;
        dialogImageViewPreview = view.findViewById(R.id.ivSelectedImage);
        View cardImagePreview = view.findViewById(R.id.cardImagePreview);
        MaterialButton btnSelectImage = view.findViewById(R.id.btnSelectImage);
        MaterialButton btnSubmitItem = view.findViewById(R.id.btnSubmitItem);
        EditText etItemTitle = view.findViewById(R.id.etItemTitle);
        EditText etItemMessage = view.findViewById(R.id.etItemMessage);
        MaterialButtonToggleGroup rgStatus = view.findViewById(R.id.rgStatus);

        if (existingItem != null) {
            btnSubmitItem.setText("Update Post");
            etItemTitle.setText(existingItem.getTitle());
            etItemMessage.setText(existingItem.getMessage());
            if ("FOUND".equals(existingItem.getStatus())) rgStatus.check(R.id.rbFound);
            else rgStatus.check(R.id.rbLost);
            Glide.with(this).load(existingItem.getImageUrl()).into(dialogImageViewPreview);
            if (dialogImageViewPreview != null) {
                dialogImageViewPreview.setPadding(0, 0, 0, 0);
                dialogImageViewPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            }
        }

        if (cardImagePreview != null) {
            cardImagePreview.setOnClickListener(v -> {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraLauncher.launch(takePictureIntent);
            });
        }

        btnSelectImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnSubmitItem.setOnClickListener(v -> {
            String title = etItemTitle.getText().toString().trim();
            String message = etItemMessage.getText().toString().trim();
            String status = (rgStatus.getCheckedButtonId() == R.id.rbFound) ? "FOUND" : "LOST";

            if (title.isEmpty() || message.isEmpty() || (existingItem == null && selectedImageUri == null)) {
                Toast.makeText(this, "Please fill all fields and select an image!", Toast.LENGTH_SHORT).show();
                return;
            }

            android.content.SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
            String uploaderName = prefs.getString("LOGGED_IN_NAME", "Student");
            String uploaderRoll = prefs.getString("LOGGED_IN_ROLL_NO", "Unknown Roll");

            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(existingItem == null ? "Uploading..." : "Updating...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            new Thread(() -> {
                String finalImageUrl = existingItem != null ? existingItem.getImageUrl() : null;
                if (selectedImageUri != null) {
                    finalImageUrl = uploadImageToImgBB(selectedImageUri);
                }

                String finalUrl = finalImageUrl;
                runOnUiThread(() -> {
                    if (finalUrl != null) {
                        saveToFirebase(existingItem != null ? existingItem.getId() : null, title, status, uploaderName, uploaderRoll, message, finalUrl, bottomSheetDialog, progressDialog);
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Upload failed. Try again.", Toast.LENGTH_LONG).show();
                    }
                });
            }).start();
        });

        bottomSheetDialog.show();
    }

    private Uri getImageUri(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "CapturedItem_" + System.currentTimeMillis(), null);
        return Uri.parse(path);
    }

    private void saveToFirebase(String id, String title, String status, String uploaderName, String uploaderRoll, String message, String imageUrl, BottomSheetDialog dialog, ProgressDialog progressDialog) {
        String pushId = (id != null) ? id : databaseReference.push().getKey();
        if (pushId != null) {
            long currentTime = System.currentTimeMillis();
            LostItem newItem = new LostItem(pushId, title, status, uploaderName, uploaderRoll, message, imageUrl, currentTime);
            databaseReference.child(pushId).setValue(newItem).addOnCompleteListener(task -> {
                progressDialog.dismiss();
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Announcement Shared!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, "Firebase error.", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private String uploadImageToImgBB(Uri imageUri) {
        try {
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = imageStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            String base64Image = Base64.encodeToString(byteBuffer.toByteArray(), Base64.DEFAULT);
            String urlParameters = "key=" + IMGBB_API_KEY + "&image=" + URLEncoder.encode(base64Image, "UTF-8");

            URL url = new URL("https://api.imgbb.com/1/upload");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            os.write(urlParameters.getBytes());
            os.flush();
            os.close();

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream is = conn.getInputStream();
                java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
                String response = s.hasNext() ? s.next() : "";
                JSONObject jsonObject = new JSONObject(response);
                return jsonObject.getJSONObject("data").getString("url");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
