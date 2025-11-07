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

/**
 * Class displays a list of entrant and organizers for the admin to promote/demote and/or delete
 */
public class UserManagerFragment extends Fragment {

    private RecyclerView rvUsers;
    private SearchView searchBar;
    private ManageUsersAdapter adapter;
    private FireStoreHelper helper;
    private ListenerRegistration reg;

    /**
     * Creates a the fragment by defining all the text
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return view
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_manager, container, false);

        rvUsers = view.findViewById(R.id.rvUsers);
        rvUsers.setLayoutManager(new LinearLayoutManager(getContext()));
        helper = new FireStoreHelper();
        searchBar = view.findViewById(R.id.searchView);
        adapter = new ManageUsersAdapter(new ManageUsersAdapter.UserActionListener() {
            /**
             * class handles promotion to organizer if entrant
             * @param user
             */
            @Override
            public void onPromote(User user) {
                helper.updateUserType(user.getUser_id(), "organizer");
            }

            /**
             * class handles demotion to entrant if organizer
             * @param user
             */
            @Override
            public void onDemote(User user) {
                helper.updateUserType(user.getUser_id(), "entrant");
            }

            /**
             * class handles deletion of user by admin
             * @param user
             */
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
            /**
             * handles when user hits submit on searchbar
             * @param query the query text that is to be submitted
             *
             * @return false
             * required for searchBar
             */
            @Override
            public boolean onQueryTextSubmit(String query) {
                adapter.getFiltered(query);
                return false;
            }

            /**
             * handles changing the list as user types
             * @param newText the new content of the query text field.
             *
             * @return False
             * required for searchBar
             */
            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFiltered(newText);
                return false;
            }
        });

        rvUsers.setAdapter(adapter);

        // listen to organizers + entrants
        reg = helper.listenToManageableUsers(new FireStoreHelper.ManageUsersCallback() {
            /**
             * loads the users into adapter
             * @param users
             * take in users from fragment
             */
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

    /**
     * Destroys the Fragment when going to another
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (reg != null) {
            reg.remove();
            reg = null;
        }
    }


}