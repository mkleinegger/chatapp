package at.htl_villach.chatapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import at.htl_villach.chatapplication.bll.Chat;
import at.htl_villach.chatapplication.bll.User;
import at.htl_villach.chatapplication.fragments.ChatroomFragment;
import de.hdodenhof.circleimageview.CircleImageView;

public class SingleChatActivity extends AppCompatActivity {
    private final long MAX_DOWNLOAD_IMAGE = 1024 * 1024 * 5;

    //data
    private Chat mCurrentChat;
    private User mSelectedUser;

    //database
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mUserRef;
    private StorageReference mStorageRef;

    //toolbar
    private Toolbar toolbar;
    private TextView toolbarTitle;
    private CircleImageView toolbarPicture;

    //Fragment
    private ChatroomFragment chatroom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_chat);

        Intent intent = getIntent();
        mCurrentChat = (Chat) intent.getParcelableExtra("selectedChat");

        mStorageRef = FirebaseStorage.getInstance().getReference();

        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mUserRef = FirebaseDatabase.getInstance().getReference("Users").child(mCurrentChat.getReceiver(mFirebaseUser.getUid()));
        mUserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                mSelectedUser = snapshot.getValue(User.class);

                toolbarTitle.setText(mSelectedUser.getFullname());
                toolbarPicture.post(new Runnable() {
                    @Override
                    public void run() {
                        mStorageRef.child("users/" + mSelectedUser.getId() + "/profilePicture.jpg")
                                .getBytes(MAX_DOWNLOAD_IMAGE)
                                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                    @Override
                                    public void onSuccess(byte[] bytes) {
                                        mSelectedUser.setProfilePictureResource(bytes);
                                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                        toolbarPicture.setImageBitmap(Bitmap.createScaledBitmap(bitmap, toolbarPicture.getWidth(),
                                                toolbarPicture.getHeight(), false));
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        toolbarPicture.setImageResource(R.drawable.standard_picture);
                                    }
                                });
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        toolbar = (Toolbar) findViewById(R.id.toolbar_chat);
        toolbarPicture = (CircleImageView) findViewById(R.id.toolbar_profilpicture);
        toolbarTitle = (TextView) findViewById(R.id.toolbar_title);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        toolbar.setNavigationIcon(R.drawable.ic_acion_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SingleChatActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });

        toolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SingleChatActivity.this, ProfileActivity.class);
                intent.putExtra("selectedContact", mSelectedUser);
                startActivity(intent);
            }
        });

        chatroom = (ChatroomFragment) getSupportFragmentManager().findFragmentById(R.id.chatroom);
        chatroom = ChatroomFragment.newInstance(mCurrentChat);
        getSupportFragmentManager().beginTransaction().add(R.id.chatroom, chatroom).commit();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chatroom, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_MoreInformation:
                Intent intent = new Intent(SingleChatActivity.this, ProfileActivity.class);
                intent.putExtra("selectedContact", mSelectedUser);
                startActivity(intent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(SingleChatActivity.this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }
}
