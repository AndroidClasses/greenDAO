package org.greenrobot.greendao.rx;

/**
 * Created by yangfeng on 17-4-17.
 *
 * RxJava2 throws NullPointerException against null, the dumb class NullStub and
 * its NULL object here prefer to trim the use of null and make RxJava2 happy.
 *
 */
public final class NullStub {
    public static final NullStub NULL = new NullStub();

    private NullStub() {
    }
}
