package tw.idv.askeing.jPlurk;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import tw.idv.askeing.jPlurk.model.Account;
import tw.idv.askeing.jPlurk.net.HttpResultCallback;
import tw.idv.askeing.jPlurk.net.HttpTemplate;

/**
 * jPlurk UIDGetter: get UID of User. If you get UID and UID != 0, then UID will store into AccountModel.
 * @author Askeing, Yen.
 * @version 1.0
 */
public class UIDManager {

    static Log logger = LogFactory.getLog(UIDManager.class);
    static Map<Account, Integer> cachedUids = new HashMap<Account, Integer>();

    private UIDManager() {
	}

    /**
     * Return UID of user.
     * @param user user account
     * @return UID
     */
    public static int getUID(Account user) {
    	if(isHitCache(user)){
    		return cachedUids.get(user).intValue();
    	}

    	cachedUids.put(user, fetchUid(user));
        return getUID(user);
    }

	static Integer fetchUid(Account user) {
		//GetMethod method = new GetMethod(Constants.GET_URL_M);
        GetMethod method = new GetMethod("/"+user.getName());
        method.setRequestHeader("Cookie", CookieGetter.getCookie(
                Constants.PLURK_HOST, Constants.LOGIN_URL_M, user, null));

        HttpTemplate template = new HttpTemplate(method);
        Object result = template.execute(new int[]{HttpStatus.SC_MOVED_TEMPORARILY,
                    HttpStatus.SC_OK}, new HttpResultCallback() {

            @Override
            protected Object processResult(GetMethod method) {
            	try {
                    //return NumberUtils.toInt(StringUtils.substringBetween(method.getResponseBodyAsString(), "name=\"user_id\" value=\"", "\" />"));
                    Iterator<String> it = getIterator(method.getResponseBodyAsStream(), "utf-8");
                    String line = "";
					while (it.hasNext()) {
						line = it.next();
                      //logger.debug(line);
                      if (line.contains("user_id") && line.contains("show_location")) {
                          break;
                      }
					}
                    logger.debug("Get Line: "+line);
                    logger.debug("Get ID: "+StringUtils.substringBetween(line, "\"user_id\": ", ", \"show_location\""));
                    return NumberUtils.toInt(StringUtils.substringBetween(line, "\"user_id\": ", ", \"show_location\""));
				} catch (Exception e) {
					return 0;
				}
            }
        });

        if (result != null && result instanceof Integer) {
            return (Integer) result;
        }

        return Integer.valueOf(0);
	}

	static boolean isHitCache(Account user) {
		return (cachedUids.containsKey(user)) && (cachedUids.get(user) != null);
	}

    /**
     * Test Case
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        Account user = new Account();
        Scanner scanner = new Scanner(System.in);
        System.out.print("Please input your name: ");
        user.setName(scanner.next());
        System.out.println("Name:" + user.getName());
        System.out.print("Please input your password: ");
        user.setPassword(scanner.next());
        System.out.println("Password:" + user.getPassword());

        System.out.println("\n===== Test =====\n");
        System.out.println("UID: " + UIDManager.getUID(user));

        System.out.println( NumberUtils.toInt(StringUtils.substringBetween(
                "var GLOBAL = {\"page_user\": {\"page_title\": \"= \u5634\u7832 \u3000\u3000\u3000\u3000\u3000\u3000 \u822a\u9053 =\", \"uid\": 3290989, \"is_channel\": 0, \"full_name\": \"Askeing\", \"id\": 3290989, \"num_of_fans\": 33, "
                , "uid\": ", ", \"is_channel")) );
    }
}
