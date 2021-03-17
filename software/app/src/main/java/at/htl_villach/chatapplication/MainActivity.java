package at.htl_villach.chatapplication;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.HashMap;

import at.htl_villach.chatapplication.adapters.PagerAdapter;
import at.htl_villach.chatapplication.adapters.SelectListAdapter;
import at.htl_villach.chatapplication.bll.Chat;
import at.htl_villach.chatapplication.bll.User;
import at.htl_villach.chatapplication.fragments.contacts;

public class MainActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    DatabaseReference database;
    DatabaseReference database2;
    DatabaseReference database3;
    ArrayList<User> arrUsers;
    SelectListAdapter slAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TabLayout tbMain = findViewById(R.id.tbMain);

        tbMain.setNestedScrollingEnabled(false);

        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("Users");
        database2 = FirebaseDatabase.getInstance().getReference("Chats");
        database3 = FirebaseDatabase.getInstance().getReference("Groups");
        //tbMain.setTabGravity(TabLayout.GRAVITY_FILL);

        arrUsers = new ArrayList<>();
        slAdapter = new SelectListAdapter(MainActivity.this, arrUsers);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_toolbar);
        toolbar.setTitle("ChatApp");

        final ViewPager pager = findViewById(R.id.pager);
        final PagerAdapter adapter = new PagerAdapter(getSupportFragmentManager(), tbMain.getTabCount());
        pager.setAdapter(adapter);
        pager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tbMain));


        tbMain.addOnTabSelectedListener(new TabLayout.BaseOnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                pager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }

        });

        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if(menuItem.getItemId() == R.id.mnLogout) {
                    firebaseAuth.signOut();
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.putExtra("allowBack", false);
                    startActivity(intent);
                    finish();
                    return true;
                } else if(menuItem.getItemId() == R.id.mnAddGroup) {
                    createSelectDialog();
                    return true;
                } else if(menuItem.getItemId() == R.id.mnEditProfile) {
                    Toast.makeText(MainActivity.this, "Temporary toast for menuItem Edit!",
                            Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, EditProfileActivity.class);
                    intent.putExtra("allowBack", false);
                    startActivity(intent);
                    return true;
                } else if(menuItem.getItemId() == R.id.mnDeleteProfile) {
                    startActivity(new Intent(MainActivity.this, popupDeleteDialogActivity.class));

                    Toast.makeText(MainActivity.this, "Temporary toast for menuItem Delete!",
                            Toast.LENGTH_SHORT).show();
                    return true; //fragen
                }
                return false;
            }
        });



        final FloatingActionButton btnAddUser = findViewById(R.id.btnAddUser);

        btnAddUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddUserActivity.class);
                startActivity(intent);
            }
        });

    }

    private void createSelectDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Choose Friends");
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        getFriendsFromDatabase();
        //selectDialog.setContentView(R.layout.asdf);

        //ListView lvSelect = selectDialog.findViewById(R.id.lvSelect);
        //lvSelect.setAdapter(slAdapter);
        builder.setAdapter(slAdapter, null);

        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final DialogInterface fdialog = dialog;
                Boolean[] checked = slAdapter.getChecked();
                final ArrayList<User> selectedUsers = new ArrayList<>();
                for(int i=0; i<checked.length; i++) {
                    if(checked[i]) {
                        selectedUsers.add((User) slAdapter.getItem(i));
                    }
                }
                database2.push().addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        String id = dataSnapshot.getKey();
                        HashMap<String, Boolean> users = new HashMap<>();
                        users.put(firebaseAuth.getCurrentUser().getUid(), true);
                        for(User u : selectedUsers) {
                            users.put(u.getId(), true);
                        }


                        final Chat newChatObject = new Chat(id, users, true);


                        fdialog.dismiss();
                        createGroupNameDialog(newChatObject);


                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });


            }
        });

        /*builder.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d("test", "test");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });*/


        /*AlertDialog dialog = builder.create();
        dialog.getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckBox cb = view.findViewById(R.id.cbSelectItem);
                cb.performClick();
            }
        });*/

        /*builder.setAdapter(slAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                View view = slAdapter.getView(which, new View(MainActivity.this), null);
                CheckBox cb = view.findViewById(R.id.cbSelectItem);
                cb.performClick();
            }
        });*/


        builder.show();
    }


    private void createGroupNameDialog(Chat newChat) {
        final Chat fNewChat = newChat;
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Choose Group Name");
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        final EditText input = new EditText(MainActivity.this);
        input.setHint("Group Name");
        input.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));

        builder.setView(input);
        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final Intent intent = new Intent(MainActivity.this, GroupChatActivity.class);
                intent.putExtra("selectedChat", fNewChat);

                final HashMap<String, Object> writeNewChat = new HashMap<>();
                writeNewChat.put("id", fNewChat.getId());
                writeNewChat.put("isGroupChat", fNewChat.getGroupChat());
                writeNewChat.put("users", fNewChat.getUsers());

                final HashMap<String, Object> writeNewGroup = new HashMap<>();
                writeNewGroup.put("title", input.getText().toString());
                writeNewGroup.put("picture", null);

                database3.child(fNewChat.getId()).setValue(writeNewGroup)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                database2.child(fNewChat.getId()).setValue(writeNewChat)
                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if(task.isSuccessful()) {
                                                    startActivity(intent);
                                                }
                                            }
                                        });
                            }
                        });

            }
        });
        builder.show();

    }

    private void getFriendsFromDatabase() {
        database.child(firebaseAuth.getCurrentUser().getUid()).child("friends")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        HashMap<String, String> friend = (HashMap<String, String>) dataSnapshot.getValue();
                        if(friend != null) {
                            arrUsers.clear();
                            for (String key : friend.keySet()) {

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
                                                slAdapter.renewBooleans();
                                                slAdapter.notifyDataSetChanged();

                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });
                            }
                        }
                    }


                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }
}
