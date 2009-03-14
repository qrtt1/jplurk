/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tw.idv.askeing.jPlurk;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
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
		GetMethod method = new GetMethod(Constants.GET_URL_M);
        method.setRequestHeader("Cookie", CookieGetter.getCookie(
                Constants.PLURK_HOST, Constants.LOGIN_URL_M, user, null));

        HttpTemplate template = new HttpTemplate(method);
        Object result = template.execute(new int[]{HttpStatus.SC_MOVED_TEMPORARILY,
                    HttpStatus.SC_OK}, new HttpResultCallback() {

            @Override
            protected Object processResult(GetMethod method) {
                try {
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(method.getResponseBodyAsStream(), "UTF-8"));

                    String line = "";
                    while ((line = in.readLine()) != null) {
                        logger.debug(line);
                        // Review: need improvement.
                        if (line.contains("<input type=\"hidden\" name=\"user_id\" value=\"")) {
                            String[] sUID = line.split("\"");
                            return Integer.valueOf((sUID[5]));
                        }
                    }
                } catch (Exception e) {
                   logger.error(e.getMessage(), e);
                }
                return Integer.valueOf(0);
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
    }
}