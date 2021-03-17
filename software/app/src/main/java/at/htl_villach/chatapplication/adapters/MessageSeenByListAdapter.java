package at.htl_villach.chatapplication.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

import at.htl_villach.chatapplication.R;
import at.htl_villach.chatapplication.bll.Chat;
import at.htl_villach.chatapplication.bll.Message;
import at.htl_villach.chatapplication.bll.User;
import de.hdodenhof.circleimageview.CircleImageView;

public class MessageSeenByListAdapter extends BaseAdapter {
    public long MAX_DOWNLOAD_IMAGE = 1024 * 1024 * 5;

    private ArrayList<User> mUsers;
    private Chat mCurrentChat;
    private Message mCurrentMessage;

    private LayoutInflater mInflater;

    private StorageReference mStorageRef;
    private DatabaseReference mSeenByRef;

    public MessageSeenByListAdapter(Context applicationContext, ArrayList<User> users, Chat chat, Message message) {
        this.mUsers = users;
        this.mCurrentChat = chat;
        this.mCurrentMessage = message;
        this.mInflater = (LayoutInflater.from(applicationContext));
    }

    @Override
    public int getCount() {
        return mUsers.size();
    }

    @Override
    public Object getItem(int i) {
        return mUsers.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {
        view = mInflater.inflate(R.layout.activity_list_messageseenby, null);

        mStorageRef = FirebaseStorage.getInstance().getReference();

        final User user = mUsers.get(i);

        TextView tvName = view.findViewById(R.id.tvName);
        tvName.setText(user.getFullname());

        final TextView tvIsseen = view.findViewById(R.id.tvIsseen);
        mSeenByRef = FirebaseDatabase.getInstance().getReference("MessagesSeenBy").child(mCurrentChat.getId()).child(mCurrentMessage.getId()).child(user.getId());
        mSeenByRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue().equals(true)) {
                    tvIsseen.setText(R.string.seenBy);
                } else {
                    tvIsseen.setText(R.string.notSeenBy);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        final CircleImageView image = (CircleImageView) view.findViewById(R.id.list_picture);
        image.post(new Runnable() {
            @Override
            public void run() {
                mStorageRef.child("users/" + user.getId() + "/profilePicture.jpg").getBytes(MAX_DOWNLOAD_IMAGE)
                        .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                            @Override
                            public void onSuccess(byte[] bytes) {
                                mUsers.get(i).setProfilePictureResource(bytes);
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

        return view;
    }
}
