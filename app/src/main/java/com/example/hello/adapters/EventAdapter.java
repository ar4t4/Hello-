package com.example.hello.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.example.hello.R;
import com.example.hello.models.Event;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private Context context;
    private List<Event> events;
    private String currentUserId;
    private EventClickListener listener;

    public interface EventClickListener {
        void onJoinClick(Event event);
        void onLocationClick(Event event);
    }

    public EventAdapter(Context context, List<Event> events, String currentUserId, EventClickListener listener) {
        this.context = context;
        this.events = events;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        
        holder.titleText.setText(event.getTitle());
        holder.descriptionText.setText(event.getDescription());
        holder.locationText.setText(event.getLocation());
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
        holder.dateTimeText.setText(sdf.format(new Date(event.getDateTime())));
        
        int participantCount = event.getParticipants() != null ? event.getParticipants().size() : 0;
        holder.participantsText.setText(participantCount + " Participants");
        
        boolean isJoined = event.getParticipants() != null && 
                          event.getParticipants().containsKey(currentUserId);
        
        holder.btnJoin.setText(isJoined ? "Leave" : "Join");
        holder.btnJoin.setOnClickListener(v -> listener.onJoinClick(event));
        holder.btnLocation.setOnClickListener(v -> listener.onLocationClick(event));
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView titleText, descriptionText, dateTimeText, locationText, participantsText;
        MaterialButton btnJoin, btnLocation;

        EventViewHolder(View itemView) {
            super(itemView);
            titleText = itemView.findViewById(R.id.titleText);
            descriptionText = itemView.findViewById(R.id.descriptionText);
            dateTimeText = itemView.findViewById(R.id.dateTimeText);
            locationText = itemView.findViewById(R.id.locationText);
            participantsText = itemView.findViewById(R.id.participantsText);
            btnJoin = itemView.findViewById(R.id.btnJoin);
            btnLocation = itemView.findViewById(R.id.btnLocation);
        }
    }
} 