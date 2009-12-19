package com.googlecode.jplurk.behavior;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tw.idv.askeing.jPlurk.Constants;
import tw.idv.askeing.jPlurk.util.TimeUtil;

import com.googlecode.jplurk.net.Request;

public class GetPlurks implements IBehavior {

	static Log logger = LogFactory.getLog(GetPlurks.class);

	@Override
	public boolean action(Request params, Object arg) {
		params.setEndPoint(Constants.GET_PLURK_URL);
		params.addParam("user_id", params.getUserUId());
		if (arg != null && arg instanceof String) {
			if (TimeUtil.isValidJsOutputFormat("" + arg)) {
				logger.info("apply offset: " + arg);
				params.addParam("offset", "\"" + arg + "\"");
			}
		}
		return true;
	}
}