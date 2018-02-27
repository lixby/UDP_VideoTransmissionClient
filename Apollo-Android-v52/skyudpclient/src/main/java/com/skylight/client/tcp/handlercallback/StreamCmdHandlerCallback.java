package com.skylight.client.tcp.handlercallback;

import com.skylight.command.callback.ICmdHandlerCallback;

/**
 * Description:
 * Author: Created by lixby on 17-12-27.
 */

public abstract class StreamCmdHandlerCallback extends ICmdHandlerCallback {

    /**
     *
     * @param channel data type
     * @param data
     * @param length data length
     * @param pts
     */
    public abstract void responseReadFrame(int channel, byte[] data, int length, long pts);

}
