package tw.idv.askeing.jPlurk.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TimeUtil {
	
	static Log logger = LogFactory.getLog(TimeUtil.class);
	public final static SimpleDateFormat format = 
		new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
	
	public final static SimpleDateFormat JS_INPUT_FORMAT = 
		new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z", Locale.US);

	public static String format(Date date) {
		return format.format(date);
	}

	public static String format(Calendar calendar) {
		return format(calendar.getTime());
	}

	public static String now() {
		return format(new Date());
	}

	/**
	 * eval javascript date object
	 * <pre>
	 * Date date = TimeUtil.fromJsDate("new Date('Sat, 04 Jul 2009 13:31:23 GMT')");
	 * </pre>
	 * @param jsDate js date object. for example: <b>new Date('Sat, 04 Jul 2009 13:31:23 GMT')</b>
	 * @return
	 * @throws ParseException 
	 */
	public static Date fromJsDate(String jsDate) throws ParseException{
		String inputDate = jsDate.contains("new Date") ? StringUtils.substringBetween(jsDate, "\"") :jsDate;
		logger.debug("parse data: " + inputDate);
		return JS_INPUT_FORMAT.parse(inputDate);
	}
}
