package at.htl_villach.chatapplication.adapters;

import android.content.Context;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

import at.htl_villach.chatapplication.R;
import at.htl_villach.chatapplication.bll.User;
import de.hdodenhof.circleimageview.CircleImageView;

public class RequestListAdapter extends BaseAdapter {

    private ArrayList<String> requestUid;
    private LayoutInflater inflater;
    private DatabaseReference database;
    private DatabaseReference database2;
    private FirebaseAuth firebaseAuth;
    StorageReference storageReference;
    private final long MAX_DOWNLOAD_IMAGE = 1024 * 1024 * 5;

    public RequestListAdapter(Context applicationContext, ArrayList<String> requestUid) {
        this.requestUid = requestUid;
        this.inflater = (LayoutInflater.from(applicationContext));
    }

    @Override
    public int getCount() {
        return requestUid.size();
    }

    @Override
    public Object getItem(int position) {
        return requestUid.get(position);

    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        convertView = inflater.inflate(R.layout.activity_list_requests, null);

        database = FirebaseDatabase.getInstance().getReference("Users");
        database2 = FirebaseDatabase.getInstance().getReference("Requests");
        firebaseAuth = FirebaseAuth.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();

        final TextView item = convertView.findViewById(R.id.txtName);
        final TextView subitem = convertView.findViewById(R.id.txtUsername);
        final CircleImageView image = convertView.findViewById(R.id.list_picture);
        //item.setText(requestUid.get(position).getFullname());
        //subitem.setText(requestUid.get(position).getUsername());

        database.orderByChild("id")
                .equalTo(requestUid.get(position))
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        HashMap<String, HashMap<String, String>> user = (HashMap<String, HashMap<String, String>>) dataSnapshot.getValue();
                        if (user != null) {
                            final User userObject = new User();
                            for (String key : user.keySet()) {
                                userObject.setUsername(user.get(key).get("username"));
                                userObject.setFullname(user.get(key).get("fullname"));
                                userObject.setEmail(user.get(key).get("email"));
                                userObject.setId(user.get(key).get("id"));
                            }
                            item.setText(userObject.getFullname());
                            subitem.setText(userObject.getUsername());

                            image.post(new Runnable() {
                                @Override
                                public void run() {
                                    storageReference.child("users/" + userObject.getId() + "/profilePicture.jpg").getBytes(MAX_DOWNLOAD_IMAGE)
                                            .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                                @Override
                                                public void onSuccess(byte[] bytes) {
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

                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


        final ImageView imageDecline = convertView.findViewById(R.id.imageDecline);
        final ImageView imageAccept = convertView.findViewById(R.id.imageAccept);

        imageDecline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                database2.child(firebaseAuth.getCurrentUser().getUid())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                dataSnapshot.getRef().removeValue();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
            }
        });

        imageAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                database2.child(firebaseAuth.getCurrentUser().getUid())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                dataSnapshot.getRef().removeValue();

                                DatabaseReference ref = database.child(firebaseAuth.getCurrentUser().getUid()).child("friends");
                                HashMap<String, Object> newFriend = new HashMap<>();
                                newFriend.put(requestUid.get(position), true);
                                ref.updateChildren(newFriend)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {

                                            }
                                        });

                                DatabaseReference ref2 = database.child(requestUid.get(position)).child("friends");
                                HashMap<String, Object> newFriend2 = new HashMap<>();
                                newFriend2.put(firebaseAuth.getCurrentUser().getUid(), true);
                                ref2.updateChildren(newFriend2)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {

                                            }
                                        });
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
            }
        });

        return convertView;
    }
}
