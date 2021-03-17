package at.htl_villach.chatapplication;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import at.htl_villach.chatapplication.adapters.ContactListAdapter;
import at.htl_villach.chatapplication.bll.Chat;
import at.htl_villach.chatapplication.bll.User;
import de.hdodenhof.circleimageview.CircleImageView;

public class GroupInfoActivity extends AppCompatActivity {

    private DatabaseReference database;
    private DatabaseReference database2;
    private StorageReference storageReference;
    private FirebaseAuth firebaseAuth;
    private ArrayList<User> arrUsers;
    private Chat groupChat;
    private ContactListAdapter adapter;
    private final long MAX_DOWNLOAD_IMAGE = 1024 * 1024 * 5;
    private CircleImageView imgGroupPicture;
    private Uri imageUri;
    private Toolbar toolGroupName;
    private EditText editGroupName;

    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int REQUEST_PICK_PHOTO = 2;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    public static final int PICK_IMAGE = 1;
    public static final int TAKE_PICTURE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_info);
        toolGroupName = findViewById(R.id.toolGroupInfo);
        toolGroupName.setTitle("Group Info");
        toolGroupName.setNavigationIcon(R.drawable.ic_acion_back);
        toolGroupName.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        toolGroupName.inflateMenu(R.menu.menu_group_info);
        Intent intent = getIntent();

        groupChat = intent.getParcelableExtra("groupChat");
        assert groupChat.getGroupChat();
        arrUsers = new ArrayList<>();

        imgGroupPicture = findViewById(R.id.imgGroupPicture);

        final ListView listMembers = findViewById(R.id.listMembers);
        final LinearLayout linearLayout = findViewById(R.id.linearLayout);

        storageReference = FirebaseStorage.getInstance().getReference();
        database = FirebaseDatabase.getInstance().getReference("Users");
        database2 = FirebaseDatabase.getInstance().getReference("Groups");
        firebaseAuth = FirebaseAuth.getInstance();


        adapter = new ContactListAdapter(GroupInfoActivity.this, arrUsers);
        listMembers.setAdapter(adapter);
        adapter.notifyDataSetChanged();

        getUsersDatabase();

        listMembers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                User contact = (User) adapter.getItem(position);
                Intent intent = new Intent(getApplicationContext(), ProfileActivity.class);

                intent.putExtra("selectedContact", contact);

                startActivity(intent);
            }
        });

        refreshPicture();
        loadGroupName();

        toolGroupName.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.mnEdit) {
                    String groupName = toolGroupName.getTitle().toString();

                    editGroupName = new EditText(getApplicationContext());
                    editGroupName.setText(groupName);
                    editGroupName.setTextSize(20);
                    editGroupName.setBackground(Drawable.createFromPath("@android:color/transparent"));
                    editGroupName.setMaxLines(1);
                    editGroupName.setInputType(InputType.TYPE_CLASS_TEXT);

                    toolGroupName.addView(editGroupName);

                    editGroupName.getLayoutParams().width = ActionBar.LayoutParams.MATCH_PARENT;
                    toolGroupName.setTitle("");
                    toolGroupName.getMenu().clear();
                    toolGroupName.inflateMenu(R.menu.menu_group_info_edit);
                    editGroupName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                            if(actionId == EditorInfo.IME_ACTION_DONE) {
                                toolGroupName.getMenu().performIdentifierAction(R.id.mnEditCheck,0);
                                return true;
                            }
                            return false;
                        }
                    });
                    return true;
                } else if (menuItem.getItemId() == R.id.mnEditCheck) {
                    String groupName = editGroupName.getText().toString();
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(editGroupName.getWindowToken(), 0);
                    toolGroupName.removeView(editGroupName);
                    toolGroupName.setTitle(groupName);
                    toolGroupName.getMenu().clear();
                    toolGroupName.inflateMenu(R.menu.menu_group_info);
                    updateGroupName(groupName);
                    return true;
                }
                return false;
            }
        });


        imgGroupPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String[] items = {"Choose from Gallery", "Open Camera", "Delete Current Picture"};
                AlertDialog.Builder builder = new AlertDialog.Builder(GroupInfoActivity.this);
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
                            case 2:
                                deleteProfilePicture();
                                break;
                            default:
                                Toast.makeText(getApplicationContext(), "Something unexpected happened", Toast.LENGTH_SHORT).show();
                                break;
                        }
                    }
                });
                builder.show();
            }
        });

    }

    private void refreshPicture() {
        storageReference.child("groups/" + groupChat.getId() + "/profilePicture.jpg").getBytes(MAX_DOWNLOAD_IMAGE)
                .addOnSuccessListener(new OnSuccessListener<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        imgGroupPicture.setImageBitmap(Bitmap.createScaledBitmap(bitmap, imgGroupPicture.getWidth(),
                                imgGroupPicture.getHeight(), false));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        imgGroupPicture.setImageResource(R.drawable.standard_picture);
                    }
                });
    }

    private void loadGroupName() {
        database2.child(groupChat.getId())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        HashMap<String, String> group = (HashMap<String, String>) dataSnapshot.getValue();
                        if (group != null) {
                            String title = group.get("title");
                            toolGroupName.setTitle(title);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void updateGroupName(String groupName) {
        HashMap<String, Object> updateTitle = new HashMap<>();
        updateTitle.put("title", groupName);
        database2.child(groupChat.getId())
                .updateChildren(updateTitle)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), "Could not be saved to database", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void getUsersDatabase() {
        for (String key : groupChat.getUsers().keySet()) {
            if(!key.equals(firebaseAuth.getCurrentUser().getUid())) {
                database.orderByChild("id")
                        .equalTo(key)
                        .addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                HashMap<String, HashMap<String, String>> user = (HashMap<String, HashMap<String, String>>) dataSnapshot.getValue();
                                if (user != null) {
                                    User userObject = new User();
                                    for (String key : user.keySet()) {
                                        userObject.setUsername(user.get(key).get("username"));
                                        userObject.setFullname(user.get(key).get("fullname"));
                                        userObject.setEmail(user.get(key).get("email"));
                                        userObject.setId(user.get(key).get("id"));
                                    }
                                    arrUsers.add(userObject);
                                }

                                adapter.notifyDataSetChanged();

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
            }
        }
    }

    private void checkPermissionAndTakePhotoIfGranted() {
        int permission = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(GroupInfoActivity.this, PERMISSIONS_STORAGE, REQUEST_TAKE_PHOTO);
        } else {
            captureImage();
        }
    }

    private void checkPermissionAndPickPhotoIfGranted() {
        int permission = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(GroupInfoActivity.this, PERMISSIONS_STORAGE, REQUEST_PICK_PHOTO);
        } else {
            pickImage();
        }
    }

    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photo = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/profilePicture.jpg");
        imageUri = Uri.fromFile(photo);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, TAKE_PICTURE);
    }

    private void pickImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Choose a Picture"), PICK_IMAGE);
    }

    private void saveImageToDatabase() {
        storageReference.child("groups/" + groupChat.getId() + "/profilePicture.jpg").putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Toast.makeText(getApplicationContext(), "Image saved", Toast.LENGTH_SHORT).show();
                        refreshPicture();

                    }
                });
    }

    private Bitmap createSquaredBitmap(Bitmap source) {
        int dim = Math.max(source.getWidth(), source.getHeight());
        Bitmap dstBmp = Bitmap.createBitmap(dim, dim, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(dstBmp);
        canvas.drawColor(Color.WHITE);
        canvas.drawBitmap(source, (dim - source.getWidth()) / 2, (dim - source.getHeight()) / 2, null);

        return dstBmp;
    }

    private void checkOrientation() throws Exception {
        String[] filePathColumn = {MediaStore.Images.Media.DATA};

        String filePath;

        Cursor cursor = getApplicationContext().getContentResolver().query(imageUri, filePathColumn, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            filePath = cursor.getString(columnIndex);
            cursor.close();
        } else {
            filePath = imageUri.getPath();
        }


        ExifInterface exif = new ExifInterface(filePath);
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
        bitmap = createSquaredBitmap(bitmap);

        if (orientation == 6) {
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            FileOutputStream out = new FileOutputStream(filePath);
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 40, out);
        } else {
            FileOutputStream out = new FileOutputStream(filePath);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 40, out);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        try {
            if (resultCode == Activity.RESULT_OK) {
                if (requestCode == PICK_IMAGE) {
                    imageUri = data.getData();
                    checkOrientation();
                    saveImageToDatabase();
                } else if (requestCode == TAKE_PICTURE) {
                    checkOrientation();
                    saveImageToDatabase();

                }
            }
        } catch (Exception ex) {
            Log.e("app", "onActivityResult", ex);
            Toast.makeText(getApplicationContext(), ex.getMessage(), Toast.LENGTH_LONG).show();
        }

    }

    private void deleteProfilePicture() {
        storageReference.child("groups/" + groupChat.getId() + "/profilePicture.jpg")
                .delete()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        refreshPicture();
                    }
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_TAKE_PHOTO) {
            int permission = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permission == PackageManager.PERMISSION_GRANTED) {
                captureImage();
            }
        } else if (requestCode == REQUEST_PICK_PHOTO) {
            int permission = ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permission == PackageManager.PERMISSION_GRANTED) {
                pickImage();
            }
        }
    }
}
