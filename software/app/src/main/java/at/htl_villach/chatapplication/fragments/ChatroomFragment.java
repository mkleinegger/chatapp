package at.htl_villach.chatapplication.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import at.htl_villach.chatapplication.R;
import at.htl_villach.chatapplication.adapters.ChatroomAdapter;
import at.htl_villach.chatapplication.bll.Chat;
import at.htl_villach.chatapplication.bll.Message;

public class ChatroomFragment extends Fragment {
    private static int TOTAL_ITEMS_TO_LOAD = 10;

    public static final int PICK_IMAGE = 1;
    public static final int TAKE_PICTURE = 2;
    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int REQUEST_PICK_PHOTO = 2;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    //controlls
    private EditText etMessage;
    private ImageButton btnSend;
    private ImageButton btnSendPicture;
    private RecyclerView recyclerViewMessages;
    private ChatroomAdapter chatroomAdapter;
    private LinearLayoutManager linearLayoutManager;
    private SwipeRefreshLayout swipeRefreshLayout;

    //Database
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mRootRef;
    private StorageReference mStorageRef;
    private ValueEventListener mValueEventListener;
    private HashMap<DatabaseReference, ValueEventListener> mDatabaseListeners = new HashMap<>();

    //data
    private List<Message> mMessages = new ArrayList<>();
    private Chat mCurrentChat;
    private int mCurrentPage = 1;
    private int mItemPos = 0;
    private String mLastKey = "";
    private String mPrevKey = "";
    private Uri mImageUri;

    public ChatroomFragment() {
        // Required empty public constructor
    }

    public static ChatroomFragment newInstance(Chat selectedChat) {
        ChatroomFragment toDoListFragment = new ChatroomFragment();

        Bundle args = new Bundle();
        args.putParcelable("selectedChat", selectedChat);
        toDoListFragment.setArguments(args);

        return toDoListFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentChat = getArguments().getParcelable("selectedChat");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chatroom, container, false);

        mFirebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        mRootRef = FirebaseDatabase.getInstance().getReference();
        mStorageRef = FirebaseStorage.getInstance().getReference();

