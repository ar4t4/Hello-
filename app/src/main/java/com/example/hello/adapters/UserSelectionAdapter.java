package com.example.hello.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.hello.R;
import com.example.hello.models.User;
import com.google.android.material.imageview.ShapeableImageView;
import java.util.List;

public class UserSelectionAdapter extends RecyclerView.Adapter<UserSelectionAdapter.UserViewHolder> {
    private Context context;
    private List<User> users;

    public UserSelectionAdapter(Context context, List<User> users) {
        this.context = context;
        this.users = users;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user_selection, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.userName.setText(user.getName());
        holder.userEmail.setText(user.getEmail());
        
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                users.add(user);
            } else {
                users.remove(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView userImage;
        TextView userName, userEmail;
        CheckBox checkBox;

        UserViewHolder(View itemView) {
            super(itemView);
            userImage = itemView.findViewById(R.id.userImage);
            userName = itemView.findViewById(R.id.userName);
            userEmail = itemView.findViewById(R.id.userEmail);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }
} 