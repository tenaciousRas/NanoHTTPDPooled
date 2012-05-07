import java.io.File;
import java.io.IOException;
import java.util.Properties;

import android.content.Context;
import android.util.Log;

/**
 * <p>
 * AndroidHTTPD version 0.5, Copyright &copy; 2012 Free Beachler
 * (fbeachler@gmail.com, http://github.com/tenaciousRas)
 * 
 * @author fbeachler
 * 
 */
public class AndroidHTTPD extends NanoHTTPDPooled {

	public static final String TAG = "AndroidHTTPD";

	/**
	 * Implementors handle HTTP requests delegated by AndroidHTTPD#serve(java.lang.String,
	 * java.lang.String, java.util.Properties, java.util.Properties,
	 * java.util.Properties)
	 */
	public static interface RequestHandler {
		public Response onRequestReceived(String uri, String method,
				Properties header, Properties parms, Properties files);
	}

	/**
	 * The request handler set for this instance.
	 */
	private RequestHandler requestHandler;

	public AndroidHTTPD(Context ctx, int port, File wwwroot,
			RequestHandler requestHandler) throws IOException {
		super(port, wwwroot);
		this.requestHandler = requestHandler;
		Log.i(TAG,
				new StringBuilder().append(
						"server started and listening in background")
						.toString());
	}

	/**
	 * @return the requestHandler
	 */
	public RequestHandler getRequestHandler() {
		return requestHandler;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * NanoHTTPD#serve(java.lang.String,
	 * java.lang.String, java.util.Properties, java.util.Properties,
	 * java.util.Properties)
	 */
	@Override
	public Response serve(String uri, String method, Properties header,
			Properties parms, Properties files) {
		Log.i(TAG,
				new StringBuilder().append("#serve called with uri=")
						.append(uri).append(", method=").append(method)
						.append(", header=")
						.append(null == header ? null : header.toString())
						.append(", parms=")
						.append(null == parms ? null : parms.toString())
						.toString());
		if (null != requestHandler) {
			return requestHandler.onRequestReceived(uri, method, header, parms,
					files);
		}
		return super.serve(uri, method, header, parms, files);
	}

}
