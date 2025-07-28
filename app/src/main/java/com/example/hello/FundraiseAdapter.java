package com.example.hello;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class FundraiseAdapter extends RecyclerView.Adapter<FundraiseAdapter.FundraiseViewHolder> {
    private Context context;
    private List<Fundraise> fundraises;
    private String currentUserId;
    private OnFundraiseActionListener actionListener;

    public interface OnFundraiseActionListener {
        void onEdit(Fundraise fundraise);
        void onDelete(Fundraise fundraise);
        void onDonate(Fundraise fundraise);
    }

    public FundraiseAdapter(Context context, List<Fundraise> fundraises, 
                          String currentUserId, OnFundraiseActionListener listener) {
        this.context = context;
        this.fundraises = fundraises;
        this.currentUserId = currentUserId;
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public FundraiseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_fundraise_item, parent, false);
        return new FundraiseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FundraiseViewHolder holder, int position) {
        Fundraise fundraise = fundraises.get(position);
        
        holder.campaignTitle.setText(fundraise.getTitle());
        holder.campaignDescription.setText(fundraise.getDescription());
        holder.donationMethod.setText("Donation Method: " + 
            (fundraise.getDonationMethod() != null ? fundraise.getDonationMethod() : "Not specified"));
        
        // Show edit/delete options only for creator with null checks
        if (currentUserId != null && fundraise.getCreatorId() != null && 
            currentUserId.equals(fundraise.getCreatorId())) {
            holder.optionsLayout.setVisibility(View.VISIBLE);
            holder.btnEdit.setOnClickListener(v -> actionListener.onEdit(fundraise));
            holder.btnDelete.setOnClickListener(v -> {
                // Show confirmation dialog
                new MaterialAlertDialogBuilder(context)
                    .setTitle("Delete Fundraiser")
                    .setMessage("Are you sure you want to delete this fundraiser?")
                    .setPositiveButton("Delete", (dialog, which) -> 
                        actionListener.onDelete(fundraise))
                    .setNegativeButton("Cancel", null)
                    .show();
            });
        } else {
            holder.optionsLayout.setVisibility(View.GONE);
        }
        
        // Format currency amounts with null/zero checks
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        double raisedAmount = fundraise.getRaisedAmount();
        double targetAmount = 0;
        try {
            targetAmount = Double.parseDouble(fundraise.getAmountNeeded());
        } catch (NumberFormatException e) {
            // Handle invalid amount string
            targetAmount = 0;
        }
        
        holder.raisedAmount.setText(format.format(raisedAmount));
        holder.targetAmount.setText("Target: " + format.format(targetAmount));
        
        // Calculate and set progress with zero check
        int progress = targetAmount > 0 ? 
            (int)((raisedAmount / targetAmount) * 100) : 0;
        holder.progressIndicator.setProgress(progress);

        holder.btnDonate.setOnClickListener(v -> actionListener.onDonate(fundraise));
    }

    @Override
    public int getItemCount() {
        return fundraises.size();
    }

    static class FundraiseViewHolder extends RecyclerView.ViewHolder {
        ImageView campaignImage;
        TextView campaignTitle, campaignDescription, donationMethod, 
                 raisedAmount, targetAmount;
        LinearProgressIndicator progressIndicator;
        MaterialButton btnDonate, btnEdit, btnDelete;
        LinearLayout optionsLayout;

        FundraiseViewHolder(View itemView) {
            super(itemView);
            campaignImage = itemView.findViewById(R.id.campaignImage);
            campaignTitle = itemView.findViewById(R.id.campaignTitle);
            campaignDescription = itemView.findViewById(R.id.campaignDescription);
            donationMethod = itemView.findViewById(R.id.donationMethod);
            raisedAmount = itemView.findViewById(R.id.raisedAmount);
            targetAmount = itemView.findViewById(R.id.targetAmount);
            progressIndicator = itemView.findViewById(R.id.progressIndicator);
            btnDonate = itemView.findViewById(R.id.btnDonate);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            optionsLayout = itemView.findViewById(R.id.optionsLayout);
        }
    }
} 