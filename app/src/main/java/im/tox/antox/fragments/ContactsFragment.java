package im.tox.antox.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import com.shamanland.fab.FloatingActionButton;
import com.shamanland.fab.ShowHideOnScroll;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import im.tox.antox.R;
import im.tox.antox.activities.FriendProfileActivity;
import im.tox.antox.adapters.LeftPaneAdapter;
import im.tox.antox.data.AntoxDB;
import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.AntoxFriend;
import im.tox.antox.utils.Constants;
import im.tox.antox.utils.FriendInfo;
import im.tox.antox.utils.FriendRequest;
import im.tox.antox.utils.LeftPaneItem;
import im.tox.antox.utils.Tuple;
import im.tox.jtoxcore.FriendExistsException;
import im.tox.jtoxcore.ToxException;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by ollie on 28/02/14.
 */
public class ContactsFragment extends Fragment {
    /**
     * List View for displaying all the friends in a scrollable list
     */
    private ListView contactsListView;
    /**
     * Adapter for the friendListView
     */
    private LeftPaneAdapter leftPaneAdapter;

    ToxSingleton toxSingleton = ToxSingleton.getInstance();

    private Subscription friendInfoSub;
    private Subscription keySub;

    private String activeKey;

    public ContactsFragment() {
    }

    public void updateContacts(Tuple<ArrayList<FriendInfo>,ArrayList<FriendRequest>> friendstuple) {
        ArrayList<FriendInfo> friendsList = friendstuple.x;
        ArrayList<FriendRequest> friendRequests = friendstuple.y;

        Collections.sort(friendsList, new NameComparator());
        Collections.sort(friendsList, new OnlineComparator());

        leftPaneAdapter = new LeftPaneAdapter(getActivity());
        FriendRequest friend_requests[] = new FriendRequest[friendRequests.size()];
        friend_requests = friendRequests.toArray(friend_requests);
        if (friend_requests.length > 0) {
            leftPaneAdapter.addItem(new LeftPaneItem(getResources().getString(R.string.contacts_delimiter_requests)));
            for (int i = 0; i < friend_requests.length; i++) {
                LeftPaneItem request = new LeftPaneItem(friend_requests[i].requestKey, friend_requests[i].requestMessage);
                leftPaneAdapter.addItem(request);
            }
        }
        FriendInfo friends_list[] = new FriendInfo[friendsList.size()];
        friends_list = friendsList.toArray(friends_list);
        if (friends_list.length > 0) {
            if (friend_requests.length > 0) {
                leftPaneAdapter.addItem(new LeftPaneItem(getResources().getString(R.string.contacts_delimiter_friends)));
            }

            boolean onlineAdded = false;
            boolean offlineAdded = false;
            for (int i = 0; i < friends_list.length; i++) {
                if(!offlineAdded && !friends_list[i].isOnline) {
                    leftPaneAdapter.addItem(new LeftPaneItem(getResources().getString(R.string.contacts_delimiter_offline)));
                    offlineAdded = true;
                }
                if(!onlineAdded && friends_list[i].isOnline) {
                    leftPaneAdapter.addItem(new LeftPaneItem(getResources().getString(R.string.contacts_delimiter_online)));
                    onlineAdded = true;
                }
                LeftPaneItem friend = new LeftPaneItem(friends_list[i].friendKey, friends_list[i].friendName, friends_list[i].lastMessage, friends_list[i].isOnline, friends_list[i].getFriendStatusAsToxUserStatus(), friends_list[i].unreadCount, friends_list[i].lastMessageTimestamp);
                leftPaneAdapter.addItem(friend);
            }
        }
        contactsListView.setAdapter(leftPaneAdapter);
        setSelectionToKey(activeKey);
        System.out.println("updated contacts");
    }

