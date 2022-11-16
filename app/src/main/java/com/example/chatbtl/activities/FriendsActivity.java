package com.example.chatbtl.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import com.example.chatbtl.adapters.UserAdapter;
import com.example.chatbtl.databinding.ActivityFriendsBinding;
import com.example.chatbtl.interfaces.UserInterface;
import com.example.chatbtl.models.User;
import com.example.chatbtl.utilities.Constants;
import com.example.chatbtl.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FriendsActivity extends AppCompatActivity implements UserInterface {

    private ActivityFriendsBinding binding;
    private PreferenceManager preferenceManager;
    private List<User> friends;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFriendsBinding.inflate(getLayoutInflater());
        preferenceManager = new PreferenceManager(getApplicationContext());
        currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
        friends = new ArrayList<>();
        setContentView(binding.getRoot());
        loadFriends();
        setListeners();
    }

    private void loadFriends() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);

                    if (task.isSuccessful() && task.getResult() != null) {
                        friends = new ArrayList<>();

                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            if (queryDocumentSnapshot.getString(Constants.KEY_FRIEND_IDS)
                                    .contains(currentUserId)) {
                                User user = new User();
                                user.setId(queryDocumentSnapshot.getId());
                                user.setName(queryDocumentSnapshot.getString(Constants.KEY_NAME));
                                user.setPhone(queryDocumentSnapshot.getString(Constants.KEY_PHONE));
                                user.setImage(queryDocumentSnapshot.getString(Constants.KEY_IMAGE));
                                user.setFriendIds(queryDocumentSnapshot.getString(Constants.KEY_FRIEND_IDS));
                                user.setOnline(queryDocumentSnapshot.getBoolean(Constants.KEY_ONLINE));
                                friends.add(user);
                            }
                        }

                        if (friends.size() > 0) {
                            UserAdapter userAdapter = new UserAdapter(friends, this);
                            binding.recyclerPeople.setAdapter(userAdapter);
                            binding.recyclerPeople.setVisibility(View.VISIBLE);
                        } else {
                            showToast("Cac 0 tim thay");
                        }
                    } else {
                        showToast("Lỗi r dm");
                    }
                });
    }

    private void setListeners() {
        binding.buttonBack.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), MainActivity.class)));
        binding.buttonMessages.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), MainActivity.class)));

        binding.inputSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
                binding.buttonCancel.setVisibility(View.VISIBLE);
        });
        binding.inputSearch.setOnKeyListener((v, keyCode, keyEvent) -> {
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                searchFriends(friends, binding.inputSearch.getText().toString());
                return true;
            }
            return false;
        });

        binding.buttonAddNewFriend.setOnClickListener(v -> {
            binding.inputSearch.setVisibility(View.INVISIBLE);
            binding.inputAddFriend.setVisibility(View.VISIBLE);
        });

        binding.inputAddFriend.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus)
                binding.buttonCancel.setVisibility(View.VISIBLE);
        });
        binding.inputAddFriend.setOnKeyListener((v, keyCode, keyEvent) -> {
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                searchPeoples(binding.inputAddFriend.getText().toString());
                return true;
            }
            return false;
        });

        binding.buttonCancel.setOnClickListener(v -> {
            binding.buttonCancel.setVisibility(View.INVISIBLE);
            binding.inputSearch.setText("");
            binding.inputSearch.clearFocus();
            if (friends.size() > 0) {
                UserAdapter userAdapter = new UserAdapter(friends, this);
                binding.recyclerPeople.setAdapter(userAdapter);
                binding.recyclerPeople.setVisibility(View.VISIBLE);
            } else {
                showToast("Cac 0 tim thay");
            }

            if (binding.inputAddFriend.getVisibility() == View.VISIBLE) {
                binding.inputSearch.setVisibility(View.VISIBLE);
                binding.inputAddFriend.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void searchFriends(List<User> friends, String phone) {
        List<User> friendSearches = new ArrayList<>();
        for (User friend : friends) {
            if (friend.getPhone().contains(phone)) {
                friendSearches.add(friend);
            }
        }
        UserAdapter userAdapter = new UserAdapter(friendSearches, this);
        binding.recyclerPeople.setAdapter(userAdapter);
        binding.recyclerPeople.setVisibility(View.VISIBLE);
    }

    private void searchPeoples(String phone) {
        loading(true);
        List<User> peopleSearches = new ArrayList<>();
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_PHONE, phone)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);

                    if (task.isSuccessful() && task.getResult() != null && task.getResult().size() > 0) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        User user = new User();
                        user.setId(documentSnapshot.getId());
                        user.setName(documentSnapshot.getString(Constants.KEY_NAME));
                        user.setPhone(documentSnapshot.getString(Constants.KEY_PHONE));
                        user.setImage(documentSnapshot.getString(Constants.KEY_IMAGE));
                        user.setFriendIds(documentSnapshot.getString(Constants.KEY_FRIEND_IDS));
                        user.setOnline(documentSnapshot.getBoolean(Constants.KEY_ONLINE));
                        peopleSearches.add(user);
                    } else {
                        showToast("Không tìm thấy");
                    }
                    UserAdapter userAdapter = new UserAdapter(peopleSearches, this);
                    binding.recyclerPeople.setAdapter(userAdapter);
                    binding.recyclerPeople.setVisibility(View.VISIBLE);
                });
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onUserClicked(User user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
        finish();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFriends();
    }

    //    private void showErrorMessage() {
//        binding.textErrorMessage.setText(String.format("%s", "No user available"));
//        binding.textErrorMessage.setVisibility(View.VISIBLE);
//    }
}