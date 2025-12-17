package framework.utilitaire;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

public class JsonUtils {
    public static String toJson(Object obj) {
        if (obj == null) return "null";
        if (obj instanceof Number || obj instanceof Boolean) return String.valueOf(obj);
        if (obj instanceof String) return quote((String) obj);
        if (obj.getClass().isArray()) return arrayToJson(obj);
        if (obj instanceof Collection) return collectionToJson((Collection<?>) obj);
        if (obj instanceof Map) return mapToJson((Map<?, ?>) obj);
        return objectToJson(obj);
    }

    private static String quote(String s) {
        if (s == null) return "null";
        return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + '"';
    }

    private static String arrayToJson(Object array) {
        int len = Array.getLength(array);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < len; i++) {
            if (i > 0) sb.append(',');
            sb.append(toJson(Array.get(array, i)));
        }
        sb.append(']');
        return sb.toString();
    }

    private static String collectionToJson(Collection<?> coll) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Object o : coll) {
            if (!first) sb.append(',');
            first = false;
            sb.append(toJson(o));
        }
        sb.append(']');
        return sb.toString();
    }

    private static String mapToJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append(quote(String.valueOf(e.getKey()))).append(':').append(toJson(e.getValue()));
        }
        sb.append('}');
        return sb.toString();
    }

    private static String objectToJson(Object obj) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        Class<?> cls = obj.getClass();
        while (cls != null && cls != Object.class) {
            Field[] fields = cls.getDeclaredFields();
            for (Field f : fields) {
                f.setAccessible(true);
                try {
                    Object v = f.get(obj);
                    if (!first) sb.append(',');
                    first = false;
                    sb.append(quote(f.getName())).append(':').append(toJson(v));
                } catch (IllegalAccessException ignore) {}
            }
            cls = cls.getSuperclass();
        }
        sb.append('}');
        return sb.toString();
    }
}
