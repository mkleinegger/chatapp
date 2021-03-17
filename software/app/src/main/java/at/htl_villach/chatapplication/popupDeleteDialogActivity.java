package at.htl_villach.chatapplication;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import at.htl_villach.chatapplication.bll.User;

public class popupDeleteDialogActivity extends AppCompatActivity {
    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    String userId;
    String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popup_delete_dialog);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = dm.widthPixels;
        int heigth = dm.heightPixels;

        getWindow().setLayout((int) (width * 1), (int) (heigth * 0.32));
        firebaseAuth = FirebaseAuth.getInstance().getInstance();
    }

    public void deleteProfile(View view) {

        final TextInputLayout txtPassword = findViewById(R.id.txtPassword);
        String password = txtPassword.getEditText().getText().toString();
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(getApplicationContext(), R.string.emptyPasswordField, Toast.LENGTH_SHORT).show();
        } else {
            user = firebaseAuth.getCurrentUser();
            userId = user.getUid();
            email = user.getEmail();
            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            user.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (task.isSuccessful()) {
                                        DatabaseReference drfUser = FirebaseDatabase.getInstance().getReference("Users").child(userId);
                                        drfUser.removeValue();

                                        removeFromFriendsList();

                                        Toast.makeText(popupDeleteDialogActivity.this, "User account deleted!",
                                                Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(popupDeleteDialogActivity.this, LoginActivity.class);
                                        intent.putExtra("allowBack", false);
                                        startActivity(intent);
                                    } else {
                                        Toast.makeText(popupDeleteDialogActivity.this, "Something went wrong!",
                                                Toast.LENGTH_SHORT).show();
                                        onBackPressed();
                                    }
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getApplicationContext(), "Something went wrong " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });


        }
    }

    private void removeFromFriendsList() {
        FirebaseDatabase.getInstance().getReference().child("Users")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {

                            User test = snapshot.getValue(User.class);
                            snapshot.child("friends").hasChild(userId);
                            Log.e("TEST", test.getFullname() + " ist befreundet mit " + userId + ": " + snapshot.child("friends").hasChild(userId));
                            if (snapshot.child("friends").hasChild(userId)) {
                                DatabaseReference drfUser = FirebaseDatabase.getInstance().getReference("Users").child(test.getId()).child("friends").child(userId);
                                drfUser.removeValue();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }


    public void onCancel(View view) {
        this.onBackPressed();
    }
}
