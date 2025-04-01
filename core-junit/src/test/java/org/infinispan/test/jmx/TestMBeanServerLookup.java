package org.infinispan.test.jmx;

import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

import org.infinispan.commons.jmx.MBeanServerLookup;

public final class TestMBeanServerLookup implements MBeanServerLookup {

   Properties props;

   private final MBeanServer mBeanServer = MBeanServerFactory.newMBeanServer();

   @Override
   public MBeanServer getMBeanServer(Properties props) {
      this.props = props;
      return mBeanServer;
   }
}
