/*
 *  Copyright 2015 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.aem.community.core.servlets;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.engine.SlingRequestProcessor;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.w3c.dom.Document;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.aem.community.core.models.MediaReplacedElementFactory;
import com.day.cq.contentsync.handler.util.RequestResponseFactory;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMMode;
import com.lowagie.text.DocumentException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Servlet that writes some sample content into the response. It is mounted for
 * all resources of a specific Sling resource type. The
 * {@link SlingSafeMethodsServlet} shall be used for HTTP methods that are
 * idempotent. For write operations use the {@link SlingAllMethodsServlet}.
 */
@SuppressWarnings("serial")
@SlingServlet(paths = "/bin/pdfgenerator")
public class SimpleServlet extends SlingSafeMethodsServlet {
	
	 /** Service to create HTTP Servlet requests and responses */
    @Reference
    private RequestResponseFactory requestResponseFactory;

    /** Service to process requests through Sling */
    @Reference
    private SlingRequestProcessor requestProcessor;


    @Override
    protected void doGet(final SlingHttpServletRequest req,
            final SlingHttpServletResponse resp) throws ServletException, IOException {
       
        ResourceResolver resourceResolver= req.getResourceResolver();
        PageManager pageManager=resourceResolver.adaptTo(PageManager.class);
        String pdfPath="/content/pdf-generator/en.html";
    	HttpServletRequest request = requestResponseFactory.createRequest("GET", pdfPath);
		WCMMode.DISABLED.toRequest(req);
		ByteArrayOutputStream os= new ByteArrayOutputStream();
		HttpServletResponse response = requestResponseFactory.createResponse(os);
		requestProcessor.processRequest(request, response, req.getResourceResolver());
		String htmlString= os.toString();	
		StringBuilder cssString = new StringBuilder();
		String cssArray[] = { "/etc/designs/external/tether.css","/etc/designs/pdf-generator/clientlib-all.css", "/etc/designs/pdf-generator/pdf-styles.css"};
		// extract the CSS file content		 
		for (String cssFile : cssArray) {		 
			Element style = new Element(Tag.valueOf("style"), "");		 
			style.attr("type", "text/css");
			//use logic as above in a method getStringFromPath
			cssString = cssString.append(getStringFromPath(cssFile, request, requestResponseFactory, requestProcessor,req));
		}

		 OutputStream osFinal =resp.getOutputStream();
		 ITextRenderer renderer = new ITextRenderer();
		 org.jsoup.nodes.Document document = Jsoup.parse(htmlString);
		 Document doc = null;
		 W3CDom w3cDom = new W3CDom();
		 doc = w3cDom.fromJsoup(document);
		 renderer.getSharedContext().setReplacedElementFactory(new MediaReplacedElementFactory(renderer.getSharedContext().getReplacedElementFactory(), req.getResourceResolver(),null));
		 renderer.setDocument(doc, null);
		 renderer.layout();
		 try {
			renderer.createPDF(osFinal, false);
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 // complete the PDF
		 renderer.finishPDF();
		 // saving the PDF
		 response.setHeader("Expires", "0");
		 response.setHeader("Cache-Control", "must-revalidate, post-check=0, pre-check=0");
		 response.setHeader("Pragma", "public");
		 // setting the content type
		 response.setContentType("application/pdf");
		 response.setHeader("Content-disposition", "attachment; filename=Sample.pdf");
		 os.flush();
		 os.close();    	
       
    }
    
	private Object getStringFromPath(String cssFile, HttpServletRequest request,
			RequestResponseFactory requestResponseFactory2, SlingRequestProcessor requestProcessor2,SlingHttpServletRequest req) {
		String requestPath = "content/we-retail/us/en.html";
       	request = requestResponseFactory2.createRequest("GET", requestPath);
		WCMMode.DISABLED.toRequest(request);
		ByteArrayOutputStream os= new ByteArrayOutputStream();
		HttpServletResponse response = requestResponseFactory.createResponse(os);
		try {
			requestProcessor.processRequest(request, response, req.getResourceResolver());
		} catch (ServletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String cssString= os.toString();			
		return cssString;
	}
}
