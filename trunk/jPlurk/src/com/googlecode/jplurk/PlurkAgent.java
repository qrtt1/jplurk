package com.googlecode.jplurk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import tw.idv.askeing.jPlurk.model.Account;
import tw.idv.askeing.jPlurk.model.Message;
import tw.idv.askeing.jPlurk.model.Qualifier;
import tw.idv.askeing.jPlurk.model.ResponseMessage;
import tw.idv.askeing.jPlurk.util.JsonUtil;

import com.googlecode.jplurk.behavior.AddPlurk;
import com.googlecode.jplurk.behavior.AllowOrDenyFriendRequest;
import com.googlecode.jplurk.behavior.GetNotifications;
import com.googlecode.jplurk.behavior.GetPlurks;
import com.googlecode.jplurk.behavior.GetResponsePlurks;
import com.googlecode.jplurk.behavior.GetUnreadPlurks;
import com.googlecode.jplurk.behavior.IBehavior;
import com.googlecode.jplurk.behavior.Login;
import com.googlecode.jplurk.behavior.ResponsePlurk;
import com.googlecode.jplurk.exception.LoginFailureException;
import com.googlecode.jplurk.exception.NotLoginException;
import com.googlecode.jplurk.exception.RequestFailureException;
import com.googlecode.jplurk.net.Result;

/**
 * PlurkAgent is a facade that assemble many plurk's behavior in one class.
 * @author Ching Yi, Chan
 */
public class PlurkAgent implements IPlurkAgent {

	static Log logger = LogFactory.getLog(PlurkAgent.class);

	private Account account;
	private PlurkTemplate plurkTemplate;

	private boolean isLogin;

	public PlurkAgent(Account account) {
		this.account = account;
		this.plurkTemplate = new PlurkTemplate(account);
	}

