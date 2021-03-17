package at.htl_villach.chatapplication.bll;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by pupil on 4/22/19.
 */

public class Chat implements Parcelable {

    private String id;
    private Boolean isGroupChat;
    private HashMap<String, Boolean> users;

    public Chat(String id, HashMap<String, Boolean> users, Boolean isGroupChat) {
        this.id = id;
        this.users = users;
        this.isGroupChat = isGroupChat;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public HashMap<String, Boolean> getUsers() {
        return users;
    }

    public void setUsers(HashMap<String, Boolean> users) {
        this.users = users;
    }

    public Boolean getGroupChat() {
        return isGroupChat;
    }

    public void setGroupChat(Boolean groupChat) {
        isGroupChat = groupChat;
    }

    protected Chat(Parcel in) {
        id = in.readString();
        isGroupChat = in.readByte() != 0;
        users = in.readHashMap(String.class.getClassLoader());

    }

    public static final Creator<Chat> CREATOR = new Creator<Chat>() {
        @Override
        public Chat createFromParcel(Parcel in) {
            return new Chat(in);
        }

        @Override
        public Chat[] newArray(int size) {
            return new Chat[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeByte((byte) (isGroupChat ? 1 : 0));
        dest.writeMap(users);
    }

    public String getReceiver(String uid) {
        String result = "";
        for (Map.Entry<String, Boolean> entry : users.entrySet()) {
            if(!uid.equals(entry.getKey())){
                result = entry.getKey();
            }
        }
        return result;
    }

    public ArrayList<String> getReceivers(String uid) {
        ArrayList<String> result = new ArrayList<>();
        for (Map.Entry<String, Boolean> entry : users.entrySet()) {
            if(!uid.equals(entry.getKey())){
                result.add(entry.getKey());
            }
        }
        return result;
    }
}
