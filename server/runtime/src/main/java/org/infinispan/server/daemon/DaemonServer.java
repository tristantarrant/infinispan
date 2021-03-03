package org.infinispan.server.daemon;

import org.infinispan.rest.RestChannelInitializer;
import org.infinispan.rest.RestRequestHandler;
import org.infinispan.rest.configuration.RestServerConfiguration;
import org.infinispan.server.core.AbstractProtocolServer;
import org.infinispan.server.core.transport.NettyChannelInitializer;
import org.infinispan.server.core.transport.NettyInitializers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.group.ChannelMatcher;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 12.1
 **/
public class DaemonServer extends AbstractProtocolServer<DaemonServerConfiguration> {
   protected DaemonServer() {
      super("Daemon");
   }

   @Override
   public ChannelOutboundHandler getEncoder() {
      return null;
   }

   @Override
   public ChannelInboundHandler getDecoder() {
      return null;
   }

   @Override
   public ChannelInitializer<Channel> getInitializer() {
      return new NettyInitializers(new NettyChannelInitializer<DaemonServerConfiguration>(this, transport, null, null) {

      });
   }

   @Override
   public ChannelMatcher getChannelMatcher() {
      return channel -> channel.pipeline().get(RestRequestHandler.class) != null;
   }
}
