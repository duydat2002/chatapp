package com.example.chatbtl.activities;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.chatbtl.R;
import com.example.chatbtl.databinding.ActivityProfileBinding;
import com.example.chatbtl.models.User;
import com.example.chatbtl.utilities.Constants;
import com.example.chatbtl.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private ActivityProfileBinding binding;
    private PreferenceManager preferenceManager;
    private User otherUser;
    private String currentUserId;
    private String encodedImage;
    private String typeAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileBinding.inflate(getLayoutInflater());
        preferenceManager = new PreferenceManager(getApplicationContext());
        currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
        setContentView(binding.getRoot());
        loadUserDatas();
        setListeners();
        showToast("Create");
    }

    private void setListeners() {
        if (otherUser == null) {
            // Chủ tài khoản vào trang
            binding.buttonBack.setOnClickListener(v ->
                    startActivity(new Intent(getApplicationContext(), MainActivity.class)));
            binding.buttonMain.setImageResource(R.drawable.ic_exit);
            binding.buttonMain.setOnClickListener(v -> signOut());

            binding.buttonChangeInfo.setVisibility(View.VISIBLE);
            binding.buttonChangePassword.setVisibility(View.VISIBLE);
            binding.buttonChangeImage.setVisibility(View.VISIBLE);
        } else {
            // Khởi tạo nút lần đầu
            typeAction = otherUser.getFriendIds().contains(currentUserId) ? "Unfriend" : "Addfriend";
            setTypeAction(typeAction);

            //Người khác vào trang
            binding.buttonBack.setOnClickListener(v ->
                    startActivity(new Intent(getApplicationContext(), FriendsActivity.class)));

            binding.buttonChangeInfo.setVisibility(View.INVISIBLE);
            binding.buttonChangePassword.setVisibility(View.INVISIBLE);
            binding.buttonChangeImage.setVisibility(View.INVISIBLE);
        }

        binding.buttonChangeImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            pickImage.launch(intent);
        });
    }

    private void setTypeAction(String typeAction) {
        if (typeAction.equals("Unfriend")) {
            // Đã là bạn bè
            binding.buttonMain.setImageResource(R.drawable.ic_unfriend);
            binding.buttonMain.setOnClickListener(v -> unFriend(otherUser));
        } else {
            // Chưa phải bạn
            binding.buttonMain.setImageResource(R.drawable.ic_add_friend);
            binding.buttonMain.setOnClickListener(v -> addFriend(otherUser));
        }
    }

    private final ActivityResultLauncher<Intent> pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(imageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        binding.imageProfile.setImageBitmap(bitmap);
                        encodedImage = encodedImage(bitmap);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
    );

    private String encodedImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private void signOut() {
        showToast("Signing out...");

        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(currentUserId)
                .update(Constants.KEY_ONLINE, false)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Unable to sign out"));
    }

    private void unFriend(User otherUser) {
        List<String> friendIdList;
        String friendIds;
        FirebaseFirestore database = FirebaseFirestore.getInstance();

        // === Xóa bạn bên chủ ===
        friendIdList = new ArrayList<>(Arrays.asList(
                preferenceManager.getString(Constants.KEY_FRIEND_IDS).split("-")));
        friendIdList.remove(otherUser.getId());
        friendIds = friendIdList.toString();
        friendIds = friendIds.substring(1, friendIds.length() - 1).replaceAll(", ", "-");

        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(currentUserId)
                .update(Constants.KEY_FRIEND_IDS, friendIds);
        preferenceManager.putString(Constants.KEY_FRIEND_IDS, friendIds);


        // === Xóa bạn bên khách ===
        friendIdList = new ArrayList<>(Arrays.asList(
                otherUser.getFriendIds().split("-")));
        friendIdList.remove(currentUserId);
        friendIds = friendIdList.toString();
        friendIds = friendIds.substring(1, friendIds.length() - 1).replaceAll(", ", "-");

        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(otherUser.getId())
                .update(Constants.KEY_FRIEND_IDS, friendIds);
        otherUser.setFriendIds(friendIds);

        typeAction = "Addfriend";
        setTypeAction(typeAction);

        binding.buttonMain.setImageResource(R.drawable.ic_add_friend);
        showToast("Unfriended " + otherUser.getName());
    }

    private void addFriend(User otherUser) {
        List<String> friendIdList;
        String friendIds;
        FirebaseFirestore database = FirebaseFirestore.getInstance();

        // === Kết bạn bên chủ ===
        friendIdList = new ArrayList<>(Arrays.asList(
                preferenceManager.getString(Constants.KEY_FRIEND_IDS).split("-")));
        if (!friendIdList.contains(otherUser.getId()))
            friendIdList.add(otherUser.getId());
        friendIds = friendIdList.toString();
        friendIds = friendIds.substring(1, friendIds.length() - 1).replaceAll(", ", "-");

        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(currentUserId)
                .update(Constants.KEY_FRIEND_IDS, friendIds);

        // === Kết bạn bên khách ===
        friendIdList = new ArrayList<>(Arrays.asList(
               otherUser.getFriendIds().split("-")));
        if (!friendIdList.contains(currentUserId))
            friendIdList.add(currentUserId);
        friendIds = friendIdList.toString();
        friendIds = friendIds.substring(1, friendIds.length() - 1).replaceAll(", ", "-");

        database.collection(Constants.KEY_COLLECTION_USERS)
                .document(otherUser.getId())
                .update(Constants.KEY_FRIEND_IDS, friendIds);
        otherUser.setFriendIds(friendIds);

        typeAction = "Unfriend";
        setTypeAction(typeAction);

        binding.buttonMain.setImageResource(R.drawable.ic_unfriend);
        showToast("Befriended " + otherUser.getName());
    }

    private void loadUserDatas() {
        otherUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        String name, phone, image;

        if (otherUser != null) {
            name = otherUser.getName();
            phone = otherUser.getPhone();
            image = otherUser.getImage();
        } else {
            name = preferenceManager.getString(Constants.KEY_NAME);
            phone = preferenceManager.getString(Constants.KEY_PHONE);
            image = preferenceManager.getString(Constants.KEY_IMAGE);
        }

        byte[] bytes = Base64.decode(image, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
        binding.textName.setText(name);
        binding.textPhone.setText(phone);

    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }
}