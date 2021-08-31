package cat.heavenly.chat.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import cat.heavenly.chat.R;
import cat.heavenly.chat.databinding.ActivityChatBinding;

public class ChatActivity extends AppCompatActivity {
	private ActivityChatBinding binding;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityChatBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		Bundle data = getIntent().getExtras();

		String chatId = data.getString("chatId");

		TextView chatName = binding.chatName;

		DatabaseReference chatRef = FirebaseDatabase
			.getInstance()
			.getReference("/rooms/" + chatId);

		chatRef.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				if (snapshot.exists()) {
					chatName.setText(snapshot.child("name").getValue().toString());
				}
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {}
		});

		ImageButton goBack = binding.goBack;
		goBack.setOnClickListener(v -> {
			finish();
		});
	}
}