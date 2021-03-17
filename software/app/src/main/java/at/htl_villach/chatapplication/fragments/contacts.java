package at.htl_villach.chatapplication.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

import at.htl_villach.chatapplication.ProfileActivity;
import at.htl_villach.chatapplication.R;
import at.htl_villach.chatapplication.adapters.ContactListAdapter;
import at.htl_villach.chatapplication.bll.User;


public class contacts extends Fragment {
    private ArrayList<User> arrUsers;
    private ArrayList<String> arrFriends;
    private ContactListAdapter adapter;
    private SwipeRefreshLayout srLayout;
    DatabaseReference database;
    FirebaseAuth firebaseAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_contacts, container, false);
        srLayout = rootView.findViewById(R.id.srLayout);

        database = FirebaseDatabase.getInstance().getReference("Users");
        firebaseAuth = FirebaseAuth.getInstance();

        arrUsers = new ArrayList<User>();
        arrFriends = new ArrayList<>();

        adapter = new ContactListAdapter(getContext(), arrUsers);

        final ListView lvContacts = rootView.findViewById(R.id.lvContacts);
        srLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getFriendsFromDatabase();
            }
        });


        lvContacts.setAdapter(adapter);

        if(!srLayout.isRefreshing()) {
            RefreshList();
        }

        lvContacts.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position,
                                    long arg3) {
                User contact = (User) adapter.getItemAtPosition(position);
                Intent intent = new Intent(getActivity(), ProfileActivity.class);

                intent.putExtra("selectedContact", contact);

                startActivity(intent);

            }
        });

        return rootView;

    }

    public void RefreshList() {
        srLayout.setRefreshing(true);
        getFriendsFromDatabase();
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
                                                srLayout.setRefreshing(false);
                                                adapter.notifyDataSetChanged();

                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });
                            }
                        }
                        srLayout.setRefreshing(false);
                    }


                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }




}
