package com.google.jplurk.net;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;


public class JPlurkResponseHandler implements ResponseHandler<String> {

	static Logger logger = org.slf4j.LoggerFactory.getLogger(JPlurkResponseHandler.class);

    public String handleResponse(final HttpResponse response)
            throws HttpResponseException, IOException {
    	HttpEntity entity = response.getEntity();
    	String ret = (entity == null ? null : EntityUtils.toString(entity));

		if (logger.isDebugEnabled()) {
			Header[] headers = response.getAllHeaders();
			for (Header header : headers) {
				logger.debug(header.toString());
			}
		}

        StatusLine statusLine = response.getStatusLine();
        if (statusLine.getStatusCode() >= 300) {
        	logger.warn("Http Response Body: \n" + ret);
            throw new HttpResponseException(statusLine.getStatusCode(),
                    statusLine.getReasonPhrase());
        }

        return ret;
    }

}