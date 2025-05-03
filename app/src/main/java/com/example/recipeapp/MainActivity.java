package com.example.recipeapp;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements BotChat.ApiResponseListener {
    private ChatAdapter chatAdapter;
    private ArrayList<ChatMessage> chatMessages;
    private EditText editTextMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(chatMessages);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(chatAdapter);

        editTextMessage = findViewById(R.id.editTextMessage);
        findViewById(R.id.btnSend).setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String message = editTextMessage.getText().toString().trim();
        if (message.isEmpty()) return;

        chatMessages.add(new ChatMessage(message, true));
        editTextMessage.setText("");
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
        new BotChat(this).execute(message);
    }

    @Override
    public void onApiResponse(String response) {
        chatMessages.add(new ChatMessage(response, false));
        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
    }
}