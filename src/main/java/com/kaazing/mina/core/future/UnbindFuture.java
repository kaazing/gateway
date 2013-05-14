/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.future;

import org.apache.mina.core.future.IoFuture;

public interface UnbindFuture extends IoFuture {

    /**
     * Reports immediately whether the unbind operation has successfully completed
     * @return true if the unbind operation has successfully completed, else false
     */
    boolean isUnbound();

    /**
     * Signal that the unbind operation has successfully completed (fulfills the future)
     */
    void setUnbound();

    /**
     * Immediately gets any exception thrown by the unbind processing (if completed)
     * @return the exception, or null if the unbind operation has not yet completed or completed successfully
     */
    Throwable getException();

    /**
     * Signal that the unbind operation has failed (fulfills the future)
     * @param exception
     */
    void setException(Throwable exception);
}
