package com.example.chatbtl.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.example.chatbtl.R;
import com.example.chatbtl.adapters.ChatAdapter;
import com.example.chatbtl.databinding.ActivityChatBinding;
import com.example.chatbtl.models.ChatMessage;
import com.example.chatbtl.models.User;
import com.example.chatbtl.utilities.Constants;
import com.example.chatbtl.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private User receiverUser;
    private String senderId, conversionId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        loadReceiverDatas();
        initAll();
        setListeners();
        listenMessages();
    }

    private void loadReceiverDatas() {
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(receiverUser.getName());
//        if (receiverUser.getOnline()) {
//            binding.textActive.setText("Online");
//            binding.textActive.setTextColor(getResources().getColor(R.color.online));
//        } else {
//            binding.textActive.setText("Offline");
//            binding.textActive.setTextColor(getResources().getColor(R.color.offline));
//        }

        byte[] bytes = Base64.decode(receiverUser.getImage(), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);
    }

    private void initAll() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        senderId = preferenceManager.getString(Constants.KEY_USER_ID);
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages, senderId, receiverUser);
        binding.recyclerMessage.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
        getConversionId();
    }

    private void setListeners() {
        binding.buttonBack.setOnClickListener(v -> onBackPressed());
        binding.buttonSent.setOnClickListener(v -> {
            if (!binding.inputMessage.getText().toString().trim().isEmpty())
                sentMessage();
        });

        binding.textName.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), ProfileActivity.class);
            intent.putExtra(Constants.KEY_USER, receiverUser);
            startActivity(intent);
        });
    }

    private void sentMessage() {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, senderId);
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.getId());
        message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString().trim());
        message.put(Constants.KEY_TIMESTAMP, new Date());
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .add(message);

        getConversionId();
        Toast.makeText(getApplicationContext(), conversionId, Toast.LENGTH_LONG).show();

        if (conversionId != null) {
            updateConversion(binding.inputMessage.getText().toString());
        } else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constants.KEY_SENDER_ID, senderId);
            conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.getId());
            conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.getName());
            conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.getImage());
            conversion.put(Constants.KEY_LAST_MESSAGE, binding.inputMessage.getText().toString().trim());
            conversion.put(Constants.KEY_TIMESTAMP, new Date());
            addConversion(conversion);
        }

        binding.inputMessage.setText(null);
    }

    private void getConversionId() {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.getId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        conversionId = documentSnapshot.getId();
                    }
                });

        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, senderId)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.getId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        conversionId = documentSnapshot.getId();
                    }
                });
    }

    private void addConversion(HashMap<String, Object> conversion) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversionId = documentReference.getId());
    }

    private void updateConversion(String message) {
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_LAST_MESSAGE, message);
        updates.put(Constants.KEY_TIMESTAMP, new Date());
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .document(conversionId)
                .update(updates);
    }

    // Theo dõi thay đổi của dữ liệu chat
    private void listenMessages() {
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.getId())
                .addSnapshotListener(snapshotEventListener);

        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.getId())
                .whereEqualTo(Constants.KEY_RECEIVER_ID, senderId)
                .addSnapshotListener(snapshotEventListener);
    }

    private final EventListener<QuerySnapshot> snapshotEventListener = (value, error) -> {
        if (error != null)
            return;

        int count = chatMessages.size();
        for (DocumentChange documentChange : value.getDocumentChanges()) {
            QueryDocumentSnapshot queryDocumentSnapshot = documentChange.getDocument();
            if (documentChange.getType() == DocumentChange.Type.ADDED) {
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setChatId(queryDocumentSnapshot.getId());
                chatMessage.setSenderId(queryDocumentSnapshot.getString(Constants.KEY_SENDER_ID));
                chatMessage.setReceiverId(queryDocumentSnapshot.getString(Constants.KEY_RECEIVER_ID));
                chatMessage.setMessage(queryDocumentSnapshot.getString(Constants.KEY_MESSAGE));
                chatMessage.setTimeStamp(convertDate(queryDocumentSnapshot.getDate(Constants.KEY_TIMESTAMP)));
                chatMessage.setDateObj(queryDocumentSnapshot.getDate(Constants.KEY_TIMESTAMP));
                chatMessages.add(chatMessage);
            }
        }
        Collections.sort(chatMessages, (message1, message2) -> message1.getDateObj().compareTo(message2.getDateObj()));

        if (count == 0) {
            // Tin nhắn đầu tiên
            chatAdapter.notifyDataSetChanged();
        } else {
            // Tin nhắn tiếp theo
            chatAdapter.notifyItemRangeChanged(chatMessages.size(), chatMessages.size());
            binding.recyclerMessage.smoothScrollToPosition(chatMessages.size() - 1);
        }

        binding.recyclerMessage.setVisibility(View.VISIBLE);

        binding.progressBar.setVisibility(View.GONE);
    };

    private String convertDate(Date date) {
        return new SimpleDateFormat("HH:mm, dd/MM/yyyy", Locale.getDefault()).format(date);
    }


}