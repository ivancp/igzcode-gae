package com.igzcode.java.gae.filter;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.igzcode.java.gae.util.ConfigUtil;
import com.igzcode.java.gae.util.MailUtil;
import com.igzcode.java.util.Trace;

public class ResponseFilter implements Filter {

	protected static final Logger LOGGER = Logger.getLogger(ResponseFilter.class.getName());

	private String mailFrom = null;
	private String mailTo = null;
	private String appName = null;
	
	private Boolean disabled = false;
	
	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest p_req, ServletResponse p_resp, FilterChain p_filterChain) throws ServletException, IOException {
		if( !this.disabled ){
			
			HttpServletRequest request = (HttpServletRequest) p_req;
			ServletResponseWrapper response = new ServletResponseWrapper((HttpServletResponse)p_resp);
			
			String url = request.getRequestURL().toString();
			
			Exception problem = null;
		    try {
		    	p_filterChain.doFilter(p_req, response);
		    } catch (Exception e) {
		        problem = e;
		        Trace.error( e );
		    }
		    
		    LOGGER.info("ResponseFilter check response");
		    
		    if (problem != null) {
		        this.processError(problem, p_resp, url);
		        
		        if (problem instanceof ServletException) {
		            throw (ServletException) problem;
		        }
		        if (problem instanceof IOException) {
		            throw (IOException) problem;
		        }
		    } else {
		    	int status = response.getStatus();
		 	    if ( status == HttpServletResponse.SC_INTERNAL_SERVER_ERROR ){
		 	    	this.processError(problem, p_resp, url);
		 	    }
		    }
		    
		} else {
			p_filterChain.doFilter(p_req, p_resp);
		}
	}

	private void processError( Exception p_exception, ServletResponse p_resp, String p_url){
		LOGGER.info("IGZCODE ResponseFilter sending error email to " + mailTo);

		String body = "Error accessing " + p_url;
		if( p_exception != null ){
			body = body + "\n\r" + Trace.error( p_exception );
		}
        String subject = this.appName + " server error notification";
	    String fromEmail = mailFrom;
	    
		this.sendEmail(mailTo, fromEmail, body, subject);
	}
	
	private void sendEmail(String p_toMail, String p_fromEmail, String p_body, String p_subject) {
        String[] from = new String[] { p_fromEmail };
        String[][] to = new String[][] { { p_toMail, p_toMail } };
        String[][] cc = null;
        String[][] bcc = null;
        String[][] replyTo = null;
        
        try {
			MailUtil.sendHtmlMail(from, to, cc, bcc, replyTo, p_subject, p_body);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		ConfigUtil config = ConfigUtil.getInstance();
        config.init();
        
        if ( config.isDev() ) {
        	this.disabled = true;
        }
        
		this.mailFrom = filterConfig.getInitParameter("mailFrom");
		this.mailTo = filterConfig.getInitParameter("mailTo");
		this.appName = filterConfig.getInitParameter("appName");
	
		if ( this.mailFrom == null ) {
			throw new ServletException("IgzcodeGAE ResponseFilter must be configured in web.xml. Please add mailFrom param to the filter config.");
		}
		if ( this.mailTo == null ) {
			throw new ServletException("IgzcodeGAE ResponseFilter must be configured in web.xml. Please add mailTo param to the filter config.");
		}
		if ( this.appName == null ) {
			throw new ServletException("IgzcodeGAE ResponseFilter must be configured in web.xml. Please add appName param to the filter config.");
		}
	}
}
