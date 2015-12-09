package org.sandroproxy.utils.pac;

import java.lang.ref.SoftReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.util.Log;

/*****************************************************************************
 * ProxySelector that will use a PAC script to find an proxy for a given URI.
 * 
 * @author Bernd Rosstauscher (proxyvole@rosstauscher.de) Copyright 2009
 ****************************************************************************/
public class PacProxySelector {

	// private static final String PAC_PROXY = "PROXY";
	private static final String PAC_SOCKS = "SOCKS";
	private static final String PAC_DIRECT = "DIRECT";

	private final static String TAG = "ProxyDroid.PAC";

	private PacScriptParser pacScriptParser;

	private HashMap<String, SoftReference<List<Proxy>>> cache = new HashMap<>();

	/*************************************************************************
	 * Constructor
	 * 
	 * @param pacSource
	 *            the source for the PAC file.
	 ************************************************************************/

	public PacProxySelector(PacScriptSource pacSource) throws Exception {
		super();
		selectEngine(pacSource);
	}

	/*************************************************************************
	 * Selects one of the available PAC parser engines.
	 * 
	 * @param pacSource
	 *            to use as input.
	 ************************************************************************/

	private void selectEngine(PacScriptSource pacSource) throws Exception {
		try {
			if (this.pacScriptParser != null) {
				PacScriptSource prevPacSrc = this.pacScriptParser.getScriptSource();
				if (prevPacSrc != null && pacSource.getScriptContent().equals(prevPacSrc.getScriptContent())) {
					return;
				}
			}
			pacSource.getScriptContent();		// Try to read here to handle error while proxy start
			this.pacScriptParser = new RhinoPacScriptParser(pacSource);
			this.cache.clear();
		} catch (Exception e) {
			Log.e(TAG, "PAC parser error.", e);
			throw e;
		}
	}

	/*************************************************************************
	 * select
	 * 
	 * @see java.net.ProxySelector#select(URI)
	 ************************************************************************/
	public List<Proxy> select(URI uri) {
		if (uri == null || uri.getHost() == null) {
			throw new IllegalArgumentException("URI must not be null.");
		}

		List<Proxy> res;
		SoftReference<List<Proxy>> v = cache.get(uri.toString());
		if (v != null) {
			res = v.get();
			if (res != null) {
//				Log.d(TAG, uri.toString() + " : cache hit!");
				return res;
			}
		}

//		Log.d(TAG, "no cache, start evaluating!");

//		// Fix for Java 1.6.16 where we get a infinite loop because
//		// URL.connect(Proxy.NO_PROXY) does not work as expected.
//		PacScriptSource scriptSource = this.pacScriptParser.getScriptSource();
//		if (String.valueOf(scriptSource).contains(uri.getHost())) {
//			return null;
//		}

		res = findProxy(uri);
		cache.put(uri.toString(), new SoftReference<>(res));
		return res;
	}

	/*************************************************************************
	 * Evaluation of the given URL with the PAC-file.
	 * 
	 * Two cases can be handled here: DIRECT Fetch the object directly from the
	 * content HTTP server denoted by its URL PROXY name:port Fetch the object
	 * via the proxy HTTP server at the given location (name and port)
	 * 
	 * @param uri
	 *            <code>URI</code> to be evaluated.
	 * @return <code>Proxy</code>-object list as result of the evaluation.
	 ************************************************************************/

	private List<Proxy> findProxy(URI uri) {
		try {
			List<Proxy> proxies = new ArrayList<Proxy>();
			String parseResult = this.pacScriptParser.evaluate(uri.toString(),
					uri.getHost());
			String[] proxyDefinitions = parseResult.split("[;]");
			for (String proxyDef : proxyDefinitions) {
				if (proxyDef.trim().length() > 0) {
					proxies.add(buildProxyFromPacResult(proxyDef));
				}
			}
			return proxies;
		} catch (ProxyEvaluationException e) {
			Log.e(TAG, "PAC resolving error.", e);
			return null;
		}
	}

	/*************************************************************************
	 * The proxy evaluator will return a proxy string. This method will take
	 * this string and build a matching <code>Proxy</code> for it.
	 * 
	 * @param pacResult
	 *            the result from the PAC parser.
	 * @return a Proxy
	 ************************************************************************/

	private Proxy buildProxyFromPacResult(String pacResult) {
		if (pacResult == null || pacResult.trim().length() < 6) {
			return Proxy.NO_PROXY;
		}
		String proxyDef = pacResult.trim();
		if (proxyDef.toUpperCase().startsWith(PAC_DIRECT)) {
			return Proxy.NO_PROXY;
		}

		// Check proxy type.
		String type = Proxy.TYPE_HTTP;
		if (proxyDef.toUpperCase().startsWith(PAC_SOCKS)) {
			type = Proxy.TYPE_SOCKS5;
		}

		String host = proxyDef.substring(6);
		Integer port = 80;

		// Split port from host
		int indexOfPort = host.indexOf(':');
		if (indexOfPort != -1) {
			port = Integer.parseInt(host.substring(indexOfPort + 1).trim());
			host = host.substring(0, indexOfPort).trim();
		}

		return new Proxy(host, port, type);
	}
}
