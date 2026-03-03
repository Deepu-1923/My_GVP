package com.example.mygvp;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
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
                        Glide.with(this).load(selectedImageUri).centerCrop().into(dialogImageViewPreview);
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Safe check for API Key
        try {
            IMGBB_API_KEY = getString(R.string.imgbb_api_key);
        } catch (Exception e) {
            IMGBB_API_KEY = "68c5b96439066601247065975d048450"; // Fallback if resource missing
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

        adapter = new LostItemAdapter(displayList, item -> showEditItemDialog(item));
        recyclerView.setAdapter(adapter);

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

        findViewById(R.id.fabAddItem).setOnClickListener(v -> showAddItemDialog());

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
                        // Skip malformed entries without crashing
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

    private void showAddItemDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_item);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        selectedImageUri = null;
        dialogImageViewPreview = dialog.findViewById(R.id.ivSelectedImage);
        Button btnSelectImage = dialog.findViewById(R.id.btnSelectImage);
        Button btnSubmitItem = dialog.findViewById(R.id.btnSubmitItem);
        EditText etItemTitle = dialog.findViewById(R.id.etItemTitle);
        EditText etItemMessage = dialog.findViewById(R.id.etItemMessage);
        MaterialButtonToggleGroup rgStatus = dialog.findViewById(R.id.rgStatus);

        btnSelectImage.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnSubmitItem.setOnClickListener(view -> {
            String title = etItemTitle.getText().toString().trim();
            String message = etItemMessage.getText().toString().trim();
            String status = (rgStatus.getCheckedButtonId() == R.id.rbFound) ? "FOUND" : "LOST";

            if (title.isEmpty() || message.isEmpty() || selectedImageUri == null) {
                Toast.makeText(this, "Please fill all fields and select an image!", Toast.LENGTH_SHORT).show();
                return;
            }

            android.content.SharedPreferences prefs = getSharedPreferences("MyGVP_UserPrefs", MODE_PRIVATE);
            String uploaderName = prefs.getString("LOGGED_IN_NAME", "Student");
            String uploaderRoll = prefs.getString("LOGGED_IN_ROLL_NO", "Unknown Roll");

            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Uploading...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            new Thread(() -> {
                String uploadedImageUrl = uploadImageToImgBB(selectedImageUri);
                runOnUiThread(() -> {
                    if (uploadedImageUrl != null) {
                        saveToFirebase(null, title, status, uploaderName, uploaderRoll, message, uploadedImageUrl, dialog, progressDialog);
                    } else {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Upload failed. Try again.", Toast.LENGTH_LONG).show();
                    }
                });
            }).start();
        });

        dialog.show();
    }

    private void showEditItemDialog(LostItem item) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_item);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        selectedImageUri = null;
        dialogImageViewPreview = dialog.findViewById(R.id.ivSelectedImage);
        Button btnSelectImage = dialog.findViewById(R.id.btnSelectImage);
        Button btnSubmitItem = dialog.findViewById(R.id.btnSubmitItem);
        EditText etItemTitle = dialog.findViewById(R.id.etItemTitle);
        EditText etItemMessage = dialog.findViewById(R.id.etItemMessage);
        MaterialButtonToggleGroup rgStatus = dialog.findViewById(R.id.rgStatus);

        btnSubmitItem.setText("Update Post");
        etItemTitle.setText(item.getTitle());
        etItemMessage.setText(item.getMessage());
        
        if ("FOUND".equals(item.getStatus())) {
            rgStatus.check(R.id.rbFound);
        } else {
            rgStatus.check(R.id.rbLost);
        }
        
        Glide.with(this).load(item.getImageUrl()).into(dialogImageViewPreview);

        btnSelectImage.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            imagePickerLauncher.launch(intent);
        });

        btnSubmitItem.setOnClickListener(view -> {
            String newTitle = etItemTitle.getText().toString().trim();
            String newMessage = etItemMessage.getText().toString().trim();
            String newStatus = (rgStatus.getCheckedButtonId() == R.id.rbFound) ? "FOUND" : "LOST";

            if (newTitle.isEmpty() || newMessage.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty!", Toast.LENGTH_SHORT).show();
                return;
            }

            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Updating...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            if (selectedImageUri != null) {
                new Thread(() -> {
                    String uploadedImageUrl = uploadImageToImgBB(selectedImageUri);
                    runOnUiThread(() -> {
                        if (uploadedImageUrl != null) {
                            saveToFirebase(item.getId(), newTitle, newStatus, item.getUploaderName(), item.getUploaderRoll(), newMessage, uploadedImageUrl, dialog, progressDialog);
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(this, "Image upload failed.", Toast.LENGTH_LONG).show();
                        }
                    });
                }).start();
            } else {
                saveToFirebase(item.getId(), newTitle, newStatus, item.getUploaderName(), item.getUploaderRoll(), newMessage, item.getImageUrl(), dialog, progressDialog);
            }
        });

        dialog.show();
    }

    private void saveToFirebase(String existingId, String title, String status, String uploaderName, String uploaderRoll, String message, String imageUrl, Dialog dialog, ProgressDialog progressDialog) {
        String pushId = (existingId != null) ? existingId : databaseReference.push().getKey();

        if (pushId != null) {
            long currentTime = System.currentTimeMillis();
            LostItem newItem = new LostItem(pushId, title, status, uploaderName, uploaderRoll, message, imageUrl, currentTime);

            databaseReference.child(pushId).setValue(newItem).addOnCompleteListener(task -> {
                progressDialog.dismiss();
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show();
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
