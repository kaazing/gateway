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
package org.kaazing.mina.netty.bootstrap;

class ConnectionlessClientBootstrap extends ConnectionlessBootstrap implements ClientBootstrap {

//    private volatile ChannelPipeline pipeline = pipeline();
//    private volatile ChannelPipelineFactory pipelineFactory = pipelineFactory(pipeline);

    ConnectionlessClientBootstrap() {
//        super.setPipeline(pipeline(new ConnectionlessChannelHandler()));
    }

//    @Override
//    public ChannelPipeline getPipeline() {
//        ChannelPipeline pipeline = this.pipeline;
//        if (pipeline == null) {
//            throw new IllegalStateException(
//                    "getPipeline() cannot be called " +
//                    "if setPipelineFactory() was called.");
//        }
//        return pipeline;
//    }
//
//    @Override
//    public void setPipeline(ChannelPipeline pipeline) {
//        if (pipeline == null) {
//            throw new NullPointerException("pipeline");
//        }
//        this.pipeline = pipeline;
//        pipelineFactory = pipelineFactory(pipeline);
//    }
//
//    @Override
//    public Map<String, ChannelHandler> getPipelineAsMap() {
//        ChannelPipeline pipeline = this.pipeline;
//        if (pipeline == null) {
//            throw new IllegalStateException("pipelineFactory in use");
//        }
//        return pipeline.toMap();
//    }
//
//    @Override
//    public void setPipelineAsMap(Map<String, ChannelHandler> pipelineMap) {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public ChannelPipelineFactory getPipelineFactory() {
//        return pipelineFactory;
//    }
//
//    @Override
//    public void setPipelineFactory(ChannelPipelineFactory pipelineFactory) {
//        if (pipelineFactory == null) {
//            throw new NullPointerException("pipelineFactory");
//        }
//        pipeline = null;
//        this.pipelineFactory = pipelineFactory;
//    }
//
//    private static final class ConnectionlessChannelHandler extends SimpleChannelUpstreamHandler {
//    }
}
