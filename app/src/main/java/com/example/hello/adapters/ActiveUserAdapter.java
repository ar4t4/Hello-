package com.example.hello.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.hello.R;
import com.example.hello.helpers.CloudinaryHelper;
import com.example.hello.models.User;
import de.hdodenhof.circleimageview.CircleImageView;
import java.util.List;

public class ActiveUserAdapter extends RecyclerView.Adapter<ActiveUserAdapter.ActiveUserViewHolder> {
    private Context context;
    private List<User> activeUsers;
    private OnActiveUserClickListener clickListener;

    public interface OnActiveUserClickListener {
        void onActiveUserClick(User user);
    }

    public ActiveUserAdapter(Context context, List<User> activeUsers, OnActiveUserClickListener clickListener) {
        this.context = context;
        this.activeUsers = activeUsers;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ActiveUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_active_user, parent, false);
        return new ActiveUserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActiveUserViewHolder holder, int position) {
        User user = activeUsers.get(position);
        
        // Set user name - first name only
        String firstName = user.getFirstName();
        holder.userName.setText(firstName != null && !firstName.isEmpty() ? firstName : "User");
        
        // Load user profile image
        if (user.getImageUrl() != null && !user.getImageUrl().isEmpty()) {
            CloudinaryHelper.loadImage(context, user.getImageUrl(), holder.userImage);
        } else {
            holder.userImage.setImageResource(R.drawable.profile_placeholder);
        }

        // Handle click events
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onActiveUserClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return activeUsers.size();
    }

    static class ActiveUserViewHolder extends RecyclerView.ViewHolder {
        CircleImageView userImage;
        TextView userName;
        View activeIndicator;

        ActiveUserViewHolder(View itemView) {
            super(itemView);
            userImage = itemView.findViewById(R.id.activeUserImage);
            userName = itemView.findViewById(R.id.activeUserName);
            activeIndicator = itemView.findViewById(R.id.activeIndicator);
        }
    }
} 