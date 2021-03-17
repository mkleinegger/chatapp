package at.htl_villach.chatapplication.bll;

import android.os.Parcel;
import android.os.Parcelable;

public class User implements Parcelable {

    private String id;
    private String fullname;
    private String email;
    private String username;
    private byte[] profilePictureResource;

    public User() {
        super();
        this.id = null;
        this.email = null;
        this.username = null;
        this.fullname = null;
        this.profilePictureResource = new byte[0];
    }


    public User(String id, String email, String username) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.profilePictureResource = new byte[0];
        this.fullname = null;
    }

    protected User(Parcel in) {
        id = in.readString();
        email = in.readString();
        username = in.readString();
        fullname = in.readString();
        profilePictureResource = new byte[in.readInt()];
        in.readByteArray(profilePictureResource);
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(email);
        dest.writeString(username);
        dest.writeString(fullname);
        dest.writeInt(profilePictureResource.length);
        dest.writeByteArray(profilePictureResource);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) { this.username = username; }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public byte[] getProfilePictureResource() {
        return profilePictureResource;
    }

    public void setProfilePictureResource(byte[] profilePictureResource) {
        this.profilePictureResource = profilePictureResource;
    }
}
