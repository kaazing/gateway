/**
 * Copyright (c) 2007-2012, Kaazing Corporation. All rights reserved.
 */

package com.kaazing.mina.netty.bootstrap;

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
