package org.ihtsdo.ts.importer.resty;

import org.apache.http.entity.mime.MultipartEntity;
import us.monoid.web.AbstractContent;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLConnection;

public class MultipartEntityContent extends AbstractContent {

	private final MultipartEntity multipartEntity;

	public MultipartEntityContent(MultipartEntity multipartEntity) {
		this.multipartEntity = multipartEntity;
	}

	@Override
	protected void addContent(URLConnection con) throws IOException {
		sendEntity(con, multipartEntity);
	}

	@Override
	public void writeHeader(OutputStream os) throws IOException {
	}

	@Override
	public void writeContent(OutputStream os) throws IOException {
	}

	private void sendEntity(URLConnection urlConnection, MultipartEntity entity) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) urlConnection;
		conn.setReadTimeout(10000);
		conn.setConnectTimeout(15000);
		conn.setRequestMethod("POST");
		conn.setUseCaches(false);
		conn.setDoInput(true);
		conn.setDoOutput(true);

		conn.setRequestProperty("Connection", "Keep-Alive");
		conn.addRequestProperty("Content-length", entity.getContentLength()+"");
		conn.addRequestProperty(entity.getContentType().getName(), entity.getContentType().getValue());

		OutputStream os = conn.getOutputStream();
		entity.writeTo(conn.getOutputStream());
		os.close();
	}

}
