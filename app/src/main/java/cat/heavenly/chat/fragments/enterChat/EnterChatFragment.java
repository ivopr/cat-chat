package cat.heavenly.chat.fragments.enterChat;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

import cat.heavenly.chat.R;
import cat.heavenly.chat.activities.ChatActivity;
import cat.heavenly.chat.databinding.FragmentEnterChatBinding;

public class EnterChatFragment extends Fragment {
	private FragmentEnterChatBinding binding;

	private static final int RC_SIGN_IN = 100;
	private static final String TAG = "GOOGLE_SIGN_IN_TAG";

	private GoogleSignInClient googleSignInClient;
	private FirebaseAuth firebaseAuth;

	public View onCreateView(
		@NonNull LayoutInflater inflater,
		ViewGroup container,
		Bundle savedInstanceState
	) {
		binding = FragmentEnterChatBinding
			.inflate(inflater, container, false);

		View root = binding.getRoot();

		// Objeto de configurações de login social google
		GoogleSignInOptions gso =
			new GoogleSignInOptions
				.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
			.requestIdToken(getString(R.string.auth_client_id))
			.requestEmail()
			.build();

		// Inicia o cliente de login com as configurações definidas anteriormente
		googleSignInClient = GoogleSignIn
			.getClient(root.getContext(), gso);

		final TextView loginHelper = binding.loginHelper;

		// Inicia o módulo de autenticação do firebase e registra um
		// listener para realizar atualizações de tela toda vez que
		// o estado da autenticação do usuário for alterado
		firebaseAuth = FirebaseAuth.getInstance();
		firebaseAuth.addAuthStateListener(auth -> {
			if (auth.getCurrentUser() != null) {
				loginHelper.setClickable(false);
				loginHelper.setText("Conectado como " + auth.getCurrentUser().getDisplayName());
			} else {
				loginHelper.setClickable(true);
				loginHelper.setText("Clique aqui e faça seu login");
			}
		});

		// Quando clicar no botão, busca uma sala com o ID fornecido e
		// entra, ou cria uma nova sala, se o ID fornecido ainda não existir
		Button enterChat = binding.button;
		enterChat.setOnClickListener(v -> {
			TextInputEditText inputChatId = binding.chatId; // findViewById(R.id.chat_id)
			FirebaseUser user = firebaseAuth.getCurrentUser();

			if (user != null) {
				if (!Objects.requireNonNull(inputChatId.getText()).toString().isEmpty()) {
					String chatId = inputChatId.getText().toString().trim();
					FirebaseDatabase database = FirebaseDatabase.getInstance();
					DatabaseReference roomReference = database.getReference("/rooms/" + chatId);
					DatabaseReference roomsReference = database.getReference("/rooms");

					roomReference.addListenerForSingleValueEvent(new ValueEventListener() {
						@Override
						public void onDataChange(@NonNull DataSnapshot snapshot) {
							if (!snapshot.exists()) {
								roomsReference.child(chatId).child("name").setValue("Olar");
							}

							// Intent da página do chat
							Intent chatIntent =
								new Intent(
									root.getContext(),
									ChatActivity.class
								);

							chatIntent.putExtra("chatId", chatId);

							startActivity(chatIntent);
						}

						@Override
						public void onCancelled(@NonNull DatabaseError error) {
							Toast.makeText(
								root.getContext(),
								"Não foi possível criar/entrar na sala",
								Toast.LENGTH_SHORT
							).show();
						}
					});
				} else {
					Toast.makeText(
						root.getContext(),
						"Não é possível criar/entrar numa sala sem nome",
						Toast.LENGTH_SHORT
					).show();
				}
			} else {
				Toast.makeText(
					root.getContext(),
					"Faça seu login primeiro",
					Toast.LENGTH_SHORT
				).show();
			}
		});

		loginHelper.setOnClickListener(v -> {
			Log.d(TAG, "BEGIN");
			Intent signInIntent = googleSignInClient.getSignInIntent();

			startActivityForResult(signInIntent, RC_SIGN_IN);
		});

		return root;
	}

	@Override
	public void onActivityResult(
		int requestCode,
		int resultCode,
		@Nullable Intent data
	) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == RC_SIGN_IN) {
			Log.d(TAG, "onActivityResult");
			Task<GoogleSignInAccount> accountTask = GoogleSignIn.getSignedInAccountFromIntent(data);
			try {
				GoogleSignInAccount account = accountTask.getResult(ApiException.class);
				firebaseAuthWithGoogleAccount(account);
			} catch (Exception e) {
				Log.d(TAG, "" + e.getMessage());
			}
		}
	}

	private void firebaseAuthWithGoogleAccount(GoogleSignInAccount account) {
		Log.d(TAG, "firebaseAuthWithGoogleAccount: begin firebase auth with google account");
		AuthCredential auth = GoogleAuthProvider.getCredential(account.getIdToken(), null);
		firebaseAuth.signInWithCredential(auth)
			.addOnSuccessListener(authResult -> {
				Log.d(TAG, "firebaseAuthWithGoogleAccount: logged in");
				FirebaseUser user = firebaseAuth.getCurrentUser();
				if (user != null) {
					String uid = user.getUid();
					String email = user.getEmail();

					Log.d(TAG, "firebaseAuthWithGoogleAccount: email " + email);
					Log.d(TAG, "firebaseAuthWithGoogleAccount: UID " + uid);
				}

				if (authResult.getAdditionalUserInfo().isNewUser()) {
					Log.d(TAG, "firebaseAuthWithGoogleAccount: new user");
				} else {
					Log.d(TAG, "firebaseAuthWithGoogleAccount: old user");
				}
			})
			.addOnFailureListener(e -> {
				Log.d(TAG, "firebaseAuthWithGoogleAccount: login failed" + e.getMessage());
			});
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		binding = null;
	}
}