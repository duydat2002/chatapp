package com.example.chatbtl.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatbtl.R;
import com.example.chatbtl.databinding.ItemUserBinding;
import com.example.chatbtl.models.User;

public class TestAdapter {



    public class TestViewHolder extends RecyclerView.ViewHolder {
        private final ItemUserBinding binding;

        TestViewHolder(ItemUserBinding itemUserBinding) {
            super(itemUserBinding.getRoot());
            binding = itemUserBinding;
        }

        void setData(User user) {
            binding.textName.setText("123");
            byte[] bytes = Base64.decode(user.getImage(), Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            binding.imageProfile.setImageBitmap(bitmap);
        }
    }
}