        etMessage = (EditText) view.findViewById(R.id.message_to_send);
        etMessage.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    btnSendPicture.setVisibility(View.VISIBLE);
                } else {
                    btnSendPicture.setVisibility(View.GONE);
                }
            }
        });

        btnSend = (ImageButton) view.findViewById(R.id.btn_send);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String msg = etMessage.getText().toString();

                if (!msg.trim().equals("")) {
                    sendMessage(msg.trim(), "text");
                } else {
                    Toast.makeText(getActivity(), R.string.emptyMessage, Toast.LENGTH_SHORT).show();
                }

                etMessage.setText("");
            }
        });

        btnSendPicture = (ImageButton) view.findViewById(R.id.btn_sendPicture);
        btnSendPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String[] items = {"Choose from Gallery", "Open Camera"};
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                checkPermissionAndPickPhotoIfGranted();
                                break;
                            case 1:
                                checkPermissionAndTakePhotoIfGranted();
                                break;
                            default:
                                Toast.makeText(getContext(), "Something unexpected happened", Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                });
                builder.show();
            }
        });

        linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setStackFromEnd(true);

        chatroomAdapter = new ChatroomAdapter(getContext(), mMessages, mCurrentChat);

        recyclerViewMessages = (RecyclerView) view.findViewById(R.id.recycler_view_messages);
        recyclerViewMessages.setNestedScrollingEnabled(false);

        recyclerViewMessages.setLayoutManager(linearLayoutManager);
        recyclerViewMessages.setAdapter(chatroomAdapter);

        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mCurrentPage++;
                mItemPos = 0;

                readMoreMessages();
            }
        });

        readMessages();
        updateSeenMessage();

        return view;
    }

    private void readMoreMessages() {
        DatabaseReference messagesRef = mRootRef.child("Messages").child(mCurrentChat.getId());
        Query messageQuery = messagesRef.orderByKey().endAt(mLastKey).limitToLast(10);

        messageQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Message message = dataSnapshot.getValue(Message.class);

                if (!mPrevKey.equals(message.getId())) {
                    mMessages.add(mItemPos++, message);
                } else {
                    mPrevKey = mLastKey;
                }

                if (mItemPos == 1) {
                    mLastKey = message.getId();
                }

                if (!message.getSender().equals(mFirebaseUser.getUid()) && !message.isSeen()) {
                    mRootRef.child("MessagesSeenBy").child(mCurrentChat.getId()).child(message.getId()).child(mFirebaseUser.getUid()).setValue(true);
                }

                chatroomAdapter.notifyDataSetChanged();

                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                Message m = dataSnapshot.getValue(Message.class);

                mMessages.remove(m);
                chatroomAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void readMessages() {
        DatabaseReference messagesRef = mRootRef.child("Messages").child(mCurrentChat.getId());
        Query messageQuery = messagesRef.limitToLast(mCurrentPage * TOTAL_ITEMS_TO_LOAD);

        messageQuery.addValueEventListener(mValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mMessages.clear();
                for (DataSnapshot ds : dataSnapshot.getChildren()) {
                    Message message = ds.getValue(Message.class);
                    mItemPos++;

                    if (mItemPos == 1) {
                        mLastKey = message.getId();
                        mPrevKey = mLastKey;
                    }

                    if (!message.getSender().equals(mFirebaseUser.getUid()) && !message.isSeen()) {
                        mRootRef.child("MessagesSeenBy").child(mCurrentChat.getId()).child(message.getId()).child(mFirebaseUser.getUid()).setValue(true);
                    }

                    mMessages.add(message);
                }

                chatroomAdapter.notifyDataSetChanged();

                recyclerViewMessages.scrollToPosition(mMessages.size() - 1);

                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                System.out.println("The read failed: " + databaseError.getCode());
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        mDatabaseListeners.put(messagesRef, mValueEventListener);
    }

    private void updateSeenMessage() {
        DatabaseReference seenMessagesRef = FirebaseDatabase.getInstance().getReference("MessagesSeenBy").child(mCurrentChat.getId());
        seenMessagesRef.addValueEventListener(mValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                HashMap<String, HashMap<String, Object>> messageSeenBy = (HashMap<String, HashMap<String, Object>>) dataSnapshot.getValue();

                for (Message m : mMessages) {
                    if (!m.isSeen()) {
                        if (!messageSeenBy.get(m.getId()).containsValue(false)) {
                            mRootRef.child("Messages").child(mCurrentChat.getId()).child(m.getId()).child("seen").setValue(true);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mDatabaseListeners.put(seenMessagesRef, mValueEventListener);
    }

    private void sendMessage(String message, String type) {
        //message
        DatabaseReference sendMessagesRef = mRootRef.child("Messages").child(mCurrentChat.getId());
        String messageId = sendMessagesRef.push().getKey();

        HashMap<String, Object> hashMapMessage = new HashMap<>();
        hashMapMessage.put("id", messageId);
        hashMapMessage.put("sender", mFirebaseUser.getUid());
        hashMapMessage.put("type", type);
        hashMapMessage.put("message", message);
        hashMapMessage.put("timestamp", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        hashMapMessage.put("seen", false);

        sendMessagesRef.child(messageId).updateChildren(hashMapMessage);

        //messageSeenBy
        HashMap<String, Object> hashMapMessageSeenBy = new HashMap<>();
        for (Map.Entry<String, Boolean> entry : mCurrentChat.getUsers().entrySet()) {
            if (!entry.getKey().equals(mFirebaseUser.getUid())) {
                hashMapMessageSeenBy.put(entry.getKey(), false);
            }
        }

        mRootRef.child("MessagesSeenBy").child(mCurrentChat.getId()).child(messageId).setValue(hashMapMessageSeenBy);

        //Notifications
        HashMap<String, Object> hashMapNotifications = new HashMap<>();
        hashMapNotifications.put("sender", mFirebaseUser.getUid());
        hashMapNotifications.put("type", "message");
        hashMapNotifications.put("message", message);

        for (String receiver : mCurrentChat.getReceivers(mFirebaseUser.getUid())) {
            mRootRef.child("Notifications").child(receiver).push().setValue(hashMapNotifications);
        }
    }

    private void checkPermissionAndTakePhotoIfGranted() {
        int permission = ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), PERMISSIONS_STORAGE, REQUEST_TAKE_PHOTO);
        } else {
            captureImage();
        }
    }

    private void checkPermissionAndPickPhotoIfGranted() {
        int permission = ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(), PERMISSIONS_STORAGE, REQUEST_PICK_PHOTO);
        } else {
            pickImage();
        }
    }

    private void captureImage() {
        Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, TAKE_PICTURE);
    }

    private void pickImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Choose a Picture"), PICK_IMAGE);
    }

    private void saveImageToDatabase() {
        final DatabaseReference sendMessagesRef = mRootRef.child("Messages").child(mCurrentChat.getId());
        final String messageId = sendMessagesRef.push().getKey();

        mStorageRef.child("message_images/Chat_" + mCurrentChat.getId() + "/Message_" + messageId + ".jpg")
                .putFile(mImageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Toast.makeText(getContext(), "Image send", Toast.LENGTH_SHORT).show();

                        Task<Uri> urlTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!urlTask.isSuccessful()) ;

                        //Message
                        HashMap<String, Object> hashMapMessage = new HashMap<>();
                        hashMapMessage.put("id", messageId);
                        hashMapMessage.put("sender", mFirebaseUser.getUid());
                        hashMapMessage.put("type", "image");
                        hashMapMessage.put("message", ((Uri) urlTask.getResult()).toString());
                        hashMapMessage.put("timestamp", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
                        hashMapMessage.put("seen", false);

                        sendMessagesRef.child(messageId).updateChildren(hashMapMessage);

                        //MessageSeenBy
                        HashMap<String, Object> hashMapMessageSeenBy = new HashMap<>();
                        for (Map.Entry<String, Boolean> entry : mCurrentChat.getUsers().entrySet()) {
                            if (!entry.getKey().equals(mFirebaseUser.getUid())) {
                                hashMapMessageSeenBy.put(entry.getKey(), false);
                            }
                        }
                        mRootRef.child("MessagesSeenBy").child(mCurrentChat.getId()).child(messageId).setValue(hashMapMessageSeenBy);

                        //Notifications
                        HashMap<String, Object> hashMapNotifications = new HashMap<>();
                        hashMapNotifications.put("sender", mFirebaseUser.getUid());
                        hashMapNotifications.put("type", "message");
                        hashMapNotifications.put("message", "Picture");

                        for (String receiver : mCurrentChat.getReceivers(mFirebaseUser.getUid())) {
                            mRootRef.child("Notifications").child(receiver).push().setValue(hashMapNotifications);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), "failed to send image", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveBitmapToDatabase(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        bitmap.recycle();

        final DatabaseReference sendMessagesRef = mRootRef.child("Messages").child(mCurrentChat.getId());
        final String messageId = sendMessagesRef.push().getKey();

        mStorageRef.child("message_images/Chat_" + mCurrentChat.getId() + "/Message_" + messageId + ".jpg")
                .putBytes(byteArray)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Toast.makeText(getContext(), "Image send", Toast.LENGTH_SHORT).show();

                        Task<Uri> urlTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!urlTask.isSuccessful()) ;

                        //Message
                        HashMap<String, Object> hashMapMessage = new HashMap<>();
                        hashMapMessage.put("id", messageId);
                        hashMapMessage.put("sender", mFirebaseUser.getUid());
                        hashMapMessage.put("type", "image");
                        hashMapMessage.put("message", ((Uri) urlTask.getResult()).toString());
                        hashMapMessage.put("timestamp", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
                        hashMapMessage.put("seen", false);

                        sendMessagesRef.child(messageId).updateChildren(hashMapMessage);

                        //MessageSeenBy
                        HashMap<String, Object> hashMapMessageSeenBy = new HashMap<>();
                        for (Map.Entry<String, Boolean> entry : mCurrentChat.getUsers().entrySet()) {
                            if (!entry.getKey().equals(mFirebaseUser.getUid())) {
                                hashMapMessageSeenBy.put(entry.getKey(), false);
                            }
                        }
                        mRootRef.child("MessagesSeenBy").child(mCurrentChat.getId()).child(messageId).setValue(hashMapMessageSeenBy);

                        //Notifications
                        HashMap<String, Object> hashMapNotifications = new HashMap<>();
                        hashMapNotifications.put("sender", mFirebaseUser.getUid());
                        hashMapNotifications.put("type", "message");
                        hashMapNotifications.put("message", "Picture");

                        for (String receiver : mCurrentChat.getReceivers(mFirebaseUser.getUid())) {
                            mRootRef.child("Notifications").child(receiver).push().setValue(hashMapNotifications);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), "failed to send image", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        try {
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == PICK_IMAGE) {
                    mImageUri = data.getData();
                    saveImageToDatabase();
                } else if (requestCode == TAKE_PICTURE) {
                    Bundle extras = data.getExtras();
                    Bitmap bitmap = (Bitmap) extras.get("data");
                    saveBitmapToDatabase(bitmap);
                }
            }
        } catch (Exception ex) {
            Toast.makeText(getContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        for (Map.Entry<DatabaseReference, ValueEventListener> entry : mDatabaseListeners.entrySet()) {
            DatabaseReference databaseReference = entry.getKey();
            ValueEventListener value = entry.getValue();
            databaseReference.removeEventListener(value);
        }
    }
}
