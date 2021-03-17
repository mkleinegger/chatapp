package at.htl_villach.chatapplication.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;

import at.htl_villach.chatapplication.SingleChatActivity;
import at.htl_villach.chatapplication.R;
import at.htl_villach.chatapplication.bll.Chat;
import at.htl_villach.chatapplication.bll.User;
import de.hdodenhof.circleimageview.CircleImageView;

public class ContactListAdapter extends BaseAdapter {
    ArrayList<User> contacts;
    LayoutInflater inflater;
    DatabaseReference database;
    StorageReference storageReference;
    FirebaseAuth firebaseAuth;
    private final long MAX_DOWNLOAD_IMAGE = 1024 * 1024 * 5;


    public ContactListAdapter(Context applicationContext, ArrayList<User> contacts) {
        this.contacts = contacts;
        this.inflater = (LayoutInflater.from(applicationContext));
    }


    @Override
    public int getCount() {
        return contacts.size();
    }

    @Override
    public Object getItem(int i) {
        return contacts.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {
        view = inflater.inflate(R.layout.activity_list_contacts, null);
        TextView item = view.findViewById(R.id.txtName);
        TextView subitem = view.findViewById(R.id.txtLastChat);
        final View innerView = view;
        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("Chats");
        storageReference = FirebaseStorage.getInstance().getReference();

        final User thisUser = contacts.get(i);


        final View test = view;
        final CircleImageView image = (CircleImageView) view.findViewById(R.id.list_picture);

        ImageView imageNewChat = view.findViewById(R.id.imageNewChat);

        item.setText(contacts.get(i).getFullname());

        subitem.setText(contacts.get(i).getUsername());

        image.post(new Runnable() {
            @Override
            public void run() {
                storageReference.child("users/" + thisUser.getId() + "/profilePicture.jpg").getBytes(MAX_DOWNLOAD_IMAGE)
                        .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                            @Override
                            public void onSuccess(byte[] bytes) {
                                contacts.get(i).setProfilePictureResource(bytes);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                image.setImageBitmap(Bitmap.createScaledBitmap(bitmap, image.getWidth(),
                                        image.getHeight(), false));
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                image.setImageResource(R.drawable.standard_picture);
                            }
                        });


            }

        });


        imageNewChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                database.orderByChild("id")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                HashMap<String, Object> chat = (HashMap<String, Object>) dataSnapshot.getValue();
                                HashMap<String, Object> usersInChat = new HashMap<>();
                                final FirebaseUser currUser = firebaseAuth.getCurrentUser();
                                HashMap<String, Object> foundChat = null;

                                if (chat != null) {
                                    for (String key : chat.keySet()) {
                                        usersInChat = (HashMap<String, Object>) chat.get(key);
                                        if (!(Boolean) usersInChat.get("isGroupChat")) {
                                            usersInChat = (HashMap<String, Object>) usersInChat.get("users");
                                            if (usersInChat.containsKey(currUser.getUid()) && usersInChat.containsKey(thisUser.getId())) {
                                                foundChat = (HashMap<String, Object>) chat.get(key);
                                            }
                                        }

                                    }

                                    if (foundChat != null) {
                                        Chat chatObject = new Chat((String) foundChat.get("id"), (HashMap<String, Boolean>) foundChat.get("users"), (Boolean) foundChat.get("isGroupChat"));
                                        Intent intent = new Intent(innerView.getContext(), SingleChatActivity.class);
                                        intent.putExtra("selectedChat", chatObject);
                                        innerView.getContext().startActivity(intent);
                                    } else {
                                        createChat(currUser, thisUser, innerView.getContext());
                                    }

                                } else {
                                    createChat(currUser, thisUser, innerView.getContext());
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
            }
        });

        return view;
    }

    private void createChat(final FirebaseUser currUser, final User thisUser, final Context context) {
        database.push().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String id = dataSnapshot.getKey();
                HashMap<String, Object> newChat = new HashMap<>();
                HashMap<String, Boolean> userPair = new HashMap<>();
                userPair.put(currUser.getUid(), true);
                userPair.put(thisUser.getId(), true);
                newChat.put("id", id);
                newChat.put("isGroupChat", false);
                newChat.put("users", userPair);

                final Intent intent = new Intent(context, SingleChatActivity.class);
                intent.putExtra("selectedChat", new Chat(id, userPair, false));

                database.child(id).setValue(newChat)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    context.startActivity(intent);
                                }
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


}
