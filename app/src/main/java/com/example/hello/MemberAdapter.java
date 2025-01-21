package com.example.hello;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private List<Member> memberList;
    private Context context;

    public MemberAdapter(Context context, List<Member> memberList) {
        this.context = context;
        this.memberList = memberList;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.member_item, parent, false);
        return new MemberViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        Member member = memberList.get(position);
        holder.tvMemberName.setText(member.getName());
        holder.tvMemberDetails.setText("Home: " + member.getUid() + "\nCollege: " + member.getCollege());

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
        return memberList.size();
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView tvMemberName, tvMemberDetails;

        public MemberViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvMemberDetails = itemView.findViewById(R.id.tvMemberDetails);
        }
    }
}
