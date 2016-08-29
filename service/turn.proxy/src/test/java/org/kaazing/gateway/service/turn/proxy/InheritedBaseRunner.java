/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaazing.gateway.service.turn.proxy;


import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;


/**
 * Created by APirvu on 8/29/2016.
 */
public class InheritedBaseRunner extends BlockJUnit4ClassRunner {


    @Override
    protected Object createTest() throws Exception {
        if (getTestClass().getJavaClass().isAnnotationPresent(IgnoreBaseClassTests.class)) {
            System.out.println("This is a base class it should not be run directly");
            return new TestClass(Object.class);
        }
        return super.createTest();
    }

    /**
     * @param klass
     * @throws InitializationError
     * @since
     */
    public InheritedBaseRunner(Class<?> klass) throws InitializationError {
        super(klass);
        try {
            this.filter(new InheritedTestsFilter());
        } catch (NoTestsRemainException e) {
            // throw new IllegalStateException("class should contain at least one runnable test", e);
        }

    }

}
