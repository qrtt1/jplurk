package com.google.jplurk;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.jplurk.action.PlurkActionSheet;
import com.google.jplurk.exception.PlurkException;
import com.google.jplurk.net.JPlurkResponseHandler;
import com.google.jplurk.net.ProxyProvider;

/**
 *
 * @author Askeing
 */
public class PlurkClient {

    // <editor-fold defaultstate="collapsed" desc="Init PlurkClient">
    static Logger logger = LoggerFactory.getLogger(PlurkClient.class);
    HttpClient client = new DefaultHttpClient();
    PlurkSettings config;

    static abstract class IdActions {

        abstract HttpUriRequest createMethod(Set<Integer> idSet) throws PlurkException;

        public JSONObject execute(PlurkClient client, String... ids) {
            try {
                Set<Integer> idSet = new HashSet<Integer>();
                for (String id : ids) {
                    idSet.add(NumberUtils.toInt(id, 0));
                }
                idSet.remove(0);

                return new JSONObject(client.execute(createMethod(idSet)));
            } catch (PlurkException e) {
                logger.error(e.getMessage(), e);
            } catch (JSONException e) {
                logger.error(e.getMessage(), e);
            }
            return null;
        }
    }

    /**
     *
     * @param settings
     */
    public PlurkClient(PlurkSettings settings) {
        this.config = settings;
        configureHttpClient();
    }

