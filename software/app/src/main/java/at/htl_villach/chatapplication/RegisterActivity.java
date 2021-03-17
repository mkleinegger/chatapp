package at.htl_villach.chatapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.HashMap;

public class RegisterActivity extends AppCompatActivity {
    FirebaseAuth firebaseAuth;
    DatabaseReference database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        final TextInputLayout txtUsername = findViewById(R.id.txtUsername);
        final TextInputLayout txtEmail = findViewById(R.id.txtEmail);
        final TextInputLayout txtPassword = findViewById(R.id.txtPassword);
        final TextInputLayout txtRepeatPassword = findViewById(R.id.txtRepeatPassword);
        final TextInputLayout txtFullname = findViewById(R.id.txtFullName);
        final Button btnRegister = findViewById(R.id.btnRegister);


        firebaseAuth = FirebaseAuth.getInstance();

        btnRegister.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                final String email = txtEmail.getEditText().getText().toString();
                final String username = txtUsername.getEditText().getText().toString();
                final String password = txtPassword.getEditText().getText().toString();
                String repeatPassword = txtRepeatPassword.getEditText().getText().toString();
                final String fullname = txtFullname.getEditText().getText().toString();
                boolean error = false;

                if (TextUtils.isEmpty(username)) {
                    txtUsername.setError("Please fill in your username.");
                    Toast.makeText(getApplicationContext(), "Please fill in your username.", Toast.LENGTH_SHORT).show();
                    error= true;
                }
                if (TextUtils.isEmpty(email)) {
                    txtEmail.setError("Please fill in your email address.");
                    Toast.makeText(getApplicationContext(), "Please fill in your email address.", Toast.LENGTH_SHORT).show();
                    error= true;
                }
                if (TextUtils.isEmpty(password)) {
                    txtPassword.setError("Please fill in your password.");
                    Toast.makeText(getApplicationContext(), "Please fill in your password.", Toast.LENGTH_SHORT).show();
                    error= true;
                }
                if (password.length() < 7) {
                    txtPassword.setError("Your password must be at least 7 characters.");
                    Toast.makeText(getApplicationContext(), "Your password must be at least 7 characters.", Toast.LENGTH_SHORT).show();
                    error= true;
                }
                if (!TextUtils.equals(password, repeatPassword)) {
                    txtRepeatPassword.setError("The passwords do not match.");
                    Toast.makeText(getApplicationContext(), "The passwords do not match.", Toast.LENGTH_SHORT).show();
                    error= true;
                }
                if(TextUtils.isEmpty(fullname)) {
                    txtFullname.setError("Please fill in your Full Name.");
                    Toast.makeText(getApplicationContext(), "Please fill in your Full Name.", Toast.LENGTH_SHORT).show();
                    error= true;
                }
                if(!error){
                    firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {

                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                                String userId = firebaseUser.getUid();
                                database = FirebaseDatabase.getInstance().getReference("Users").child(userId);
                                HashMap<String, String> newUser = new HashMap<>();
                                newUser.put("id",  userId);
                                newUser.put("email", email);
                                newUser.put("username", username);
                                newUser.put("fullname", fullname);
                                newUser.put("token", FirebaseInstanceId.getInstance().getToken());

                                database.setValue(newUser)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if(task.isSuccessful()) {
                                                    startNewIntent();
                                                }
                                            }
                                        });
                            } else {
                                Toast.makeText(getApplicationContext(), "Register not successful.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }


            }
        });

        if (firebaseAuth.getCurrentUser() != null) {
            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
            startActivity(intent);
        }


    }




    private void startNewIntent() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}