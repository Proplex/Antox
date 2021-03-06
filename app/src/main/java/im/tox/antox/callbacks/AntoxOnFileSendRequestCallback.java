package im.tox.antox.callbacks;

import android.content.Context;
import android.util.Log;

import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.AntoxFriend;
import im.tox.jtoxcore.callbacks.OnFileSendRequestCallback;

public class AntoxOnFileSendRequestCallback implements OnFileSendRequestCallback<AntoxFriend> {

    private static final String TAG = "OnFileSendRequestCallback";
    private Context ctx;
    ToxSingleton toxSingleton = ToxSingleton.getInstance();

    public AntoxOnFileSendRequestCallback(Context ctx) { this.ctx = ctx; };

    public void execute(AntoxFriend friend, int filenumber, long filesize, byte[] filename) {
        Log.d(TAG, "execute");
        toxSingleton.fileSendRequest(friend.getId(), filenumber, new String(filename), filesize, ctx);
    }
}
