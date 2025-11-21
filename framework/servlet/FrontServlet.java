package framework.servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.lang.reflect.Method;

import framework.annotation.AnnotationReader;
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
            Class<?> controllerClass = mapping.getControllerClass();
            Object controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
            Method method = mapping.getMethod();

            Object result = method.invoke(controllerInstance);

            if (result instanceof String) {
                resp.getWriter().write((String) result);
            } else if (result instanceof ModelAndView) {
                ModelAndView mv = (ModelAndView) result;
                // Set model attributes
                for (java.util.Map.Entry<String, Object> entry : mv.getModel().entrySet()) {
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
}
