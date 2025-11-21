package framework.utilitaire;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class MappingInfo {
    private Class<?> controllerClass;
    private Method method;
    private String url;
    private boolean found;
    // Pattern support
    private boolean isPattern;
    private Pattern regex;
    private List<String> variableNames;
    private Map<String, String> lastPathVariables;
    
    public MappingInfo(Class<?> controllerClass, Method method, String url) {
        this.controllerClass = controllerClass;
        this.method = method;
        this.url = url;
        this.found = true;
        this.variableNames = new ArrayList<>();
        this.lastPathVariables = new HashMap<>();
        compilePatternIfNeeded();
    }
    
    public MappingInfo() {
        this.found = false;
    }
    
    public Class<?> getControllerClass() {
        return controllerClass;
    }
    
    public Method getMethod() {
        return method;
    }
    
    public String getUrl() {
        return url;
    }
    
    public boolean isFound() {
        return found;
    }
    
    public String getClassName() {
        return found ? controllerClass.getSimpleName() : null;
    }
    
    public String getMethodName() {
        return found ? method.getName() : null;
    }

    private void compilePatternIfNeeded() {
        if (url != null && url.contains("{")) {
            isPattern = true;
            // Convert "/book/{id}" to regex like ^/book/([^/]+)$ and collect ["id"]
            StringBuilder regexBuilder = new StringBuilder("^");
            StringBuilder name = new StringBuilder();
            boolean inVar = false;
            for (int i = 0; i < url.length(); i++) {
                char c = url.charAt(i);
                if (c == '{') {
                    inVar = true;
                    name.setLength(0);
                } else if (c == '}' && inVar) {
                    inVar = false;
                    variableNames.add(name.toString());
                    regexBuilder.append("([^/]+)");
                } else {
                    if (inVar) {
                        name.append(c);
                    } else {
                        // escape regex special chars minimally
                        if (".[]()\\+^$|".indexOf(c) >= 0) {
                            regexBuilder.append('\\');
                        }
                        regexBuilder.append(c);
                    }
                }
            }
            regexBuilder.append("$");
            regex = Pattern.compile(regexBuilder.toString());
        } else {
            isPattern = false;
        }
    }

    public boolean matches(String path) {
        if (!isPattern) {
            return url != null && url.equals(path);
        }
        Matcher m = regex.matcher(path);
        if (!m.matches()) return false;
        lastPathVariables.clear();
        for (int i = 0; i < variableNames.size(); i++) {
            lastPathVariables.put(variableNames.get(i), m.group(i + 1));
        }
        return true;
    }

    public Map<String, String> getLastPathVariables() {
        return lastPathVariables;
    }
}
