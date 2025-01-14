package com.example.hello;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class FundraiserAdapter extends ArrayAdapter<Fundraiser> {
    private final String currentUserId;

    public FundraiserAdapter(@NonNull Context context, int resource, @NonNull List<Fundraiser> objects) {
        super(context, resource, objects);
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.fundraiser_item, parent, false);
        }

        Fundraiser fundraiser = getItem(position);
        if (fundraiser == null) return convertView;

        TextView title = convertView.findViewById(R.id.tvReason);
        TextView amount = convertView.findViewById(R.id.tvAmountNeeded);
        TextView method = convertView.findViewById(R.id.tvDonationMethod);
        Button deleteButton = convertView.findViewById(R.id.btnDeleteFundraiser);

        title.setText(fundraiser.getTitle() != null ? fundraiser.getTitle() : "N/A");
        amount.setText(fundraiser.getAmountNeeded() != null ? "Amount Needed: " + fundraiser.getAmountNeeded() : "Amount Needed: N/A");
        method.setText(fundraiser.getDonationMethod() != null ? "Donation Method: " + fundraiser.getDonationMethod() : "Donation Method: N/A");

        // Show delete button only for the creator of the fundraiser
        if (currentUserId.equals(fundraiser.getCreatorId())) {
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(v -> {
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Fundraisers")
                        .child(fundraiser.getCommunityId())
                        .child(fundraiser.getFundraiseId());

                ref.removeValue().addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Fundraiser deleted", Toast.LENGTH_SHORT).show();
                    remove(fundraiser);
                    notifyDataSetChanged();
                }).addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to delete fundraiser", Toast.LENGTH_SHORT).show());
            });
        } else {
            deleteButton.setVisibility(View.GONE);
        }

        return convertView;
    }
}
