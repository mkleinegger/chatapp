package at.htl_villach.chatapplication;

import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import at.htl_villach.chatapplication.adapters.ChatListAdapter;
import at.htl_villach.chatapplication.bll.Chat;
import at.htl_villach.chatapplication.bll.Message;

public class SendMessageToActivity extends AppCompatActivity {
    //toolbar
    private Toolbar toolbar;

    //controls
    private TextView tvMessage;
    private ListView lvUser;
    private TextView tvTime;
    private ImageView ivImage;
    private ChatListAdapter adapter;

    //database
    private DatabaseReference mRootRef;
    private FirebaseAuth mFirebaseUser;

    //data
    private Message mSelectedMessage;
    private ArrayList<Chat> mChats = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_to);

        setContentView(R.layout.activity_message_info);

        mRootRef = FirebaseDatabase.getInstance().getReference();
        mFirebaseUser = FirebaseAuth.getInstance();

        mSelectedMessage = getIntent().getParcelableExtra("selectedMessage");

        getChatsFromDatabase();

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

        adapter = new ChatListAdapter(SendMessageToActivity.this, mChats);

        lvUser = findViewById(R.id.list);
        lvUser.setAdapter(adapter);

        lvUser.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position,
                                    long arg3) {
                final Chat chat = (Chat) adapter.getItemAtPosition(position);

                AlertDialog.Builder builder = new AlertDialog.Builder(SendMessageToActivity.this);

                builder.setPositiveButton(R.string.popUpBtnYes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //Message
                        DatabaseReference sendMessagesRef = mRootRef.child("Messages").child(chat.getId());
                        String messageId = sendMessagesRef.push().getKey();

                        HashMap<String, Object> hashMapMessage = new HashMap<>();
                        hashMapMessage.put("id", messageId);
                        hashMapMessage.put("sender", mFirebaseUser.getCurrentUser().getUid());
                        hashMapMessage.put("message", mSelectedMessage.getMessage());
                        hashMapMessage.put("type", mSelectedMessage.getType());
                        hashMapMessage.put("timestamp", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
                        hashMapMessage.put("seen", false);

                        sendMessagesRef.child(messageId).updateChildren(hashMapMessage);

                        //MessageSeenBy
                        HashMap<String, Object> hashMapMessageSeenBy = new HashMap<>();
                        for (Map.Entry<String, Boolean> entry : chat.getUsers().entrySet()) {
                            if (!entry.getKey().equals(mFirebaseUser.getCurrentUser().getUid())) {
                                hashMapMessageSeenBy.put(entry.getKey(), false);
                            }
                        }

                        mRootRef.child("MessagesSeenBy").child(chat.getId()).child(messageId).setValue(hashMapMessageSeenBy);

                        //Notification
                        HashMap<String, Object> hashMapNotifications = new HashMap<>();
                        hashMapNotifications.put("sender", mFirebaseUser.getCurrentUser().getUid());
                        hashMapNotifications.put("type", "message");
                        hashMapNotifications.put("message", (mSelectedMessage.getType().equals("text")) ? mSelectedMessage.getMessage() : "Picture");

                        for (String receiver : chat.getReceivers(mFirebaseUser.getUid())) {
                            mRootRef.child("Notifications").child(receiver).push().setValue(hashMapNotifications);
                        }

                        finish();
                    }
                });

                builder.setNegativeButton(R.string.popUpBtnNo, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

                String content = (mSelectedMessage.getType().equals("text")) ? "message " + mSelectedMessage.getMessage() : "image";

                builder.setTitle(R.string.sendToPopUpTitle);
                builder.setMessage(SendMessageToActivity.this.getResources().getString(R.string.sendToPopUpMessage, content));

                AlertDialog dialog = builder.create();

                dialog.show();
            }
        });
    }

    private void getChatsFromDatabase() {
        mRootRef.child("Chats")
                .orderByChild("id")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        ArrayList<Chat> tempChat = new ArrayList<>();
                        HashMap<String, Object> chats = (HashMap<String, Object>) dataSnapshot.getValue();
                        FirebaseUser currentUser = mFirebaseUser.getCurrentUser();
                        if (chats != null) {
                            for (String key : chats.keySet()) {
                                HashMap<String, Object> curObj = (HashMap<String, Object>) chats.get(key);
                                HashMap<String, Boolean> userPair = (HashMap<String, Boolean>) curObj.get("users");
                                Boolean isGroupChat = (Boolean) curObj.get("isGroupChat");
                                if (userPair.containsKey(currentUser.getUid())) {
                                    tempChat.add(new Chat(key, userPair, isGroupChat));
                                }
                            }

                            if (!tempChat.isEmpty()) {
                                mChats.clear();
                                mChats.addAll(tempChat);
                                adapter.notifyDataSetChanged();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }
}
