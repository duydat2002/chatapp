package com.example.chatbtl.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatbtl.databinding.ItemConversionBinding;
import com.example.chatbtl.interfaces.ConversionInterface;
import com.example.chatbtl.models.ChatMessage;
import com.example.chatbtl.models.User;

import java.util.List;

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
            binding.getRoot().setOnClickListener(v -> {
                User user = new User();
                user.setId(chatMessages.getConversionId());
                user.setName(chatMessages.getConversionName());
                user.setImage(chatMessages.getConversionImage());
                conversionInterface.onConversionClicked(user);
            });
        }

    }
}
