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
package com.aem.community.core.models;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.ValueFormatException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.w3c.dom.Element;
import org.xhtmlrenderer.extend.FSImage;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.pdf.ITextFSImage;
import org.xhtmlrenderer.pdf.ITextImageElement;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.simple.extend.FormSubmissionListener;

import org.apache.commons.io.IOUtils;

import com.lowagie.text.BadElementException;
import com.lowagie.text.Image;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;;

/**
 * Replaced element in order to replace elements like
 * <tt><div class="media" data-src="image.png" /></tt> with the real media
 * content.
 */
public class MediaReplacedElementFactory implements ReplacedElementFactory {
	
private final ReplacedElementFactory superFactory;
private ResourceResolver resourceResolver;
private ITextOutputDevice _outputDevice;

public MediaReplacedElementFactory(ReplacedElementFactory superFactory, ResourceResolver resourceResolver, ITextOutputDevice outputDevice) {
	this.resourceResolver = resourceResolver;
	this.superFactory = superFactory;
	this._outputDevice = outputDevice;
}

@Override
public ReplacedElement createReplacedElement(LayoutContext layoutContext, BlockBox blockBox, UserAgentCallback userAgentCallback, int cssWidth, int cssHeight) {
	Element element = blockBox.getElement();
	if (element == null) {
	  return null;
	}
		
	String tagName = element.getTagName();

	// Replace any img tag with the binary data of `image.png` into the PDF.
	if ("img".equals(tagName)) {
		InputStream input = null;
		String imageSrc = element.getAttribute("src");
		if (imageSrc != null && imageSrc.startsWith("/")) {
		    imageSrc = imageSrc.replace("_jcr_content", "jcr:content");
	            Resource imageRes = this.resourceResolver.resolve(imageSrc);
			
		    if(imageRes != null && imageRes.getChild("jcr:content/renditions/original/jcr:content") != null){
			Node node = imageRes.getChild("jcr:content/renditions/original/jcr:content").adaptTo(Node.class);
			try {
				input = node.getProperty("jcr:data").getBinary().getStream();
			} catch (ValueFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (PathNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (RepositoryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			}
		} 

		if (input != null) {
		
			byte[] bytes=null;
			try {
				bytes = IOUtils.toByteArray(input);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Image image=null;;
			try {
				image = Image.getInstance(bytes);
			} catch (BadElementException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			final FSImage fsImage = new ITextFSImage(image);
				if (fsImage != null) {
				    if ((cssWidth != -1) || (cssHeight != -1)) {
					  fsImage.scale(cssWidth, cssHeight);
					}
			return new ITextImageElement(fsImage);
					}
		    }
			
		}
		
		return this.superFactory.createReplacedElement(layoutContext, blockBox, userAgentCallback, cssWidth, cssHeight);
	   
	}

	@Override
	public void reset() {
		this.superFactory.reset();
	}

	@Override
	public void remove(Element e) {
		this.superFactory.remove(e);
	}

	@Override
	public void setFormSubmissionListener(FormSubmissionListener listener) {
		this.superFactory.setFormSubmissionListener(listener);
	}
}