	/**
	 * @throws RequestFailureException
	 * @see com.googlecode.jplurk.IPlurkAgent#addLongPlurk(tw.idv.askeing.jPlurk.model.Qualifier, java.lang.String)
	 */
	public Result addLongPlurk(Qualifier qualifier, String longText) throws RequestFailureException{
		List<String> texts = new ArrayList<String>();
		StringBuffer buf = new StringBuffer(longText);
		while(buf.length()>0){
			if(buf.length() <= 135){
				texts.add(buf.toString());
				buf.setLength(0);
				continue;
			}

			texts.add(buf.substring(0, 135));
			buf.delete(0, 135);
		}

		String first = texts.remove(0);
		Result result = addPlurk(qualifier, first);
		String plurkId =  (String) result.getAttachement().get("plurk_id");
		String plurkOwnerId = (String) result.getAttachement().get("owner_id");
		for (String t : texts) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException ignored) {
			}
			responsePlurk(qualifier, plurkId, plurkOwnerId, t);
		}

		return result;
	}

	/**
	 * @throws RequestFailureException
	 * @see com.googlecode.jplurk.IPlurkAgent#addPlurk(tw.idv.askeing.jPlurk.model.Qualifier, java.lang.String)
	 */
	@SuppressWarnings("unchecked")
	public Result addPlurk(Qualifier qualifier, String text) throws RequestFailureException{
		Message message = new Message();
		message.setQualifier(qualifier);
		message.setContent(text);
		Result result = execute(AddPlurk.class, message);

		// parse json response and attach the plurk_id and owner_id
		JSONObject plurkObject = (JSONObject) JsonUtil.parse(result.getResponseBody()).get("plurk");
		result.getAttachement().putAll(JsonUtil.get(plurkObject, "plurk_id", "owner_id"));
		return result;
	}

	@SuppressWarnings({ "unchecked", "serial" })
	@Override
	public Result allowFriendRequest(final int uid) throws RequestFailureException {
		Result result = execute(AllowOrDenyFriendRequest.class, new HashMap(){
			{
				put("type", "allow");
				put("uid", uid);
			}
		});
		return result;
	}

	@SuppressWarnings({ "unchecked", "serial" })
	@Override
	public Result denyFriendRequest(final int uid) throws RequestFailureException {
		Result result = execute(AllowOrDenyFriendRequest.class, new HashMap(){
			{
				put("type", "deny");
				put("uid", uid);
			}
		});
		return result;
	}

	/**
	 * execute is a standard process to check login status and execute behavior.
	 * if the agent get a login failure in previous login method call, it raise NotLoginException.
	 * if the agent get the result is not ok, it raise the RequestFailureException.
	 * @param clazz
	 * @param args
	 * @return
	 * @throws RequestFailureException
	 */
	protected Result execute(Class<? extends IBehavior> clazz, Object args) throws RequestFailureException{
		if(!isLogin) {
			throw new NotLoginException();
		}

		Result result = plurkTemplate.doAction(clazz, args);

		if(!result.isOk()){
			throw new RequestFailureException(result);
		}
		return result;
	}

	/**
	 * @see com.googlecode.jplurk.IPlurkAgent#getNotifications()
	 */
	@SuppressWarnings("unchecked")
	public Result getNotifications() throws RequestFailureException{
		// input example
		// DI( Notifications.render( 6543452, 0) );
		Result result = execute(GetNotifications.class, null);
		List<Integer> uids = new ArrayList<Integer>();
		for (String line : result.getResponseBody().split("\n")) {
			if(line != null && line.contains("Notifications.render(")){
				String uid = StringUtils.trimToEmpty(StringUtils.substringBetween(line, "Notifications.render(", ","));
				logger.debug("get notification from uid: " + NumberUtils.toInt(uid, -1));
				if(NumberUtils.toInt(uid, -1) != -1){
					uids.add(NumberUtils.toInt(uid, -1));
				}
			}
		}
		result.getAttachement().put("uids", uids);
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Result getPlurks() throws RequestFailureException {
		Result result = execute(GetPlurks.class, null);
		if(result.isOk()){
			try {
				JSONArray array = JsonUtil.parseArray(result.getResponseBody());
				result.getAttachement().put("json", array);
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Result getResponsePlurks(Integer plurkId) throws RequestFailureException {
		Result result = execute(GetResponsePlurks.class, plurkId);
		if(result.isOk()){
			try {
				result.getAttachement().put("json", JsonUtil.parse(result.getResponseBody()));
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		return result;
	}

//	@SuppressWarnings({ "unchecked", "serial" })
//	@Override
//	public Result makeFan(final int uid) throws RequestFailureException {
//		Result result = execute(AllowOrDenyFriendRequest.class, new HashMap(){
//			{
//				put("type", "fan");
//				put("uid", uid);
//			}
//		});
//		return result;
//	}

	/**
	 * @throws RequestFailureException
	 * @see com.googlecode.jplurk.IPlurkAgent#getUnreadPlurks()
	 */
	@SuppressWarnings("unchecked")
	public Result getUnreadPlurks() throws RequestFailureException{
		Result result = execute(GetUnreadPlurks.class, null);
		for (Object each : JsonUtil.parseArray(result.getResponseBody())) {
			if (each instanceof JSONObject) {
				JSONObject o = (JSONObject) each;
				result.getAttachement().put("" + o.get("plurk_id"), o);
			}
		}
		return result;
	}

	/**
	 * @see com.googlecode.jplurk.IPlurkAgent#login()
	 */
	public Result login() throws LoginFailureException {
		Result result = plurkTemplate.doAction(Login.class, account);
		if (!result.isOk()) {
			throw new LoginFailureException(account);
		}
		isLogin = true;
		return result;
	}

	/**
	 * @throws RequestFailureException
	 * @see com.googlecode.jplurk.IPlurkAgent#responsePlurk(tw.idv.askeing.jPlurk.model.Qualifier, java.lang.String, java.lang.String, java.lang.String)
	 */
	public Result responsePlurk(Qualifier qualifier, String plurkId, String plurkOwnerId, String text) throws RequestFailureException{
		ResponseMessage message = new ResponseMessage();
		message.setQualifier(qualifier);
		message.setContent(text);
		message.setPlurkId(plurkId);
		message.setPlurkOwnerId(plurkOwnerId);
		return execute(ResponsePlurk.class, message);
	}

}
