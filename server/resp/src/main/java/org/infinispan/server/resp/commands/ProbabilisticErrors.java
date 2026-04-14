package org.infinispan.server.resp.commands;

/**
 * Shared error message constants for probabilistic data structure commands
 * (Bloom filters, Cuckoo filters, Count-Min Sketch, Top-K).
 * These match the exact error strings returned by Redis module commands.
 */
public final class ProbabilisticErrors {
   public static final String ERR_NOT_FOUND = "ERR not found";
   public static final String ERR_CAPACITY = "ERR (capacity should be larger than 0)";
   public static final String ERR_UNKNOWN_ARGUMENT = "Unknown argument received";

   // CMS errors use a different prefix format than BF/CF
   public static final String CMS_KEY_NOT_FOUND = "CMS: key does not exist";
   public static final String CMS_KEY_EXISTS = "CMS: key already exists";
   public static final String CMS_CANNOT_PARSE = "CMS: Cannot parse number";
   public static final String CMS_NEGATIVE_NUMBER = "CMS: Number cannot be negative";
   public static final String CMS_INVALID_WIDTH = "CMS: invalid width";
   public static final String CMS_INVALID_DEPTH = "CMS: invalid depth";
   public static final String CMS_INVALID_OVERESTIMATION = "CMS: invalid overestimation value";
   public static final String CMS_INVALID_PROB = "CMS: invalid prob value";
   public static final String CMS_WIDTH_DEPTH_MISMATCH = "CMS: width/depth is not equal";
   public static final String CMS_INVALID_NUMKEYS = "CMS: invalid numkeys";
   public static final String CMS_NUMKEYS_POSITIVE = "CMS: Number of keys must be positive";
   public static final String CMS_WRONG_NUM_KEYS = "CMS: wrong number of keys";
   public static final String CMS_INVALID_WEIGHT = "CMS: invalid weight value";

   // TopK errors
   public static final String TOPK_KEY_NOT_FOUND = "TopK: key does not exist";
   public static final String TOPK_KEY_EXISTS = "TopK: key already exists";
   public static final String TOPK_INVALID_K = "TopK: invalid k";
   public static final String TOPK_INVALID_WIDTH = "TopK: invalid width";
   public static final String TOPK_INVALID_DEPTH = "TopK: invalid depth";
   public static final String TOPK_INVALID_DECAY = "TopK: invalid decay value. must be '<= 1' & '> 0'";
   public static final String TOPK_INVALID_INCREMENT = "TopK: increment must be an integer greater or equal to 0 and smaller or equal to 100,000";
   public static final String TOPK_WITHCOUNT_EXPECTED = "WITHCOUNT keyword expected";

   // T-Digest errors
   public static final String TDIGEST_KEY_NOT_FOUND = "T-Digest: key does not exist";
   public static final String TDIGEST_KEY_EXISTS = "T-Digest: key already exists";
   public static final String TDIGEST_ERROR_PARSING_COMPRESSION = "T-Digest: error parsing compression parameter";
   public static final String TDIGEST_COMPRESSION_POSITIVE = "T-Digest: compression parameter needs to be a positive integer";
   public static final String TDIGEST_ERROR_PARSING_VAL = "T-Digest: error parsing val parameter";
   public static final String TDIGEST_ERROR_PARSING_QUANTILE = "T-Digest: error parsing quantile";
   public static final String TDIGEST_ERROR_PARSING_CDF = "T-Digest: error parsing cdf";
   public static final String TDIGEST_ERROR_PARSING_VALUE = "T-Digest: error parsing value";
   public static final String TDIGEST_ERROR_PARSING_RANK = "T-Digest: error parsing rank";
   public static final String TDIGEST_ERROR_PARSING_LOW_CUT = "T-Digest: error parsing low_cut_percentile";
   public static final String TDIGEST_ERROR_PARSING_HIGH_CUT = "T-Digest: error parsing high_cut_percentile";
   public static final String TDIGEST_CUT_PERCENTILE_RANGE = "T-Digest: low_cut_percentile and high_cut_percentile should be in [0,1]";
   public static final String TDIGEST_LOW_CUT_LOWER = "T-Digest: low_cut_percentile should be lower than high_cut_percentile";
   public static final String TDIGEST_ERROR_PARSING_NUMKEYS = "T-Digest: error parsing numkeys";
   public static final String TDIGEST_NUMKEYS_POSITIVE = "T-Digest: numkeys needs to be a positive integer";

   private ProbabilisticErrors() {
   }
}
