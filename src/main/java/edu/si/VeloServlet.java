/**
 * Copyright 2015 Smithsonian Institution.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.You may obtain a copy of
 * the License at: http://www.apache.org/licenses/
 *
 * This software and accompanying documentation is supplied without
 * warranty of any kind. The copyright holder and the Smithsonian Institution:
 * (1) expressly disclaim any warranties, express or implied, including but not
 * limited to any implied warranties of merchantability, fitness for a
 * particular purpose, title or non-infringement; (2) do not assume any legal
 * liability or responsibility for the accuracy, completeness, or usefulness of
 * the software; (3) do not represent that use of the software would not
 * infringe privately owned rights; (4) do not warrant that the software
 * is error-free or will be maintained, supported, updated or enhanced;
 * (5) will not be liable for any indirect, incidental, consequential special
 * or punitive damages of any kind or nature, including but not limited to lost
 * profits or loss of data, on any basis arising from contract, tort or
 * otherwise, even if any of the parties has been warned of the possibility of
 * such loss or damage.
 *
 *
 * This distribution includes several third-party libraries, each with their own
 * license terms. For a complete copy of all copyright and license terms, including
 * those of third-party libraries, please see the product release notes.
 *
 *@author Charles Stern 1Charlesstern@gmail.com
 *@version 1.0
 *@since 2015-8-21
 * */
package edu.si;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.view.VelocityViewServlet;

import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class VeloServlet extends VelocityViewServlet {
    //uFile is the manifest submitted by the user.
    File uFile;
    //handles output to html page
    VelocityEngine ve = new VelocityEngine();
    VelocityContext context = new VelocityContext();
    //Chooses schema + schematron to use
    boolean isWCS=true;
    String ID;
    final String nl = System.getProperty("line.separator");

    @Override
    /**
     * Initialize servlet
     *
     * <p>
     *    This method is initially used by the servlet container to start the servlet.  It is called when the servlet container deems appropriate to use this servlet.
     * </p>
     */
    public void init() throws ServletException {
        ve.init();
        ID=makeId();
        //creates directories for submitted files & logs relative to the place where the servlet container was initialized
        //e.g. foo/bar/webapps/usrFiles
        new File("webapps/usrFiles").mkdirs();
        new File("webapps/logFiles").mkdir();
        uFile = new File("webapps/usrFiles/"+getId()+".xml");
        context.put("id", getId());
        context.put("servlet", this);
        try {
            uFile.createNewFile();
        }catch(IOException e){
            System.out.println("Failed to process user file.");
            e.printStackTrace();
        }
    }
    @Override
    /**
     * Returns form that allows user to upload a file
     *
     * @param request request from web browser via HTTP
     * @param response response to web browser via HTTP
     * @Exception ServletException
     * @Exception IOException
     */
    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
            throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        ve.evaluate(context, out, "xmlVerifier.vm", new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("/resources/xmlVerifier.vm")));
        response.setContentType("text/html");
        out.close();
    }
    @Override
    /**
     * Accepts user file from html form, and returns result after processing
     *
     * @param request request from web browser via HTTP
     * @param response response to web browser via HTTP
     * @Exception ServletException
     * @Exception IOException
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
      PrintWriter out = response.getWriter();
          response.setContentType("text/html");
        ve.evaluate(context, out, "xmlVerifier.vm", new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream("/resources/Verifiercomplete.vm")));
        uFile.setWritable(true);
        //Convert user file from input data to new file on server disk.
        BufferedReader xmlFile = new BufferedReader(request.getReader(),16384);
        FileWriter uFileFiller = new FileWriter(uFile);
        String line;String fileName="";
        boolean isXml=false;
        //Removes HTML header & multipart form boundaries
        while((line=xmlFile.readLine())!=null) {
                if (line.contains("filename=")) {
                    fileName = line.substring(line.indexOf("\"", line.indexOf("filename=")) + 1, line.indexOf("\"", line.indexOf("filename=")+10));
                    if (fileName.equals(""))
                        fileName = "null file";
                }
            else {
                    if (line.contains("<?xml version="))
                        isXml = true;
                    if (line.contains("----------"))
                        isXml = false;
                    if (isXml)
                        uFileFiller.write(line + nl);
                    if (line.contains("value=\"eMammal\""))
                        isWCS = false;
                }
        }
        uFileFiller.flush();
        uFileFiller.close();
        xmlFile.close();
        out.println("<p>"+nl+"Results of " + fileName + ": ");
        try {
            execute(out, uFile);
        }catch (IOException e) {
            out.append("Error. Log file could not be created.");
            e.printStackTrace();
        }
        out.println("</p>" + nl + "</body>" + nl + "</html>");
        //Prepares the servlet for a second transaction, if required.
        makeId();
        //Currently no point in keeping user files. remove the following line if the need arises.
        uFile.delete();
        out.close();
        }

    /**
     * When the servlet is taken out of service by the servlet container
     * @see javax.servlet.Servlet
     */
    public void destroy() {
    }

    /**
     * Passes the file, log, and html output to the WebVerifier
     * @param w Outputs to web page
     * @param xml File submitted by the user via HTTP
     * @throws FileNotFoundException
     * @see edu.si.WebVerifier
     */
    public void execute(PrintWriter w, File xml) throws FileNotFoundException{
            PrintWriter log = new PrintWriter(new File("webapps/logFiles/xmlVerifierLog"+getId()+".csv"));
            //log headers
            log.println("Pass(Y/N),Error,ErrorLocation,Row,Column");
            new WebVerifier(xml.getAbsolutePath(), log, w, isWCS);
        log.close();
    }

    /**
     * Creates a unique ID under which the user's log and submitted file are kept. Logs should be removed periodically.
     * @return unique 5-character alphabetic ID
     */
    public String makeId(){

        String text = "";
        String possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        for( int i=0; i < 5; i++ )
            text += possible.charAt((int)Math.floor(Math.random() * possible.length()));
        return text;
    }
    /**
     *Gets previously made ID
     *@return unique 5-character alphabetic ID
     */
    public String getId(){
        if(ID.equals(""))
            makeId();
        return ID;
    }
    /**
     *Set unique ID to certain String
     */
    public void setId(String str){ID=str;}

}