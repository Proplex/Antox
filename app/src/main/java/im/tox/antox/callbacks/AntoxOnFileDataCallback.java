package im.tox.antox.callbacks;

import android.content.Context;
import android.util.Log;

import im.tox.antox.tox.ToxSingleton;
import im.tox.antox.utils.AntoxFriend;
import im.tox.jtoxcore.callbacks.OnFileDataCallback;

public class AntoxOnFileDataCallback implements OnFileDataCallback<AntoxFriend> {

    private static final String TAG = "OnFileDataCallback";
    private Context ctx;
    ToxSingleton toxSingleton = ToxSingleton.getInstance();

    public AntoxOnFileDataCallback(Context ctx) { this.ctx = ctx; };

    public void execute(AntoxFriend friend, int filenumber, byte[] data) {
        Log.d(TAG, "execute");
        toxSingleton.receiveFileData(friend.getId(), filenumber, data, ctx);
    }
}
