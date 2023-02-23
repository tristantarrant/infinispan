package org.infinispan.client.rest.impl.okhttp;

import static org.infinispan.client.rest.impl.okhttp.RestClientOkHttp.EMPTY_BODY;

import java.util.concurrent.CompletionStage;

import org.infinispan.client.rest.CacheSelectionRule;
import org.infinispan.client.rest.RestContainerClient;
import org.infinispan.client.rest.RestResponse;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.commons.dataconversion.internal.Json;

import okhttp3.Request;

/**
 * @author Ryan Emerson
 * @since 13.0
 */
public class RestContainerClientOkHttp implements RestContainerClient {
   private final RestClientOkHttp client;
   private final String baseClusterURL;

   public RestContainerClientOkHttp(RestClientOkHttp client) {
      this.client = client;
      this.baseClusterURL = String.format("%s%s/v2/container", client.getBaseURL(), client.getConfiguration().contextPath()).replaceAll("//", "/");
   }

   @Override
   public CompletionStage<RestResponse> shutdown() {
      return client.execute(
            new Request.Builder()
                  .post(EMPTY_BODY)
                  .url(baseClusterURL + "?action=shutdown")
      );
   }

   @Override
   public CompletionStage<RestResponse> listCacheSelectionRules() {
      return client.execute(new Request.Builder().get().url(baseClusterURL + "/cache-selectors"));
   }

   @Override
   public CompletionStage<RestResponse> deleteAllCacheSelectionRules() {
      return client.execute(new Request.Builder().delete().url(baseClusterURL + "/cache-selectors"));
   }

   @Override
   public CompletionStage<RestResponse> addCacheSelectionRule(CacheSelectionRule rule) {
      Json json = Json.object("by", rule.getDiscriminator(), "condition", rule.getCondition(), "expression", rule.getExpression(), "target", rule.getTarget());
      Request.Builder builder = new Request.Builder().url(baseClusterURL + "/cache-selectors").post(new StringRestEntityOkHttp(MediaType.APPLICATION_JSON, json.toString()).toRequestBody());
      return client.execute(builder);
   }

   @Override
   public CompletionStage<RestResponse> deleteCacheSelectionRule(int id) {
      return client.execute(new Request.Builder().delete().url(baseClusterURL + "/cache-selectors/" + id));
   }
}
