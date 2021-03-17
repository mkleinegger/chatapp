package at.htl_villach.chatapplication;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import at.htl_villach.chatapplication.adapters.MessageSeenByListAdapter;
import at.htl_villach.chatapplication.bll.Chat;
import at.htl_villach.chatapplication.bll.Message;
import at.htl_villach.chatapplication.bll.User;

public class MessageInfoActivity extends AppCompatActivity {
    //toolbar
    private Toolbar toolbar;

    //controls
    private TextView tvMessage;
    private ListView lvUser;
    private TextView tvTime;
    private ImageView ivImage;
    private MessageSeenByListAdapter adapter;

    //database
    private DatabaseReference mRootRef;

    //data
    private Message mSelectedMessage;
    private Chat mSelectedChat;
    private ArrayList<User> mUsers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_info);

        mRootRef = FirebaseDatabase.getInstance().getReference();

        mSelectedMessage = getIntent().getParcelableExtra("selectedMessage");
        mSelectedChat = getIntent().getParcelableExtra("selectedChat");

        DatabaseReference messagesSeenByRef = mRootRef.child("MessagesSeenBy").child(mSelectedChat.getId()).child(mSelectedMessage.getId());
        messagesSeenByRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    DatabaseReference usersRef = mRootRef.child("Users").child(ds.getKey());
                    usersRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            mUsers.add(snapshot.getValue(User.class));
                            adapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            System.out.println("The read failed: " + databaseError.getCode());
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
            }
        });

        toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_acion_back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        ivImage = findViewById(R.id.message_image);
        tvMessage = findViewById(R.id.message_body);

        if (mSelectedMessage.getType().equals("text")) {
            ivImage.setVisibility(View.GONE);
            tvMessage.setText(mSelectedMessage.getMessage());
        } else {
            tvMessage.setVisibility(View.GONE);
            Picasso.get()
                    .load(mSelectedMessage.getMessage())
                    .resize(0, 800)
                    .centerCrop()
                    .into(ivImage);
        }

        tvTime = findViewById(R.id.message_time);
        tvTime.setText(mSelectedMessage.getTimeAsString());

        adapter = new MessageSeenByListAdapter(MessageInfoActivity.this, mUsers, mSelectedChat, mSelectedMessage);

        lvUser = findViewById(R.id.list);
        lvUser.setAdapter(adapter);
    }
}