    private void configureHttpClient() {
        // Auth Proxy Setting
        if (StringUtils.isNotBlank(ProxyProvider.getUser())) {
            ((DefaultHttpClient) client).getCredentialsProvider().setCredentials(
                    new AuthScope(ProxyProvider.getHost(), ProxyProvider.getPort()),
                    new UsernamePasswordCredentials(ProxyProvider.getUser(), ProxyProvider.getPassword()));
        }
        // Proxy Host Setting
        if (StringUtils.isNotBlank(ProxyProvider.getHost())) {
            HttpHost proxy = new HttpHost(ProxyProvider.getHost(), ProxyProvider.getPort());
            client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                client.getConnectionManager().shutdown();
            }
        });
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="/API/Users/login">
    /**
     * /API/Users/login<br/>
     * Login and creat a cookie. This cookie can access other methods.
     * @param user
     * @param password
     * @return The JSONObject of /API/Profile/getOwnProfile
     */
    public JSONObject login(String user, String password) {

        try {
            HttpGet method = (HttpGet) PlurkActionSheet.getInstance().login(
                    config.createParamMap().k("username").v(user).k("password").v(password).getMap());

            JSONObject ret = new JSONObject(execute(method));
            return ret;
        } catch (PlurkException e) {
            logger.error(e.getMessage(), e);
        } catch (JSONException e) {
            logger.error(e.getMessage(), e);
        }

        return null;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="/API/Users/register">
    /**
     * /API/Users/register<br/>
     * Register a new user account.
     * @param nickName
     * @param fullName
     * @param password
     * @param gender
     * @param dateOfBirth
     * @return JSONObject with user info {"id": 42, "nick_name": "frodo_b", ...}
     */
    public JSONObject register(String nickName, String fullName,
            String password, Gender gender, String dateOfBirth) {
        return this.register(nickName, fullName, password, gender, dateOfBirth, "");
    }

    /**
     * /API/Users/register<br/>
     * Register a new user account. (with optional parameters.)
     * @param nickName
     * @param fullName
     * @param password
     * @param gender
     * @param dateOfBirth
     * @param email (optional)
     * @return JSONObject with user info {"id": 42, "nick_name": "frodo_b", ...}
     */
    public JSONObject register(String nickName, String fullName,
            String password, Gender gender, String dateOfBirth, String email) {
        final int FLAG = Pattern.DOTALL | Pattern.MULTILINE;
        Matcher m;
        // validation of nick_name
        m = Pattern.compile("[\\w]{3,}", FLAG).matcher(nickName);
        m.reset();
        if (!m.find()) {
            return null;
        }
        // validation of full_name
        m = Pattern.compile(".+", FLAG).matcher(fullName);
        m.reset();
        if (!m.find()) {
            return null;
        }
        // validation of password
        m = Pattern.compile(".{3,}", FLAG).matcher(password);
        m.reset();
        if (!m.find()) {
            return null;
        }
        // validation of date_of_birth
        m = Pattern.compile("[0-9]{4}\\-(0[1-9])|(1[0-2])\\-(0[1-9])|(1[0-9])|(2[0-9])|(3[0-1])", FLAG).matcher(dateOfBirth);
        m.reset();
        if (!m.find()) {
            return null;
        }
        // TODO: Check password need URLEncode? I try encode and work fine now...
        try {
            MapHelper paramMap = config.createParamMap().k("nick_name").v(nickName).k("full_name").v(fullName).k("password").v(password).k("gender").v(gender.toString()).k("date_of_birth").v(dateOfBirth);
            if (email != null && !email.equals((""))) {
                paramMap = paramMap.k("email").v(email);
            }
            HttpGet method = (HttpGet) PlurkActionSheet.getInstance().register(paramMap.getMap());

            JSONObject ret = new JSONObject(execute(method));
            return ret;
        } catch (PlurkException e) {
            logger.error(e.getMessage(), e);
        } catch (JSONException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="/API/Users/getPlurk">
    /**
     * /API/Users/getPlurk
     * @param plurkId
     * @return JSON object of the plurk and owner
     */
    public JSONObject getPlurk(String plurkId) {
        try {
            HttpGet method = (HttpGet) PlurkActionSheet.getInstance().getPlurk(
                    config.createParamMap().k("plurk_id").v(plurkId).getMap());
            return new JSONObject(execute(method));
        } catch (PlurkException e) {
            logger.error(e.getMessage(), e);
        } catch (JSONException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
    // </editor-fold>
    
    public JSONObject getPlurks(DateTime offset, int limit, int userId, boolean onlyResponsed, boolean onlyPrivate){
    	try {
    		MapHelper mapHelper = config.createParamMap()
    			.k("offset").v((offset == null ? DateTime.now() : offset).timeOffset())
    			.k("limit").v("" + (limit <= 0 ? 20 : limit));
			
			if (userId > 0) {
				mapHelper.k("only_user").v("" + userId);
			}
    		
    		if (onlyResponsed) {
				mapHelper.k("only_responded").v("true");
			}
			
			if (onlyPrivate) {
				mapHelper.k("only_private").v("true");
			}
			
			HttpGet method = (HttpGet) PlurkActionSheet.getInstance().getPlurks(
				mapHelper.getMap()
			);
			
			return new JSONObject(execute(method));
		} catch (PlurkException e) {
			logger.error(e.getMessage(), e);
		} catch (JSONException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
    }

    // <editor-fold defaultstate="collapsed" desc="/API/Timeline/getUnreadPlurks">
    /**
     * /API/Timeline/getUnreadPlurks<br/>
     * Get unread plurks.
     * @return JSONObject of unread plurks and their users
     */
    public JSONObject getUnreadPlurks() {
        return this.getUnreadPlurks(null);
    }

    /**
     * /API/Timeline/getUnreadPlurks<br/>
     * Get unread plurks older than offset.
     * @param offset (optional), formatted as 2009-6-20T21:55:34
     * @return JSONObject of unread plurks and their users
     */
    public JSONObject getUnreadPlurks(DateTime offset) {
        return this.getUnreadPlurks(offset, 0);
    }

    /**
     * /API/Timeline/getUnreadPlurks<br/>
     * Get the limited unread plurks.
     * @param limit (optional), Limit the number of plurks
     * @return JSONObject of unread plurks and their users
     */
    public JSONObject getUnreadPlurks(int limit) {
        return this.getUnreadPlurks(null, limit);
    }

    /**
     * /API/Timeline/getUnreadPlurks<br/>
     * Get the limited unread plurks older than offset.
     * @param offset (optional), formatted as 2009-6-20T21:55:34
     * @param limit (optional), limit the number of plurks. 0 as default, which will get 10 plurks
     * @return JSONObject of unread plurks and their users
     */
    public JSONObject getUnreadPlurks(DateTime offset, int limit) {
        try {
            MapHelper paramMap = config.createParamMap();
            if (offset != null) {
                paramMap = paramMap.k("offset").v(offset.timeOffset());
            }
            if (limit > 0) {
                paramMap = paramMap.k("limit").v(Integer.toString(limit));
            } else if (limit == 0) {
                paramMap = paramMap.k("limit").v("10");
            }
            HttpGet method = (HttpGet) PlurkActionSheet.getInstance().getUnreadPlurks(paramMap.getMap());
            return new JSONObject(execute(method));
        } catch (PlurkException e) {
            logger.error(e.getMessage(), e);
        } catch (JSONException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="/API/Timeline/plurkAdd">
    /**
     * /API/Timeline/plurkAdd<br/>
     * Add new plurk to timeline.
     * @param content
     * @param qualifier
     * @return JSON object of the new plurk
     */
    public JSONObject plurkAdd(String content, Qualifier qualifier) {
        return this.plurkAdd(content, qualifier, NoComments.False);
    }

    /**
     * /API/Timeline/plurkAdd<br/>
     * Add new plurk to timeline.
     * @param content
     * @param qualifier
     * @param no_comments (optional), true or false
     * @return JSON object of the new plurk
     */
    public JSONObject plurkAdd(String content, Qualifier qualifier, NoComments no_comments) {
        return this.plurkAdd(content, qualifier, null, no_comments, null);
    }

    /**
     * /API/Timeline/plurkAdd<br/>
     * Add new plurk to timeline.
     * @param content
     * @param qualifier
     * @param lang (optional)
     * @return JSON object of the new plurk
     */
    public JSONObject plurkAdd(String content, Qualifier qualifier, Lang lang) {
        return this.plurkAdd(content, qualifier, null, NoComments.False, lang);
    }

    /**
     * /API/Timeline/plurkAdd<br/>
     * Add new plurk to timeline.
     * @param content
     * @param qualifier
     * @param limited_to (optional), JSON Array contains friends ids
     * @param no_comments (optional), true or false
     * @param lang (optional)
     * @return JSON object of the new plurk
     */
    public JSONObject plurkAdd(String content, Qualifier qualifier, String limited_to, NoComments no_comments, Lang lang) {
        try {
            MapHelper paramMap = config.createParamMap().k("content").v(content).k("qualifier").v(qualifier.toString()).k("no_comments").v(no_comments.toString()).k("lang").v(lang == null ? config.getLang() : lang.toString());
            if (limited_to != null && !limited_to.equals("")) {
                paramMap = paramMap.k("limited_to").v(limited_to);
            }
            HttpGet method = (HttpGet) PlurkActionSheet.getInstance().plurkAdd(paramMap.getMap());
            return new JSONObject(execute(method));
        } catch (PlurkException e) {
            logger.error(e.getMessage(), e);
        } catch (JSONException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="/API/Timeline/plurkDelete">
    /**
     * /API/Timeline/plurkDelete<br/>
     * Deletes a plurk.
     * @param plurkId
     * @return {"success_text": "ok"}
     */
    public JSONObject plurkDelete(String plurkId) {
        try {
            HttpGet method = (HttpGet) PlurkActionSheet.getInstance().plurkDelete(
                    config.createParamMap().k("plurk_id").v(plurkId).getMap());
            return new JSONObject(execute(method));
        } catch (PlurkException e) {
            logger.error(e.getMessage(), e);
        } catch (JSONException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="/API/Timeline/plurkEdit">
    /**
     * /API/Timeline/plurkEdit<br/>
     * Edits a plurk.
     * @param plurkId
     * @param content
     * @return JSON object of the updated plurk
     */
    public JSONObject plurkEdit(String plurkId, String content) {
        try {
            HttpGet method = (HttpGet) PlurkActionSheet.getInstance().plurkEdit(
                    config.createParamMap().k("plurk_id").v(plurkId).k("content").v(content).getMap());
            return new JSONObject(execute(method));
        } catch (PlurkException e) {
            logger.error(e.getMessage(), e);
        } catch (JSONException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="/API/Timeline/mutePlurks">
    /**
     * /API/Timeline/mutePlurks
     * @param ids the plurk ids will be muted
     * @return JSONObject represent the {"success_text": "ok"}
     */
    public JSONObject mutePlurks(String... ids) {
        return new IdActions() {

            @Override
            HttpUriRequest createMethod(Set<Integer> idSet) throws PlurkException {
                return (HttpGet) PlurkActionSheet.getInstance().mutePlurks(
                        config.createParamMap().k("ids").v(new JSONArray(idSet).toString()).getMap());
            }
        }.execute(this, ids);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="/API/Timeline/unmutePlurks">
    /**
     * /API/Timeline/unmutePlurks
     * @param ids the plurk ids will be unmuted.
     * @return JSONObject represent the {"success_text": "ok"}
     */
    public JSONObject unmutePlurks(String... ids) {
        return new IdActions() {

            @Override
            HttpUriRequest createMethod(Set<Integer> idSet) throws PlurkException {
                return (HttpGet) PlurkActionSheet.getInstance().unmutePlurks(
                        config.createParamMap().k("ids").v(new JSONArray(idSet).toString()).getMap());
            }
        }.execute(this, ids);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="/API/Timeline/markAsRead">
    /**
     * /API/Timeline/markAsRead
     * @param ids the plurk ids will mark as read.
     * @return JSONObject represent the {"success_text": "ok"}
     */
    public JSONObject markAsRead(String... ids) {
        return new IdActions() {

            @Override
            HttpUriRequest createMethod(Set<Integer> idSet) throws PlurkException {
                return (HttpGet) PlurkActionSheet.getInstance().markAsRead(
                        config.createParamMap().k("ids").v(new JSONArray(idSet).toString()).getMap());
            }
        }.execute(this, ids);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="/API/Responses/responseAdd">
    /**
     * /API/Responses/responseAdd
     * @param plurkId which plurk will be response
     * @param content the responsed content
     * @param qualifier
     * @return JSON object
     */
    public JSONObject responseAdd(String plurkId, String content, Qualifier qualifier) {
        try {
            HttpGet method = (HttpGet) PlurkActionSheet.getInstance().responseAdd(
                    config.createParamMap().k("plurk_id").v(plurkId).k("content").v(content).k("qualifier").v(qualifier.toString()).getMap());
            return new JSONObject(execute(method));
        } catch (JSONException e) {
            logger.error(e.getMessage(), e);
        } catch (PlurkException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="/API/Responses/get">
    /**
     * /API/Responses/get
     * @param plurkId
     * @return JSON object
     */
    public JSONObject responseGet(String plurkId) {
        try {
            HttpGet method = (HttpGet) PlurkActionSheet.getInstance().responseGet(
                    config.createParamMap().k("plurk_id").v(plurkId).k("from_response").v("5").getMap());
            return new JSONObject(execute(method));
        } catch (PlurkException e) {
            logger.error(e.getMessage(), e);
        } catch (JSONException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="/API/Responses/responseDelete">
    /**
     * /API/Responses/responseDelete
     * @param plurkId the plurk id contains the response
     * @param responseId the id of the response will be deleted.
     * @return {"success_text": "ok"} when deletion is success, otherwise null.
     */
    public JSONObject responseDelete(String plurkId, String responseId) {
        try {
            HttpGet method = (HttpGet) PlurkActionSheet.getInstance().responseDelete(
                    config.createParamMap().k("plurk_id").v(plurkId).k("response_id").v(responseId).getMap());
            return new JSONObject(execute(method));
        } catch (JSONException e) {
            logger.error(e.getMessage(), e);
        } catch (PlurkException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }
    // </editor-fold>

	/**
	 * approval all friend requests
	 * @return {"success_text": "ok"} when request success, otherwise null
	 */
	public JSONObject addAllAsFriends() {
		try {
			HttpGet method = (HttpGet) PlurkActionSheet.getInstance()
				.addAllAsFriends(config.createParamMap().getMap());
			return new JSONObject(execute(method));
		} catch (PlurkException e) {
			logger.error(e.getMessage(), e);
		} catch (JSONException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

    /**
     * @param file a image file will be uploaded
     * @return json with thumbnail url. for example <pre>{"thumbnail":"http://images.plurk.com/tn_3146394_fb04befc28fbca59318f16d83d5c78cc.gif","full":"http://images.plurk.com/3146394_fb04befc28fbca59318f16d83d5c78cc.jpg"}</pre>
     */
    public JSONObject uploadPicture(File file){
		if (file == null || !file.exists() || !file.isFile()) {
			logger.warn("not a valid file: " + file);
			return null;
		}

    	HttpPost method = new HttpPost("http://www.plurk.com/API/Timeline/uploadPicture");
    	MultipartEntity entity = new MultipartEntity();
    	try {
			entity.addPart("api_key", new StringBody(config.getApiKey()));
			entity.addPart("image", new FileBody(file));
		} catch (UnsupportedEncodingException e) {
			logger.error(e.getMessage(), e);
		}
		method.setEntity(entity);

		try {
			return new JSONObject(execute(method));
		} catch (JSONException e) {
			logger.error(e.getMessage(), e);
		} catch (PlurkException e) {
			logger.error(e.getMessage(), e);
		}

		return null;
    }

    // <editor-fold defaultstate="collapsed" desc="Execution of HttpRequest">
    private String execute(HttpUriRequest method) throws PlurkException {
    	if(logger.isInfoEnabled()){
    		String uri = method.getURI().toString();
    		logger.info("execute: " + StringUtils.substringBefore(uri, "?"));
    	}
        String result = "";
        try {
            result = (String) client.execute(method, new JPlurkResponseHandler());
        } catch (Exception e) {
            throw new PlurkException(e);
        }
        return result;
    }
    // </editor-fold>


    /**
     *
     * @param args
     * @throws com.google.jplurk.exception.PlurkException
     * @throws org.apache.http.client.ClientProtocolException
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     */
    public static void main(String[] args) throws PlurkException, ClientProtocolException, IOException, InterruptedException {
//        ProxyProvider.setProvider("proxyhost", 8080);
//        ProxyProvider.setProvider("twproxy.trendmicro.com", 8080);

    	PlurkSettings config = new PlurkSettings();
        PlurkClient pc = new PlurkClient(config);

//                JSONObject oRegister = pc.register(JOptionPane.showInputDialog("nick_name"),
//                        JOptionPane.showInputDialog("full_name"),
//                        JOptionPane.showInputDialog("password"),
//                        JOptionPane.showInputDialog("gender"),
//                        JOptionPane.showInputDialog("date_of_birth"));
//                System.out.println(oRegister);

        JSONObject o = pc.login(JOptionPane.showInputDialog("id"), JOptionPane.showInputDialog("password"));
        System.out.println(o);
        System.out.println(pc.getPlurk("186562865"));
        System.out.println(pc.responseGet("186562865"));
        
        
//        186562865 , 186616350  : fail
//        186567616 : ok


//        System.out.println(pc.uploadPicture(new File("C:/images/image.jpg")));

//        JSONObject oo = pc.plurkAdd("測試 jPlurk 編輯 plruk 功能！！", Qualifier.IS, NoComments.False);
//        JSONObject oo = pc.plurkAdd("hmmmm", Qualifier.SAYS);
//        System.out.println(oo);

//        JSONObject ogp = pc.getPlurk("183532425");
//        System.out.println(ogp);

//        JSONObject oe = pc.plurkEdit("183532425", "早上的 http://code.google.com/p/jplurk/ (jPlurk) 先這樣吧～ by jPlurk v2 plurkEdit");
//        System.out.println(oe);

//        JSONObject od = pc.plurkDelete("183525435");
//        System.out.println(od);

//        JSONObject js10 = pc.getUnreadPlurks(DateTime.now(), 1);
//        System.out.println(js10);


//        System.out.println(pc.mutePlurks("183559649"));
//        Scanner scanner = new Scanner(System.in);
//        scanner.next();
//        System.out.println(pc.unmutePlurks("183559649"));

//        JSONObject js = pc.getUnreadPlurks(DateTime.now());
//        System.out.println(js);

//        JSONObject ora = pc.responseAdd("183532425", "測試刪除回應", Qualifier.FEELS);
//        System.out.println(ora);
//{"posted":"Sat, 19 Dec 2009 07:57:07 GMT","user_id":3290989,"content_raw":"測試刪除回應","lang":"en","content":"測試刪除回應","qualifier":"feels","id":825994340,"plurk_id":183532425}
//        JSONObject ord = pc.responseDelete("183532425", "825994340");
//        System.out.println(ord);

    }
}