package net.temerity.davsync;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.lang.IllegalArgumentException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.UsernamePasswordCredentials;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class DAVNetwork {
	private DateFormat lmfmt; // last modified formatter
	private File path;
	private String url;
	private Credentials creds;
	private DefaultHttpClient client;
	private XPathExpression expr = null;
	
	public DAVNetwork(DAVProfile profile, File kpfile) {
		url = "https://" + profile.getHostname() + profile.getResource();
		//File sdcard = Environment.getExternalStorageDirectory();
		path = kpfile;
		//no need to create dirs since the file MUST already exist...
		//path.getParentFile().mkdirs();
		client = new DefaultHttpClient();

		HttpParams params = client.getParams();
		HttpConnectionParams.setConnectionTimeout(params, 5000);
		HttpConnectionParams.setSoTimeout(params, 5000);

		creds = new UsernamePasswordCredentials(profile.getUsername(), profile.getPassword());
		AuthScope as = new AuthScope(profile.getHostname(), 443);
		client.getCredentialsProvider().setCredentials(as, creds); // also: AuthScope.ANY

		//CredentialsProvider cp = new BasicCredentialsProvider();
		//cp.setCredentials(AuthScope.ANY, creds);
		//client.setCredentialsProvider(cp);
		lmfmt = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z");
	}
	
	private class DAVNamespaceContext implements NamespaceContext {

		@Override public String getNamespaceURI(String prefix) {
			if( prefix.equals("D") ) {
				return "DAV:";
			} else if( prefix.equals("lp1") ) {
				return "DAV:";
			} else if( prefix.equals("lp2") ) {
				return "http://apache.org/dav/props/";
			} else if( prefix.equals("lp3") ) {
				return "http://subversion.tigris.org/xmlns/dav/";
			}
			return XMLConstants.NULL_NS_URI;
		}

		@Override public String getPrefix(String namespaceURI) {
			return null;
		}

		@Override public Iterator<String> getPrefixes(String namespaceURI) {
			return null;
		}
		
	}
	
	private Date getRemoteTimestamp() throws ClientProtocolException, IOException, ParserConfigurationException, SAXException {
		final String TAG = "DAVNetwork::getRemoteTimestamp";
		
		HttpPropFind hpf = new HttpPropFind(url);
		HttpResponse hr = client.execute(hpf);
		HttpEntity he = hr.getEntity();
		if( he == null ) {
			throw new SAXException("Caught illegal entity after executing PROPFIND");
		}
		InputStream xmlStream = he.getContent();

		long xmlLength = he.getContentLength();
		if (xmlLength < 0) {
			Log.d(TAG, "Could not get content length for PROPFIND: " + xmlLength);
			throw new IOException("Unknown MultiStatus length from PROPFIND");
		}
		
		byte[] xmlData = new byte[(int) xmlLength];
		int offset = 0, bytesRead = 0;
		while (offset < xmlLength && bytesRead >= 0) {
			bytesRead = xmlStream.read(xmlData, offset, xmlData.length - offset);
			offset += bytesRead;
		}

		xmlStream.close();
		he.consumeContent();

		if( offset < xmlData.length ) {
			Log.d(TAG, "FATAL: got " + offset + " bytes, expecting " + xmlLength + " bytes");
			throw new IOException("Unable to determine remote properties");
		}
		
		// parse the XML
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(new ByteArrayInputStream(xmlData));
		
		if( expr == null ) {
			XPathFactory factory = XPathFactory.newInstance();
			XPath xpath = factory.newXPath();
			xpath.setNamespaceContext(new DAVNamespaceContext());
			try {
				//expr = xpath.compile("/D:multistatus/D:response/D:propstat/D:prop/*[starts-with(name(), 'lp1:')]");
				expr = xpath.compile("/D:multistatus/D:response/D:propstat/D:prop/lp1:getlastmodified");
			} catch (XPathExpressionException e) {
				Log.d(TAG, "Unable to compile XPath expression: " + e);
				throw new SAXException("Unable to compile XPath expression");
			}
		}

		NodeList nodes;
		try {
			nodes = (NodeList)expr.evaluate(doc, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
			Log.d(TAG, "Unable to compile XPath expression");
			throw new SAXException("Unable to compile XPath expression");
		}
		
		if( nodes.getLength() != 1 ) {
			Log.d(TAG, "timestamp count for target != 1");
			throw new SAXException("Received incorrect PROPFIND XML");
		}

		try {
			Date lastModified = (Date)lmfmt.parse(nodes.item(0).getTextContent());
			Log.d(TAG, "Success: timestamp = " + lastModified);
			return lastModified;
		} catch (DOMException e) {
			Log.d(TAG, "Date content string too long");
		} catch (ParseException e) {
			Log.d(TAG, "Failed to parse date from WebDAV server");
		}
		throw new SAXException(); // should never get here

		/*
		SAXParserFactory spf = SAXParserFactory.newInstance();
		SAXParser sp = spf.newSAXParser();
		PropFindHandler pfh = new PropFindHandler();
		XMLReader xr = sp.getXMLReader();
		xr.setContentHandler(pfh);
		xr.parse(new InputSource(new ByteArrayInputStream(xmlData)));
		return pfh.lastModified;
		*/
	}

	public boolean testRemote() {
		final String TAG = "DAVNetwork::testRemote";
		try {
			HttpPropFind hpf = new HttpPropFind(url);
			HttpResponse hr = client.execute(hpf);
			int ret = hr.getStatusLine().getStatusCode();
			
			HttpEntity he = hr.getEntity();
			if( he != null ) {
				he.consumeContent();
			}
			
			if( ret == HttpStatus.SC_MULTI_STATUS || ret == HttpStatus.SC_NOT_FOUND ) {
				Log.d(TAG, "PROPFIND succeeded on remote resource and returned " + ret);
				return true;
			} else {
				Log.d(TAG, "PROPFIND failed on remote resource and returned " + ret);
				return false;
			}
		} catch (ClientProtocolException e) {
			Log.d(TAG, "Caught ClientProtocolException: " + e);
			return false;
		} catch (IOException e) {
			Log.d(TAG, "Caught IOException: " + e);
			return false;
		}
	}
	
	public boolean sync() throws IOException, IllegalArgumentException {
		final String TAG = "DAVNetwork::sync";
		Date date_remote, date_local;
		boolean has_remote, has_local;
		
		// get local & remote info
		try {
			date_remote = getRemoteTimestamp();
			has_remote = true;
		} catch( Exception e ) {
			date_remote = new Date(1900, 1, 1);
			has_remote = false;
		}
        if( path.exists() ) {
        	date_local = new Date(path.lastModified());
        	has_local = true;
        } else {
        	date_local = new Date(1900, 1, 1);
        	has_local = false;
        }

        // do the sync
        if( has_local == true && has_remote == false ) {
        	Log.d(TAG, "Uploading local file");
        	return upload(date_local);
        } else if( has_local == false && has_remote == true ) {
        	Log.d(TAG, "Downloading remote file");
        	return download(date_remote);
        } else if( has_local == false && has_remote == false ) {
        	// this should never happen
        	Log.d(TAG, "New KDB file creation unimplemented");
        	return false;
        } else {        	
        	Log.d(TAG, date_remote.toString() + " <=> " + date_local.toString() );
        	
        	int comparator = date_local.compareTo(date_remote);
        	if( comparator < 0 ) {
        		Log.d(TAG, "Final sync decision: download");
        		return download(date_remote);
        	} else if( comparator > 0 ) {
        		Log.d(TAG, "Final sync decision: upload");
        		return upload(date_local);
        	} else {
        		// the files are already synced, we do nothing
        		Log.d(TAG, "Final sync decision: the files are equal");
        		return true;
        	}
        }
	}
	
	// FIXME: is it possible to set the date on the uploaded file?
	private boolean upload(Date modified) {
		int ret = -1;
		final String TAG = "DAVNetwork::upload";
        
        HttpPut pm = new HttpPut(url);
		pm.setEntity(new FileEntity(path, "binary/octet-stream"));
		
		try {
			HttpResponse hr = client.execute(pm);
			ret = hr.getStatusLine().getStatusCode();
			
			HttpEntity he = hr.getEntity();
			if( he != null ) {
				he.consumeContent();
			}
			
			if( ret >= 200 && ret <= 226 ) {
				Log.d(TAG, "Put method successfully completed");
				/* hack, hack, hack: set the local file's modification date equal to the remote's */
				try {
					Date d = getRemoteTimestamp();
					if( ! path.setLastModified( d.getTime() ) )
							Log.d(TAG, "Could not set local timestamp [1]");
				} catch( Exception e ) {
					Log.d(TAG, "Could not set local timestamp [2]");
				}
				return true;
			} else {
				Log.d(TAG, "Failed to execute Put method: " + ret);
				return false;
			}
		} catch( IOException ioe ) {
			Log.d(TAG, "Caught IOException while uploading file");
			return false;
		}
		// common: HttpStatus.SC_NO_CONTENT HttpStatus.SC_CREATED HttpStatus.SC_OK
	}
	
	private boolean download(Date modified) {
		int ret = -1;
		final String TAG = "DAVNetwork::upload";
		HttpGet gm = new HttpGet(url);
		try {
			HttpResponse hr = client.execute(gm);
			ret = hr.getStatusLine().getStatusCode();
			HttpEntity entity = hr.getEntity();
			if (ret == HttpStatus.SC_OK && entity != null ) {
				// long rlen = entity.getContentLength();
			    InputStream input = entity.getContent();
				FileOutputStream output = new FileOutputStream(path, false);
				int count = -1;
				byte[] buffer = new byte[8192];
				while( (count = input.read(buffer)) != -1 ) {
					output.write(buffer, 0, count);
				}
				output.flush();
				output.close();
				input.close();
				if( ! path.setLastModified(modified.getTime()) ) {
					Log.w(TAG, "Failed to set local last-modified time equal to remote");
				}
			}
			entity.consumeContent();
			return true;
		} catch( IOException ioe ) {
			Log.d(TAG, "Caught IOException while downloading file");
			return false;
		}
	}
}
