package tw.idv.askeing.jPlurk.net;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tw.idv.askeing.jPlurk.model.AccountModel;

public class HttpMethodUtil {

	static Log logger = LogFactory.getLog(HttpMethodUtil.class);

	static Map<String, String> COOKIE_NAMEKEY_URI_MAP = null;

	static {
		COOKIE_NAMEKEY_URI_MAP = new HashMap<String, String>();
		COOKIE_NAMEKEY_URI_MAP.put("/m/login", "username");
		COOKIE_NAMEKEY_URI_MAP.put("/Users/login", "nick_name");
	}

	/**
	 * if request uri is /m/login set nameKey as  username
	 * @param user
	 * @param method
	 * @param optCookie
	 * @return
	 */
	public static PostMethod prepareForQueryCookie(AccountModel user,
			String uri, String optCookie) {
		PostMethod method = new PostMethod(uri);
		try {
			if (!COOKIE_NAMEKEY_URI_MAP.containsKey(method.getURI().toString())) {
				logger.info("use optional cookie directly");
				method.addParameter(new NameValuePair("Cookie", optCookie));
				return method;
			}

			logger.info("login to url[" + method.getURI()
				+ "] with nameKey["	+ COOKIE_NAMEKEY_URI_MAP.get(method.getURI()) + "] ");

			method.addParameter(new NameValuePair(
				COOKIE_NAMEKEY_URI_MAP.get(method.getURI().toString()), user.getName()));

			method.addParameter(new NameValuePair("password", user.getPassword()));

		} catch (URIException e) {
			logger.error(e.getMessage(), e);
		}

		return method;
	}
}