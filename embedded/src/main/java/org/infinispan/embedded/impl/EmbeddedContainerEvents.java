package org.infinispan.embedded.impl;

import java.util.List;
import java.util.Objects;

import org.infinispan.api.common.events.container.Address;

/**
 * @since 16.3
 */
final class EmbeddedContainerEvents {

   private EmbeddedContainerEvents() {
   }

   static Address wrapAddress(org.infinispan.remoting.transport.Address address) {
      return new EmbeddedAddress(address);
   }

   static List<Address> wrapAddresses(List<org.infinispan.remoting.transport.Address> addresses) {
      return addresses.stream()
            .map(EmbeddedContainerEvents::wrapAddress)
            .toList();
   }

   private record EmbeddedAddress(org.infinispan.remoting.transport.Address delegate) implements Address {
      @Override
      public String toString() {
         return delegate.toString();
      }

      @Override
      public boolean equals(Object o) {
         return o instanceof EmbeddedAddress other && Objects.equals(delegate, other.delegate);
      }

      @Override
      public int hashCode() {
         return Objects.hashCode(delegate);
      }
   }
}
