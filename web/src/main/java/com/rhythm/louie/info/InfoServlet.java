/* 
 * Copyright 2015 Rhythm & Hues Studios.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.rhythm.louie.info;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

import com.google.common.io.Resources;

import org.slf4j.LoggerFactory;

import com.rhythm.louie.server.LouieProperties;
import com.rhythm.louie.server.ServiceManager;
import com.rhythm.louie.server.ServiceProperties;

/**
 *
 * @author cjohnson
 */
@WebServlet(urlPatterns = {"","/info"})
public class InfoServlet extends HttpServlet {
    private static final String LOUIE_EXTRA_INFO = "/louie-info.html";
    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Map<String, Object> properties = new HashMap<>();
        
        String config = request.getParameter("config");
        if (config != null) {
            if ("services".equals(config)) {
                properties.put("serviceconfig",ServiceProperties.getAllServiceProperties());
            } else {
                properties.put("config", "<xmp>"+LouieProperties.getDocument()+"</xmp>");
            }
        }

        List<String> extras = Collections.emptyList();
        try {
            extras = Resources.readLines(request.getServletContext().getClassLoader().getResource(LOUIE_EXTRA_INFO),Charset.defaultCharset());
        } catch (Exception ex) {
            LoggerFactory.getLogger(InfoServlet.class)
                .error("Failed to get URL for louie-info.html file: {}", ex.toString());
        }

        properties.put("extras", extras);
        properties.put("fails", ServiceManager.getFailedServiceProviders());
        properties.put("errors", ServiceManager.getErrors());
        
        InfoUtils.writeTemplateResponse(request, response, "info.vm", properties);
    }
    
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Louie Info Home";
    }// </editor-fold>
}
