package com.example.hello;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        holder.memberName.setText(member.getName());
        holder.memberCollege.setText(member.getCollege());
        holder.bloodGroup.setText(member.getBloodGroup());
        holder.donationStatus.setText(member.isBloodDonate() ? "Available" : "Unavailable");
        
        holder.btnCall.setOnClickListener(v -> {
            if (callButtonClickListener != null && member.getPhone() != null) {
                callButtonClickListener.onCallButtonClick(member.getPhone());
            }
        });

        // Handle item click
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MemberLocationActivity.class);
            //show uid in log
            Log.d("UID", member.getUid());
            //show toast for uid
            Toast.makeText(context, member.getUid(), Toast.LENGTH_SHORT).show();
            intent.putExtra("memberId", member.getUid()); // Pass the member's UID
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return members.size();
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView memberName, memberCollege, bloodGroup, donationStatus;
        ImageButton btnCall;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            memberName = itemView.findViewById(R.id.memberName);
            memberCollege = itemView.findViewById(R.id.memberCollege);
            bloodGroup = itemView.findViewById(R.id.bloodGroup);
            donationStatus = itemView.findViewById(R.id.donationStatus);
            btnCall = itemView.findViewById(R.id.btnCall);
        }
    }
}
