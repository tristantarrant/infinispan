package org.infinispan.query.objectfilter.impl.syntax.update;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.infinispan.factories.scopes.Scope;
import org.infinispan.factories.scopes.Scopes;
import org.infinispan.query.functions.QueryFunction;
import org.infinispan.query.functions.builtin.ConversionFunctions;
import org.infinispan.query.functions.builtin.DateFunctions;
import org.infinispan.query.functions.builtin.MathFunctions;
import org.infinispan.query.functions.builtin.RegexpFunctions;
import org.infinispan.query.functions.builtin.StringFunctions;

import com.google.errorprone.annotations.ThreadSafe;

/**
 * Registry of the named functions usable in statement value expressions.
 * <p>
 * Function names are case-insensitive. This is a global, mutable component:
 * user-defined functions can be added at runtime via {@link #add(String, QueryFunction)}
 * without re-registration. The server can augment the singleton with custom
 * functions that will be available to all subsequent queries.
 *
 * @since 16.4
 */
@ThreadSafe
@Scope(Scopes.GLOBAL)
public final class QueryFunctionRegistry {

    private static final QueryFunctionRegistry STANDARD = createStandard();

    private final ConcurrentHashMap<String, QueryFunction> functions;

    private QueryFunctionRegistry() {
       this.functions = new ConcurrentHashMap<>();
    }

    QueryFunctionRegistry(ConcurrentHashMap<String, QueryFunction> functions) {
       this.functions = functions;
    }

    /**
     * Returns the registry of the built-in functions.
     */
    public static QueryFunctionRegistry standard() {
       return STANDARD;
    }

    /**
     * Creates a new registry pre-populated with the built-in functions.
     * Intended for testing or for creating isolated registries.
     */
    static QueryFunctionRegistry copyOfStandard() {
       QueryFunctionRegistry reg = new QueryFunctionRegistry();
       reg.functions.putAll(STANDARD.functions);
       return reg;
    }

    /**
     * Adds a user-defined function to this registry.
     *
     * @param name     the function name (case-insensitive)
     * @param function the function implementation
     * @throws IllegalArgumentException if a function with the given name already exists
     */
    public void add(String name, QueryFunction function) {
       String key = normalize(name);
       if (functions.putIfAbsent(key, function) != null) {
          throw new IllegalArgumentException("Function already registered: " + name);
       }
    }

    /**
     * Removes a function from this registry.
     *
     * @param name the function name (case-insensitive)
     * @return the previous function, or {@code null} if not present
     */
    public QueryFunction remove(String name) {
       return functions.remove(normalize(name));
    }

   public QueryFunction get(String name) {
      return functions.get(normalize(name));
   }

   public boolean contains(String name) {
      return functions.containsKey(normalize(name));
   }

   public Set<String> names() {
      return functions.keySet();
   }

   private static String normalize(String name) {
      return name.toLowerCase(Locale.ROOT);
   }

    private static QueryFunctionRegistry createStandard() {
        ConcurrentHashMap<String, QueryFunction> f = new ConcurrentHashMap<>();

      // math
      f.put("abs", new MathFunctions.Abs());
      f.put("round", new MathFunctions.Round());
      f.put("floor", new MathFunctions.Floor());
      f.put("ceil", new MathFunctions.Ceil());
      f.put("min", new MathFunctions.Min());
      f.put("max", new MathFunctions.Max());
      f.put("power", new MathFunctions.Power());
      f.put("sqrt", new MathFunctions.Sqrt());

      // string
      f.put("concat", new StringFunctions.Concat());
      f.put("upper", new StringFunctions.Upper());
      f.put("lower", new StringFunctions.Lower());
      f.put("trim", new StringFunctions.Trim());
      f.put("length", new StringFunctions.Length());
      f.put("substring", new StringFunctions.Substring());
      f.put("replace", new StringFunctions.Replace());

      // regexp
      f.put("regexp_like", new RegexpFunctions.RegexpLike());
      f.put("regexp_replace", new RegexpFunctions.RegexpReplace());
      f.put("regexp_substr", new RegexpFunctions.RegexpSubstr());
      f.put("regexp_instr", new RegexpFunctions.RegexpInstr());

      // date
      f.put("adddays", new DateFunctions.AddDays());
      f.put("addhours", new DateFunctions.AddHours());
      f.put("addminutes", new DateFunctions.AddMinutes());
      f.put("addmonths", new DateFunctions.AddMonths());
      f.put("addyears", new DateFunctions.AddYears());
      f.put("dateadd", new DateFunctions.DateAdd());

      // conversion
      f.put("tolong", new ConversionFunctions.ToLong());
      f.put("todouble", new ConversionFunctions.ToDouble());
      f.put("tostring", new ConversionFunctions.ToString());

        return new QueryFunctionRegistry(f);
    }
}
