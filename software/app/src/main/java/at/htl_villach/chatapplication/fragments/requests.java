package at.htl_villach.chatapplication.fragments;

import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

import at.htl_villach.chatapplication.R;
import at.htl_villach.chatapplication.adapters.RequestListAdapter;
import at.htl_villach.chatapplication.bll.User;

public class requests extends Fragment {
    private ArrayList<String> arrUids;
    private RequestListAdapter adapter;
    private SwipeRefreshLayout srLayout;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference database;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_requests, container, false);
        srLayout = rootView.findViewById(R.id.srLayout);

        arrUids = new ArrayList<>();

        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance().getReference("Requests");

        adapter = new RequestListAdapter(getContext(), arrUids);
        final ListView lvContacts = rootView.findViewById(R.id.lvRequests);
        lvContacts.setAdapter(adapter);

        srLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getRequestsFromDatabase();
            }
        });

        if(!srLayout.isRefreshing()) {
            RefreshList();
        }

        return rootView;
    }

    private void RefreshList() {
        srLayout.setRefreshing(true);
        getRequestsFromDatabase();
    }

    private void getRequestsFromDatabase() {
        database.child(firebaseAuth.getCurrentUser().getUid())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        HashMap<String, Object> requests = (HashMap<String, Object>) dataSnapshot.getValue();
                        arrUids.clear();
                        if(requests != null) {

                            arrUids.addAll(requests.keySet());
                        }
                        adapter.notifyDataSetChanged();
                        srLayout.setRefreshing(false);

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

}
