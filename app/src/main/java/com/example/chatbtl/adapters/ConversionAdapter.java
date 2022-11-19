package com.example.chatbtl.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatbtl.R;
import com.example.chatbtl.databinding.ItemConversionBinding;
import com.example.chatbtl.interfaces.ConversionInterface;
import com.example.chatbtl.models.ChatMessage;
import com.example.chatbtl.models.User;
import com.example.chatbtl.utilities.Constants;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConversionAdapter extends RecyclerView.Adapter<ConversionAdapter.ConversionViewHolder> {

    private List<ChatMessage> chatMessages;
    private ConversionInterface conversionInterface;

    public ConversionAdapter(List<ChatMessage> chatMessages, ConversionInterface conversionInterface) {
        this.chatMessages = chatMessages;
        this.conversionInterface = conversionInterface;
    }

    @NonNull
    @Override
    public ConversionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversionViewHolder(
                ItemConversionBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ConversionViewHolder holder, int position) {
        holder.setData(chatMessages.get(position));
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }


    public class ConversionViewHolder extends RecyclerView.ViewHolder {

        ItemConversionBinding binding;

        ConversionViewHolder(ItemConversionBinding itemConversionBinding) {
            super(itemConversionBinding.getRoot());
            binding = itemConversionBinding;
        }

        void setData(ChatMessage chatMessages) {
            byte[] bytes = Base64.decode(chatMessages.getConversionImage(), Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            binding.imageProfile.setImageBitmap(bitmap);
            binding.textName.setText(chatMessages.getConversionName());
            binding.textNewMessage.setText(chatMessages.getMessage());
            binding.textTimeMessage.setText(convertDate(chatMessages.getDateObj()));
//            binding.viewOnline.setBackgroundTintList(
//                ContextCompat.getColorStateList(binding.getRoot().getContext(),
//                        chatMessages.getOnline() ?
//                        R.color.online :
//                        R.color.offline
//                ));

            binding.getRoot().setOnClickListener(v -> {
                User user = new User();
                user.setId(chatMessages.getConversionId());
                user.setName(chatMessages.getConversionName());
                user.setImage(chatMessages.getConversionImage());

                conversionInterface.onConversionClicked(user);
            });
        }
    }

    private String convertDate(Date date) {
        String pattern = "HH:mm, dd/MM/yyyy";
        int day = date.getDay(),
            month = date.getMonth(),
            year = date.getYear();
        Date curDate = new Date();
        if (day == curDate.getDay() && month == curDate.getMonth() && year == curDate.getYear()) {
            pattern = "HH:mm";
        } else if (date.getYear() == new Date().getYear()) {
            pattern = "HH:mm, dd/MM";
        }
        return new SimpleDateFormat(pattern).format(date);
    }
}
