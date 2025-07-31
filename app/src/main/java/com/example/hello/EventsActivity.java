package com.example.hello;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;
import com.example.hello.adapters.EventAdapter;
import com.example.hello.models.Event;
import java.util.ArrayList;
import java.util.List;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.ExistingWorkPolicy;
import java.util.concurrent.TimeUnit;
import com.example.hello.workers.EventReminderWorker;

public class EventsActivity extends AppCompatActivity implements EventAdapter.EventClickListener {
    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private List<Event> eventList;
    private String communityId;
    private DatabaseReference eventsRef;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        // Set up back button
        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());

        communityId = getIntent().getStringExtra("communityId");
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        eventsRef = FirebaseDatabase.getInstance().getReference("Events");

        initializeViews();
        loadEvents();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerViewEvents);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        eventList = new ArrayList<>();
        adapter = new EventAdapter(this, eventList, currentUserId, this);
        recyclerView.setAdapter(adapter);

        ExtendedFloatingActionButton fabCreateEvent = findViewById(R.id.fabCreateEvent);
        fabCreateEvent.setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateEventActivity.class);
            intent.putExtra("communityId", communityId);
            startActivity(intent);
        });
    }

    private void loadEvents() {
        eventsRef.orderByChild("communityId").equalTo(communityId)
            .addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    eventList.clear();
                    for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                        Event event = eventSnapshot.getValue(Event.class);
                        if (event != null) {
                            eventList.add(event);
                        }
                    }
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
    }

    @Override
    public void onJoinClick(Event event) {
        DatabaseReference eventRef = eventsRef.child(event.getId());
        DatabaseReference participantsRef = eventRef.child("participants").child(currentUserId);
        
        if (event.getParticipants() != null && event.getParticipants().containsKey(currentUserId)) {
            // Leave event
            participantsRef.removeValue();
        } else {
            // Join event
            participantsRef.setValue(true);
            scheduleEventReminder(event);
        }
    }

    @Override
    public void onLocationClick(Event event) {
        Intent intent = new Intent(this, EventLocationActivity.class);
        intent.putExtra("eventId", event.getId());
        intent.putExtra("latitude", event.getLatitude());
        intent.putExtra("longitude", event.getLongitude());
        intent.putExtra("title", event.getTitle());
        startActivity(intent);
    }

    private void scheduleEventReminder(Event event) {
        // Schedule reminder for 1 hour before event
        long eventTime = event.getDateTime();
        long currentTime = System.currentTimeMillis();
        long reminderDelay = eventTime - currentTime - (60 * 60 * 1000); // 1 hour before

        if (reminderDelay > 0) {
            Data inputData = new Data.Builder()
                .putString("eventTitle", event.getTitle())
                .putString("eventLocation", event.getLocation())
                .build();

            OneTimeWorkRequest reminderWork = new OneTimeWorkRequest.Builder(EventReminderWorker.class)
                .setInitialDelay(reminderDelay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .build();

            WorkManager.getInstance(this)
                .enqueueUniqueWork(
                    "event_reminder_" + event.getId(),
                    ExistingWorkPolicy.REPLACE,
                    reminderWork
                );
        }
    }
} 