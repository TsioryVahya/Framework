package framework.servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;

import framework.annotation.AnnotationReader;
import framework.annotation.Param;
import framework.utilitaire.MappingInfo;
import framework.utilitaire.ModelAndView;

public class FrontServlet extends HttpServlet {

    @Override
    public void init() throws ServletException {
        super.init();
        // Initialize annotation-based URL mappings once at startup
        AnnotationReader.init();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        String originalURI = (String) req.getAttribute("originalURI");
        String requestURI = originalURI != null ? originalURI : req.getRequestURI();
        String contextPath = req.getContextPath();
        String urlPath = requestURI.startsWith(contextPath) 
                ? requestURI.substring(contextPath.length()) 
                : requestURI;
        if (urlPath.isEmpty()) {
            urlPath = "/";
        }

        System.out.println("FrontServlet handling: " + urlPath);

        // Find mapping for the URL
        MappingInfo mapping = AnnotationReader.findMappingByUrl(urlPath);

        resp.setContentType("text/html;charset=UTF-8");

        if (mapping == null || !mapping.isFound()) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("<h1>404 - URL non trouvée: " + urlPath + "</h1>");
            return;
        }

        try {
            // Expose path variables (if any) as request attributes
            Map<String, String> vars = mapping.getLastPathVariables();
            if (vars != null) {
                for (Map.Entry<String, String> e : vars.entrySet()) {
                    req.setAttribute(e.getKey(), e.getValue());
                }
            }

            Class<?> controllerClass = mapping.getControllerClass();
            Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
            Method method = mapping.getMethod();

            // Build method arguments: support HttpServletRequest/HttpServletResponse
            Parameter[] params = method.getParameters();
            Object[] args = new Object[params.length];
            for (int i = 0; i < params.length; i++) {
                Class<?> pt = params[i].getType();
                if (HttpServletRequest.class.isAssignableFrom(pt)) {
                    args[i] = req;
                } else if (HttpServletResponse.class.isAssignableFrom(pt)) {
                    args[i] = resp;
                } else {
                    // Try to resolve using @Param annotation first
                    Param pAnn = params[i].getAnnotation(Param.class);
                    String raw = null;
                    if (pAnn != null) {
                        String paramName = pAnn.value();
                        raw = req.getParameter(paramName);
                        if (raw == null) {
                            Object attr = req.getAttribute(paramName);
                            if (attr != null) {
                                if (pt.isInstance(attr)) {
                                    args[i] = attr;
                                    continue;
                                }
                                raw = String.valueOf(attr);
                            }
                        }
                    } else {
                        // Fallback: try parameter name (requires -parameters at compile time) or attribute
                        String guessName = params[i].getName();
                        raw = req.getParameter(guessName);
                        if (raw == null) {
                            Object attr = req.getAttribute(guessName);
                            if (attr != null) {
                                if (pt.isInstance(attr)) {
                                    args[i] = attr;
                                    continue;
                                }
                                raw = String.valueOf(attr);
                            }
                        }
                    }

                    if (raw != null) {
                        args[i] = convertValue(raw, pt);
                    } else {
                        args[i] = pt.isPrimitive() ? defaultForPrimitive(pt) : null;
                    }
                }
            }

            Object result = method.invoke(controllerInstance, args);

            if (result instanceof String) {
                resp.getWriter().write((String) result);
            } else if (result instanceof ModelAndView) {
                ModelAndView mv = (ModelAndView) result;
                // Set model attributes
                for (Map.Entry<String, Object> entry : mv.getModel().entrySet()) {
                    req.setAttribute(entry.getKey(), entry.getValue());
                }
                // Forward to JSP view
                RequestDispatcher dispatcher = req.getRequestDispatcher(mv.getView());
                dispatcher.forward(req, resp);
            } else if (result == null) {
                resp.getWriter().write("");
            } else {
                resp.getWriter().write(String.valueOf(result));
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("<h1>500 - Erreur serveur</h1><pre>" + e.getMessage() + "</pre>");
        }
    }

    private Object convertValue(String raw, Class<?> targetType) {
        if (raw == null) return null;
        if (String.class.equals(targetType)) return raw;
        try {
            if (targetType == int.class || targetType == Integer.class) return Integer.parseInt(raw);
            if (targetType == long.class || targetType == Long.class) return Long.parseLong(raw);
            if (targetType == double.class || targetType == Double.class) return Double.parseDouble(raw);
            if (targetType == float.class || targetType == Float.class) return Float.parseFloat(raw);
            if (targetType == boolean.class || targetType == Boolean.class) return Boolean.parseBoolean(raw);
            if (targetType == short.class || targetType == Short.class) return Short.parseShort(raw);
            if (targetType == byte.class || targetType == Byte.class) return Byte.parseByte(raw);
            if (targetType == char.class || targetType == Character.class) return raw.isEmpty() ? '\0' : raw.charAt(0);
            if (targetType.isEnum()) {
                @SuppressWarnings({"rawtypes", "unchecked"})
                Class<? extends Enum> et = (Class<? extends Enum>) targetType;
                return Enum.valueOf(et, raw);
            }
        } catch (Exception ignore) {
            // fall through to return null/default
        }
        return null;
    }

    private Object defaultForPrimitive(Class<?> pt) {
        if (pt == boolean.class) return false;
        if (pt == char.class) return '\0';
        if (pt == byte.class) return (byte) 0;
        if (pt == short.class) return (short) 0;
        if (pt == int.class) return 0;
        if (pt == long.class) return 0L;
        if (pt == float.class) return 0f;
        if (pt == double.class) return 0d;
        return null;
    }
}

