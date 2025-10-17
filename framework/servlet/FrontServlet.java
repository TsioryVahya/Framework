package framework.servlet;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class FrontServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
       
        String requestURI = req.getRequestURI();
        String contextPath = req.getContextPath();
        String resourcePath = requestURI.substring(contextPath.length());
        
        // Vérifier si la ressource existe
        String realPath = req.getServletContext().getRealPath(resourcePath);
        File resourceFile = (realPath != null) ? new File(realPath) : null;
        
        if (resourceFile != null && resourceFile.exists() && resourceFile.isFile()) {
            // Afficher la ressource
            String mimeType = req.getServletContext().getMimeType(resourcePath);
            if (mimeType == null) mimeType = "application/octet-stream";
            resp.setContentType(mimeType);
            Files.copy(resourceFile.toPath(), resp.getOutputStream());
        } else {
            // Afficher l'URL
            resp.setContentType("text/html");
            resp.getWriter().write("<h1>Vous essayez d'acceder a : " + requestURI + "</h1>");
        }
    }
}
