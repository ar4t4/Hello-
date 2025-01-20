package com.example.hello;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.MemberViewHolder> {

    private Context context;
    private List<Member> memberList;

    public MemberAdapter(Context context, List<Member> memberList) {
        this.context = context;
        this.memberList = memberList;
    }

    @NonNull
    @Override
    public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context).inflate(R.layout.member_item, parent, false);
        return new MemberViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
        Member member = memberList.get(position);
        holder.tvMemberName.setText(member.getName());
        holder.tvMemberDetails.setText("Home: " + member.getHome() + "\nCollege: " + member.getCollege());
    }

    @Override
    public int getItemCount() {
        return memberList.size();
    }

    public static class MemberViewHolder extends RecyclerView.ViewHolder {
        TextView tvMemberName, tvMemberDetails;

        public MemberViewHolder(View itemView) {
            super(itemView);
            tvMemberName = itemView.findViewById(R.id.tvMemberName);
            tvMemberDetails = itemView.findViewById(R.id.tvMemberDetails);
        }
    }
}
