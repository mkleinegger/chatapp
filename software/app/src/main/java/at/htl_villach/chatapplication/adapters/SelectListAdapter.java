package at.htl_villach.chatapplication.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

import at.htl_villach.chatapplication.R;
import at.htl_villach.chatapplication.bll.User;
import de.hdodenhof.circleimageview.CircleImageView;

public class SelectListAdapter extends BaseAdapter {
    ArrayList<User> contacts;
    Boolean[] checked;
    LayoutInflater inflater;
    DatabaseReference database;
    FirebaseAuth firebaseAuth;
    private StorageReference storageReference;

    private final long MAX_DOWNLOAD_IMAGE = 1024 * 1024 * 5;

    public SelectListAdapter(Context applicationContext, ArrayList<User> contacts) {
        this.contacts = contacts;
        this.inflater = (LayoutInflater.from(applicationContext));
        checked = new Boolean[contacts.size()];
        for(int i=0; i<checked.length; i++) {
            checked[i] = false;
        }
    }

    @Override
    public int getCount() {
        return contacts.size();
    }

    @Override
    public Object getItem(int position) {
        return contacts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        convertView = inflater.inflate(R.layout.select_list, null);
        TextView item = convertView.findViewById(R.id.txtName);
        TextView subitem = convertView.findViewById(R.id.txtUsername);
        final CircleImageView image = (CircleImageView) convertView.findViewById(R.id.list_picture);
        final CheckBox cbSelectItem = convertView.findViewById(R.id.cbSelectItem);
        final int fposition = position;
        storageReference = FirebaseStorage.getInstance().getReference();


        cbSelectItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checked[fposition] = cbSelectItem.isChecked();
            }
        });

        item.setText(contacts.get(position).getFullname());

        subitem.setText(contacts.get(position).getUsername());

        image.post(new Runnable() {
            @Override
            public void run() {
                storageReference.child("users/" + contacts.get(position).getId() + "/profilePicture.jpg").getBytes(MAX_DOWNLOAD_IMAGE)
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
        return convertView;
    }

    public void renewBooleans() {
        checked = new Boolean[contacts.size()];
        for(int i=0; i<checked.length; i++) {
            checked[i] = false;
        }
    }

    public Boolean[] getChecked() {
        return checked;
    }

}
