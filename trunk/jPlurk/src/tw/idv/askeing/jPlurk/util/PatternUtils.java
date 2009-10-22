package tw.idv.askeing.jPlurk.util;

import java.text.ParseException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PatternUtils {

	static Log logger = LogFactory.getLog(PatternUtils.class);

	final static int FLAG = Pattern.DOTALL | Pattern.MULTILINE;

	public static String getPropertyWithIntValue(String input, String prop) {
		Pattern patt = Pattern.compile(".*" + prop + "[^:]+:\\s*(\\d+).*", FLAG);
		Matcher m = patt.matcher(input);
		if (m.matches()) {
			return m.group(1);
		}
		return "";
	}

	/**
	 * the posted field in the plurk json response use <b>new Date("Sun, 05 Jul 2009 02:56:30 GMT")</b> define the date, use replaceJsDateToTimestamp method can transform it to timestamp
	 * @param input
	 * @return
	 */
	public static String replaceJsDateToTimestamp(String input){
		logger.info(input);
		Matcher m = Pattern.compile("(new Date\\([^()]+\\))", Pattern.DOTALL | Pattern.MULTILINE).matcher(input);
		m.reset();
        boolean result = m.find();
        StringBuffer sb = new StringBuffer();
        if (result) {
            do {
            	Date date = null;;
				try {
					date = TimeUtil.fromJsDate(m.group(1));
					if(date == null){
						logger.warn("cannot convert js date to java data");
						logger.warn(m.group(1));
						date = new Date();
					}
					logger.debug("transform `" + m.group(1) + "' to timestamp: " + date.getTime());
	                m.appendReplacement(sb, "" + date.getTime());
	                result = m.find();
				} catch (ParseException e) {
					logger.error(e.getMessage(), e);
					break;
				}

            } while (result);
            m.appendTail(sb);
        }
        return sb.toString();
	}


}
