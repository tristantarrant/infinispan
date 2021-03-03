package org.infinispan.server.daemon;

import org.infinispan.commons.configuration.attributes.AttributeSet;
import org.infinispan.server.core.configuration.IpFilterConfiguration;
import org.infinispan.server.core.configuration.ProtocolServerConfiguration;
import org.infinispan.server.core.configuration.SslConfiguration;

/**
 * @author Tristan Tarrant &lt;tristan@infinispan.org&gt;
 * @since 12.1
 **/
public class DaemonServerConfiguration extends ProtocolServerConfiguration {
   protected DaemonServerConfiguration(AttributeSet attributes, SslConfiguration ssl, IpFilterConfiguration ipFilter) {
      super(attributes, ssl, ipFilter);
   }
}
