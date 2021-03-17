package at.htl_villach.chatapplication.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.ViewGroup;

import at.htl_villach.chatapplication.fragments.chats;
import at.htl_villach.chatapplication.fragments.contacts;
import at.htl_villach.chatapplication.fragments.requests;

public class PagerAdapter extends FragmentStatePagerAdapter {
    private int numberTabs;
    private Fragment currFragment;

    public Fragment getCurrentFragment() {
        return currFragment;
    }

    public PagerAdapter(FragmentManager fm, int numTabs) {
        super(fm);
        this.numberTabs = numTabs;
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                chats chatSection = new chats();
                return chatSection;
            case 1:
                contacts contactSection = new contacts();
                return contactSection;
            case 2:
                requests requestSection = new requests();
                return requestSection;
            default:
                return null;

        }
    }

    @Override
    public int getCount() {
        return numberTabs;
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        if (getCurrentFragment() != object) {
            currFragment = (Fragment) object;
        }
        super.setPrimaryItem(container, position, object);
    }
}
