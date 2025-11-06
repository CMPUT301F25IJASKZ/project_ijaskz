package com.ijaskz.lotteryeventapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.ListenerRegistration;
import com.ijaskz.lotteryeventapp.FireStoreHelper;
import com.ijaskz.lotteryeventapp.User;

import java.util.List;

public class UserManagerFragment extends Fragment {

    private RecyclerView rvUsers;
    private SearchView searchBar;
    private ManageUsersAdapter adapter;
    private FireStoreHelper helper;
    private ListenerRegistration reg;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_manager, container, false);

        rvUsers = view.findViewById(R.id.rvUsers);
        rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        helper = new FireStoreHelper();
        searchBar = view.findViewById(R.id.searchView);
        adapter = new ManageUsersAdapter(new ManageUsersAdapter.UserActionListener() {
            @Override
            public void onPromote(User user) {
                helper.updateUserType(user.getUser_id(), "organizer");
            }

            @Override
            public void onDemote(User user) {
                helper.updateUserType(user.getUser_id(), "entrant");
            }

            @Override
            public void onDelete(User user) {
                helper.deleteUser(user.getUser_id());
            }

        });
        adapter.setOnUserClickListener(user->{
                Bundle b = new Bundle();
                b.putSerializable("user", user);
                ProfileFragment fragment = new ProfileFragment();
                fragment.setArguments(b);

                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
        });
        searchBar.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.getFiltered(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFiltered(newText);
                return false;
            }
        });

        rvUsers.setAdapter(adapter);

        // listen to organizers + entrants
        reg = helper.listenToManageableUsers(new FireStoreHelper.ManageUsersCallback() {
            @Override
            public void onUsersLoaded(List<User> users) {
                adapter.setUsers(users);
            }

            @Override
            public void onError(Exception e) {
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (reg != null) {
            reg.remove();
            reg = null;
        }
    }


}