package com.googlecode.jplurk.behavior;

import tw.idv.askeing.jPlurk.Constants;

import com.googlecode.jplurk.net.Request;

public class GetNotifications implements IBehavior {

	@Override
	public boolean action(Request params, Object arg) {
		params.setEndPoint(Constants.GET_NOTIFICATIONS);
		return true;
	}

}
