/**
 * Copyright (c) 2007-2013, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.future;

import org.apache.mina.core.future.IoFuture;

public interface BindFuture extends IoFuture {

    /**
     * Reports immediately whether the bind operation has successfully completed
     * @return true if the bind operation has successfully completed, else false
     */
    boolean isBound();

    /**
     * Signal that the bind operation has successfully completed (fulfills the future)
     */
    void setBound();

    /**
     * Immediately gets any exception thrown by the bind processing (if completed)
     * @return the exception, or null if the bind operation has not yet completed or completed successfully
     */
    Throwable getException();

    /**
     * Signal that the bind operation has failed (fulfills the future)
     * @param exception
     */
    void setException(Throwable exception);
}
