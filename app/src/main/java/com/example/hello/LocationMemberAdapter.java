package com.example.hello;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hello.helpers.CloudinaryHelper;

import java.util.List;

public class LocationMemberAdapter extends RecyclerView.Adapter<LocationMemberAdapter.LocationViewHolder> {

    private List<Member> members;
    private Context context;
    private OnCallButtonClickListener callButtonClickListener;

    public interface OnCallButtonClickListener {
        void onCallButtonClick(String phoneNumber);
    }

    public LocationMemberAdapter(Context context, List<Member> members, OnCallButtonClickListener listener) {
        this.context = context;
        this.members = members;
        this.callButtonClickListener = listener;
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_member_location_item, parent, false);
        return new LocationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        Member member = members.get(position);
        
        // Set member info
        holder.memberName.setText(member.getFirstName());
        holder.memberCollege.setText(member.getCollege() != null ? member.getCollege() : "Institution not specified");
        holder.bloodGroup.setText(member.getBloodGroup() != null ? member.getBloodGroup() : "N/A");
        
        // Set donation status
        boolean isAvailable = member.isBloodDonate();
        holder.donationStatus.setText(isAvailable ? "Available" : "Unavailable");
        holder.donationStatus.setTextColor(context.getResources().getColor(
            isAvailable ? R.color.success : R.color.error, null));
        holder.donationStatus.setBackgroundResource(
            isAvailable ? R.drawable.status_available : R.drawable.status_unavailable);
        
        // Set location info
        if (member.getLocation() != null) {
            holder.locationInfo.setText("Location shared • Online");
            holder.locationStatusIndicator.setBackgroundResource(R.drawable.status_indicator_active);
            holder.distanceInfo.setText("Nearby"); // You can calculate actual distance here
            holder.distanceInfo.setVisibility(View.VISIBLE);
        } else {
            holder.locationInfo.setText("Location not shared");
            holder.locationStatusIndicator.setBackgroundResource(R.drawable.status_indicator_inactive);
            holder.distanceInfo.setVisibility(View.GONE);
        }
        
        // Load profile image
        if (member.getProfileImageUrl() != null && !member.getProfileImageUrl().isEmpty()) {
            CloudinaryHelper.loadImage(context, member.getProfileImageUrl(), holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.profile_placeholder);
        }
        
        // Set click listeners
        holder.btnMap.setOnClickListener(v -> {
            if (member.getLocation() != null) {
                // Open map with member's location
                Intent intent = new Intent(context, MemberLocationActivity.class);
                intent.putExtra("memberId", member.getUid());
                intent.putExtra("memberName", member.getFirstName());
                context.startActivity(intent);
            }
        });

        holder.btnCall.setOnClickListener(v -> {
            if (callButtonClickListener != null && member.getPhone() != null) {
                callButtonClickListener.onCallButtonClick(member.getPhone());
            }
        });

        // Disable map button if location not shared
        holder.btnMap.setEnabled(member.getLocation() != null);
        holder.btnMap.setAlpha(member.getLocation() != null ? 1.0f : 0.5f);
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    public static class LocationViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImage, btnMap, btnCall;
        View locationStatusIndicator;
        TextView memberName, memberCollege, bloodGroup, donationStatus, locationInfo, distanceInfo;

        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            locationStatusIndicator = itemView.findViewById(R.id.location_status_indicator);
            memberName = itemView.findViewById(R.id.memberName);
            memberCollege = itemView.findViewById(R.id.memberCollege);
            bloodGroup = itemView.findViewById(R.id.bloodGroup);
            donationStatus = itemView.findViewById(R.id.donationStatus);
            locationInfo = itemView.findViewById(R.id.location_info);
            distanceInfo = itemView.findViewById(R.id.distance_info);
            btnMap = itemView.findViewById(R.id.btn_map);
            btnCall = itemView.findViewById(R.id.btnCall);
        }
    }
}