    private void setSelectionToKey(String key) {
        if (key != null && !key.equals("")) {
            for (int i = 0; i < leftPaneAdapter.getCount(); i++) {
                if (leftPaneAdapter.getKey(i).equals(key)) {
                    contactsListView.setSelection(i);
                    break;
                }
            }
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        friendInfoSub = toxSingleton.friendListAndRequestsSubject.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Tuple<ArrayList<FriendInfo>,ArrayList<FriendRequest>>>() {
                    @Override
                    public void call(Tuple<ArrayList<FriendInfo>,ArrayList<FriendRequest>> friendstuple) {
                        updateContacts(friendstuple);
                    }
                });
        keySub = toxSingleton.activeKeySubject.observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String s) {
                        Log.d("ContactsFragment", "key subject");
                        activeKey = s;
                        setSelectionToKey(activeKey);
                    }
                });

        FloatingActionButton fab = (FloatingActionButton) getActivity().findViewById(R.id.fab);
        fab.setSize(FloatingActionButton.SIZE_NORMAL);
        fab.setColor(Color.parseColor("#6BC25F"));
        fab.initBackground();
        fab.setImageResource(R.drawable.ic_action_new);
        contactsListView.setOnTouchListener(new ShowHideOnScroll(fab));
    }

    @Override
    public void onPause(){
        super.onPause();
        friendInfoSub.unsubscribe();
        keySub.unsubscribe();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        /**
         * Stores a 2 dimensional string array holding friend details. Will be populated
         * by a tox function once implemented
         */

        View rootView = inflater.inflate(R.layout.fragment_contacts, container, false);
        contactsListView = (ListView) rootView.findViewById(R.id.contacts_list);
        contactsListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        contactsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                LeftPaneItem item = (LeftPaneItem) parent.getAdapter().getItem(position);
                int type = item.viewType;

                if (type != Constants.TYPE_FRIEND_REQUEST) {
                    String key = item.key;
                    if (!key.equals("")) {
                        setSelectionToKey(key);
                        toxSingleton.changeActiveKey(key);
                    }
                }
            }
        });

        contactsListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View itemView, int index, long id) {
                final LeftPaneItem item = (LeftPaneItem) parent.getAdapter().getItem(index);
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                final boolean isFriendRequest = item.viewType == Constants.TYPE_FRIEND_REQUEST;
                CharSequence items[] = { "" };

                if (!isFriendRequest) {
                    items = new CharSequence[]{
                            getResources().getString(R.string.friend_action_profile),
                            getResources().getString(R.string.friend_action_delete),
                            getResources().getString(R.string.friend_action_delete_chat),
                            getResources().getString(R.string.contacts_resend_friend_request)
                    };
                }
                builder.setTitle(getResources().getString(R.string.contacts_actions_on) + " " + item.first)
                        .setCancelable(true)
                        .setItems(items, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int index) {
                                //item.first equals the key
                                if (!isFriendRequest) {
                                    String key = item.key;
                                    if (!key.equals("")) {
                                        switch (index) {
                                            case 0:
                                                Intent profile = new Intent(getActivity(), FriendProfileActivity.class);
                                                profile.putExtra("key", key);
                                                startActivity(profile);
                                                break;
                                            case 1:
                                                // Delete friend
                                                showDeleteFriendDialog(getActivity(), key);
                                                break;
                                            case 2:
                                                // Delete chat logs
                                                showDeleteChatDialog(getActivity(), key);
                                                break;

                                            case 3:
                                                // Resend Friend Request
                                                try {
                                                    toxSingleton.jTox.addFriend(key, getResources().getString(R.string.addfriend_default_message));
                                                } catch (ToxException e) {
                                                } catch (FriendExistsException e) {
                                                }
                                                break;
                                        }
                                    }
                                }
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                if(item != null) {
                    if (item.viewType != Constants.TYPE_HEADER) {
                        alert.show();
                    }
                }
                return true;
            }
        });

        /* Search function */
        EditText search = (EditText) rootView.findViewById(R.id.searchBar);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                if(leftPaneAdapter != null)
                    leftPaneAdapter.getFilter().filter(charSequence);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        return rootView;
    }

    public void showDeleteFriendDialog(Context context, String fkey) {
        final String key= fkey;
        View delete_friend_dialog = View.inflate(context, R.layout.dialog_delete_friend,null);
        final CheckBox deleteLogsCheckboxView = (CheckBox) delete_friend_dialog.findViewById(R.id.deleteChatLogsCheckBox);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder .setView(delete_friend_dialog)
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog,
                                        int id) {
                        class DeleteFriendAndChat extends AsyncTask<Void, Void, Void> {
                            @Override
                            protected Void doInBackground(Void... params) {
                                AntoxDB db = new AntoxDB(getActivity());
                                if (deleteLogsCheckboxView.isChecked())
                                    db.deleteChat(key);
                                db.deleteFriend(key);
                                db.close();
                                // Remove friend from tox friend list
                                AntoxFriend friend = toxSingleton.getAntoxFriend(key);
                                if (friend != null) {

                                    try {
                                        toxSingleton.jTox.deleteFriend(friend.getFriendnumber());
                                    } catch (ToxException e) {
                                    }
                                }
                                return null;
                            }

                            @Override
                            protected void onPostExecute(Void result) {
                                toxSingleton.updateFriendsList(getActivity());
                                toxSingleton.updateMessages(getActivity());
                            }
                        }

                        new DeleteFriendAndChat().execute();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        builder.show();
    }

    public void showDeleteChatDialog(Context context, String fkey) {
        final String key= fkey;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(getResources().getString(R.string.friend_action_delete_chat_confirmation))
                .setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.button_yes),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                                AntoxDB db = new AntoxDB(getActivity());
                                db.deleteChat(key);
                                db.close();
                                toxSingleton.updateMessages(getActivity());
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.button_no),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int id) {
                            }
                        }
                );
        builder.show();
    }

    private class NameComparator implements Comparator<FriendInfo> {
        @Override
        public int compare(FriendInfo a, FriendInfo b) {
            if(!a.alias.equals("")) {
                if(!b.alias.equals(""))
                    return a.alias.toUpperCase().compareTo(b.alias.toUpperCase());
                else
                    return a.alias.toUpperCase().compareTo(b.friendName.toUpperCase());
            } else {
                if(!b.alias.equals(""))
                    return a.friendName.toUpperCase().compareTo(b.alias.toUpperCase());
                else
                    return a.friendName.toUpperCase().compareTo(b.friendName.toUpperCase());
            }
        }
    }

    private class OnlineComparator implements Comparator<FriendInfo> {
        @Override
        public int compare(FriendInfo a, FriendInfo b) {
            if (a.isOnline && !b.isOnline)
                return -1;
            else if (!a.isOnline && b.isOnline)
                return 1;
            else
                return 0;
        }
    }

}
