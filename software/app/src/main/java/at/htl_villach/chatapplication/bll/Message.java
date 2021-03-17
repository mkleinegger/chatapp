package at.htl_villach.chatapplication.bll;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.Locale;

/**
 * Created by pupil on 4/22/19.
 */

public class Message implements Parcelable {
    private String sender;
    private String id;
    private String message;
    private String type;
    private Long timestamp;
    private boolean seen;

    public Message() { }

    public Message(String sender, String id, String message, String type, Long timestamp, boolean seen) {
        this.sender = sender;
        this.id = id;
        this.message = message;
        this.type = type;
        this.timestamp = timestamp;
        this.seen = seen;
    }

    protected Message(Parcel in) {
        sender = in.readString();
        id = in.readString();
        message = in.readString();
        type = in.readString();
        if (in.readByte() == 0) {
            timestamp = null;
        } else {
            timestamp = in.readLong();
        }
        seen = in.readByte() != 0;
    }

    public static final Creator<Message> CREATOR = new Creator<Message>() {
        @Override
        public Message createFromParcel(Parcel in) {
            return new Message(in);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isSeen() {
        return seen;
    }

    public void setSeen(boolean seen) {
        this.seen = seen;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTimeAsString() {
        Calendar cal = Calendar.getInstance(Locale.GERMANY);
        cal.setTimeInMillis(this.timestamp * 1000L);
        return DateFormat.format("hh:mm", cal).toString();
    }

    public String getTimeAsDate() {
        Calendar cal = Calendar.getInstance(Locale.GERMANY);
        cal.setTimeInMillis(this.timestamp * 1000L);
        return DateFormat.format("dd. MMMM yyyy", cal).toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(sender);
        dest.writeString(id);
        dest.writeString(message);
        dest.writeString(type);
        if (timestamp == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeLong(timestamp);
        }
        dest.writeByte((byte) (seen ? 1 : 0));
    }

    @Override
    public boolean equals(Object obj) {
        Message m = (Message) obj;

        return this.getId().equals(m.getId());
    }
}
