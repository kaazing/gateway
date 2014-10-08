/**
 * Copyright (c) 2007-2014, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.core.session;

import javax.security.auth.Subject;

public interface SubjectChangeListener {

    void subjectChanged(Subject newSubject);

}
