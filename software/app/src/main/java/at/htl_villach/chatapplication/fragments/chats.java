package at.htl_villach.chatapplication.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

import at.htl_villach.chatapplication.SingleChatActivity;
import at.htl_villach.chatapplication.GroupChatActivity;
import at.htl_villach.chatapplication.R;
import at.htl_villach.chatapplication.adapters.ChatListAdapter;
import at.htl_villach.chatapplication.bll.Chat;


public class chats extends Fragment {
    private ArrayList<Chat> arrChats;
    private ChatListAdapter adapter;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference database;
    private DatabaseReference database2;
    private SwipeRefreshLayout srLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_chats, container, false);
        arrChats = new ArrayList<>();

        srLayout = rootView.findViewById(R.id.srLayout);

        adapter = new ChatListAdapter(getContext(), arrChats);
        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("Chats");
        database2 = FirebaseDatabase.getInstance().getReference("Groups");

        final ListView lvChats = rootView.findViewById(R.id.lvChats);
        registerForContextMenu(lvChats);
        lvChats.setAdapter(adapter);

        if(!srLayout.isRefreshing()) {
            RefreshList();
        }

        srLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getChatsFromDatabase();
            }
        });

        lvChats.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> adapter, View v, int position,
                                    long arg3)
            {
                Chat chat = (Chat)adapter.getItemAtPosition(position);
                Intent intent;
                if(chat.getGroupChat()) {
                    intent = new Intent(getActivity(), GroupChatActivity.class);

                } else {
                    intent = new Intent(getActivity(), SingleChatActivity.class);
                }
                intent.putExtra("selectedChat", chat);
                startActivity(intent);

            }
        });

        return rootView;

    }

    public void RefreshList() {
        srLayout.setRefreshing(true);
        getChatsFromDatabase();
    }

    private void getChatsFromDatabase(){
        database.orderByChild("id")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        ArrayList<Chat> tempChat = new ArrayList<>();
                        HashMap<String, Object> chats = (HashMap<String,Object>) dataSnapshot.getValue();
                        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
                        if(chats != null) {
                            for(String key : chats.keySet()) {
                                HashMap<String, Object> curObj = (HashMap<String, Object>) chats.get(key);
                                HashMap<String, Boolean> userPair = (HashMap<String, Boolean>) curObj.get("users");
                                Boolean isGroupChat = (Boolean) curObj.get("isGroupChat");
                                if(userPair.containsKey(currentUser.getUid())) {
                                    tempChat.add(new Chat(key, userPair, isGroupChat));
                                }
                            }

                            if(!tempChat.isEmpty()) {
                                arrChats.clear();
                                arrChats.addAll(tempChat);
                                adapter.notifyDataSetChanged();
                            }
                        }

                        srLayout.setRefreshing(false);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

    }

    private void deleteChat(Chat chat) {
        database.orderByChild("id")
                .equalTo(chat.getId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot toDelete : dataSnapshot.getChildren()) {
                            toDelete.getRef().removeValue();
                        }
                        RefreshList();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
        database2.child(chat.getId())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot toDelete : dataSnapshot.getChildren()) {
                            toDelete.getRef().removeValue();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if(v.getId()==R.id.lvChats) {
            getActivity().getMenuInflater().inflate(R.menu.menu_list, menu);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.mnDeleteItem:
                AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
                Chat chat = (Chat) adapter.getItem(acmi.position);
                deleteChat(chat);
                return true;
            default:
                return super.onContextItemSelected(item);
        }

    }
}
