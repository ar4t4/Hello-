package com.example.hello;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hello.helpers.CloudinaryHelper;

import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private List<Member> members;
    private Context context;
    private OnCallButtonClickListener callButtonClickListener;

    public interface OnCallButtonClickListener {
        void onCallButtonClick(String phoneNumber);
    }

    public MemberAdapter(Context context, List<Member> members, OnCallButtonClickListener listener) {
        this.context = context;
        this.members = members;
        this.callButtonClickListener = listener;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_member_item, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        Member member = members.get(position);
        
        // Show first name only
        holder.memberName.setText(member.getFirstName());
        holder.memberCollege.setText(member.getCollege());
        holder.bloodGroup.setText(member.getBloodGroup());
        holder.donationStatus.setText(member.isBloodDonate() ? "Available" : "Unavailable");
        
        // Load profile image if available
        if (member.getProfileImageUrl() != null && !member.getProfileImageUrl().isEmpty()) {
            CloudinaryHelper.loadImage(context, member.getProfileImageUrl(), holder.profileImage);
        } else {
            holder.profileImage.setImageResource(R.drawable.profile_placeholder);
        }
        
        holder.btnCall.setOnClickListener(v -> {
            if (callButtonClickListener != null && member.getPhone() != null) {
                callButtonClickListener.onCallButtonClick(member.getPhone());
            }
        });

        // Handle item click - Show a dialog with user details
        holder.itemView.setOnClickListener(v -> {
            showMemberDetailsDialog(member);
        });
    }

    private void showMemberDetailsDialog(Member member) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_member_details);
        dialog.setCancelable(true);
        
        // Initialize dialog views
        ImageView ivProfileImage = dialog.findViewById(R.id.iv_profile_image);
        TextView tvName = dialog.findViewById(R.id.tv_name);
        TextView tvEmail = dialog.findViewById(R.id.tv_email);
        TextView tvPhone = dialog.findViewById(R.id.tv_phone);
        TextView tvCollege = dialog.findViewById(R.id.tv_college);
        TextView tvUniversity = dialog.findViewById(R.id.tv_university);
        TextView tvSchool = dialog.findViewById(R.id.tv_school);
        TextView tvHome = dialog.findViewById(R.id.tv_home);
        TextView tvBloodGroup = dialog.findViewById(R.id.tv_blood_group);
        Button btnShowLocation = dialog.findViewById(R.id.btn_show_location);
        Button btnClose = dialog.findViewById(R.id.btn_close);
        
        // Set data to views, using empty string instead of N/A
        String fullName = member.getFirstName() + " " + (member.getLastName() != null ? member.getLastName() : "");
        tvName.setText(fullName.trim());
        
        // Show email if available
        tvEmail.setText(member.getEmail() != null && !member.getEmail().isEmpty() ? member.getEmail() : "");
        
        tvPhone.setText(member.getPhone() != null && !member.getPhone().isEmpty() ? member.getPhone() : "");
        tvCollege.setText(member.getCollege() != null && !member.getCollege().isEmpty() ? member.getCollege() : "");
        tvSchool.setText(member.getSchool() != null && !member.getSchool().isEmpty() ? member.getSchool() : "");
        tvHome.setText(member.getHome() != null && !member.getHome().isEmpty() ? member.getHome() : "");
        tvBloodGroup.setText(member.getBloodGroup() != null && !member.getBloodGroup().isEmpty() ? member.getBloodGroup() : "");
        
        // Set university field - use the university field if available, otherwise use college
        String universityText = "";
        if (member.getUniversity() != null && !member.getUniversity().isEmpty()) {
            universityText = member.getUniversity();
        } else if (member.getCollege() != null && !member.getCollege().isEmpty()) {
            universityText = member.getCollege();
        }
        tvUniversity.setText(universityText);
        
        // Load profile image
        if (member.getProfileImageUrl() != null && !member.getProfileImageUrl().isEmpty()) {
            CloudinaryHelper.loadImage(context, member.getProfileImageUrl(), ivProfileImage);
        } else {
            ivProfileImage.setImageResource(R.drawable.profile_placeholder);
        }
        
        // Show location button click
        btnShowLocation.setOnClickListener(v -> {
            Intent intent = new Intent(context, MemberLocationActivity.class);
            intent.putExtra("memberId", member.getUid());
            context.startActivity(intent);
            dialog.dismiss();
        });
        
        // Close button click
        btnClose.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView memberName, memberCollege, bloodGroup, donationStatus;
        ImageButton btnCall;
        ImageView profileImage;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            memberName = itemView.findViewById(R.id.memberName);
            memberCollege = itemView.findViewById(R.id.memberCollege);
            bloodGroup = itemView.findViewById(R.id.bloodGroup);
            donationStatus = itemView.findViewById(R.id.donationStatus);
            btnCall = itemView.findViewById(R.id.btnCall);
            profileImage = itemView.findViewById(R.id.profileImage);
        }
    }
}
