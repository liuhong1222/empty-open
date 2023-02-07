package com.zhongzhi.empty.http;

import okhttp3.*;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

/**
 * OkhttpService
 * @author liuh
 * @date 2021年10月28日
 */
@Slf4j
public class OkHttpService {
	
	/**
	 * pakage form body from Map<String,String>
	 * @param params Map<String,String>
	 * @return FormBody
	 */
	protected FormBody packageFormBody(Map<String,String> params) {
		final FormBody.Builder builder = new FormBody.Builder();
		params.forEach((k,v)->{
			if(v != null && !"null".equals(v)) {
				builder.add(k, v);
			}
		});
		return builder.build();
	}
	
	/**
	 * dopost method
	 * 
	 * @param url
	 * @param params
	 * @return
	 * @throws IOException
	 */
	protected String doPost(String url, Map<String, String> params,CacheControl cacheControl,String urlName) throws IOException {
		long start=System.currentTimeMillis();
		FormBody body = packageFormBody(params);
		Request.Builder rBuilder = new Request.Builder();
		Request requestPost = rBuilder.url(url).post(body).cacheControl(cacheControl).build();
		Response res = null;
		try {
			res = ClientFactory.getInstance().getClient(urlName).getHttpClient().newCall(requestPost).execute();

			if (res.isSuccessful()) {
				return res.body().string();
			} else {
				String bodyString = res.body().string();
				log.error("doput for url: {} is failed, code:{}, body:{}", url, res.code(), bodyString);
				return bodyString;
			}
		} catch (Exception e){
			throw e;
		} finally {
			if(res!=null){
				res.close();
			}
			log.info( ",调用上游接口:{},耗时:{}",url,(System.currentTimeMillis()-start));
		}
	}

	/**
	 * 上传文件
	 * @param url
	 * @param fileName
	 * @param file
	 * @param cacheControl
	 * @param urlName
	 * @return
	 * @throws IOException
	 */
	protected String doPostFile(String url, String fileName,File file,CacheControl cacheControl,String urlName) throws IOException {
		long start=System.currentTimeMillis();
		
		RequestBody fileBody = RequestBody.create(MediaType.parse("multipart/form-data"), file);
		MultipartBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
				.addFormDataPart("file", fileName, fileBody)
				.build();
		
		Request.Builder rBuilder = new Request.Builder();
		Request requestPost = rBuilder.url(url).post(requestBody).cacheControl(cacheControl).build();
		Response res = null;
		try {
			res = ClientFactory.getInstance().getClient(urlName).getHttpClient().newCall(requestPost).execute();

			if (res.isSuccessful()) {
				return res.body().string();
			} else {
				String bodyString = res.body().string();
				log.error("doput for url: {} is failed, code:{}, body:{}", url, res.code(), bodyString);
				return bodyString;
			}
		} catch (Exception e){
			throw e;
		} finally {
			if(res!=null){
				res.close();
			}
			log.info( ",调用上游接口:{},耗时:{}",url,(System.currentTimeMillis()-start));
		}
	}
	
	/**
	 * 下载文件
	 * @param url
	 * @param filePath
	 * @return
	 */
	protected Boolean downloadFile(String url,String filePath) {
		InputStream inputStream = null;
		OutputStream out = null;
		try {
			File file = new File(filePath);
			if(!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			
			URL urlObject = new URL(url);
	        // 连接类的父类，抽象类
	        URLConnection urlConnection = urlObject.openConnection();
	        // http的连接类
	        HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;
	        // 设定请求的方法，默认是GET（对于知识库的附件服务器必须是GET，如果是POST会返回405。流程附件迁移功能里面必须是POST，有所区分。）
	        httpURLConnection.setRequestMethod("GET");
	        // 设置字符编码
	        httpURLConnection.setRequestProperty("Charset", "UTF-8");
	        // 打开到此 URL 引用的资源的通信链接（如果尚未建立这样的连接）。
	        int code = httpURLConnection.getResponseCode();
	        if(HttpURLConnection.HTTP_OK != code) {
	        	log.error("file download error, httpCode:{},url:{},filePath:{},info:{}",code,url,filePath,httpURLConnection.getErrorStream());
	        	return false;
	        };

	        inputStream = httpURLConnection.getInputStream();
	        
	        out = new FileOutputStream(file);
	        int size = 0;
	        int lent = 0;
	        byte[] buf = new byte[1024];
	        while ((size = inputStream.read(buf)) != -1) {
	            lent += size;
	            out.write(buf, 0, size);
	        }
	        
			return true;
		} catch (Exception e) {
			log.error("file download exception ,url:{},filePath:{},info:",url,filePath,e);
		}finally {
			try {
				inputStream.close();
				out.close();
			} catch (IOException e) {
				log.error("文件流关闭异常，info:",e);
			}
	        
		}
		
		return false;
	}

	/**
	 * dopost method
	 *
	 * @param url
	 * @param params
	 * @return
	 * @throws IOException
	 */
	protected String doPost(String url, Map<String, String> params,CacheControl cacheControl,String urlName, Map<String, String> headerParams) throws IOException {
		long start=System.currentTimeMillis();
		FormBody body = packageFormBody(params);
		Request.Builder rBuilder = new Request.Builder();

		if(headerParams != null) {
			for (String key : headerParams.keySet()) {
				rBuilder.addHeader(key,headerParams.get(key));
			}
		}

		Request requestPost = rBuilder.url(url).post(body).cacheControl(cacheControl).build();
		Response res = null;
		try {
			res = ClientFactory.getInstance().getClient(urlName).getHttpClient().newCall(requestPost).execute();
			if (res.isSuccessful()) {
				return res.body().string();
			} else {
				String bodyString = res.body().string();
				log.error("doput for url: {} is failed, code:{}, body:{}", url, res.code(), bodyString);
				return bodyString;
			}
		} catch (Exception e){
			throw e;
		} finally {
			if(res!=null){
				res.close();
			}
			log.info( ",调用上游接口:{},displayNum:{},耗时:{}",url,params.get("displaynum"),(System.currentTimeMillis()-start));
		}
		// return null;
	}

	/**
	 *
	 * @param url
	 * @param jsonStr
	 * @param cacheControl
	 * @return
	 * @throws IOException
	 */
	protected String doJsonPost(String url, String jsonStr, CacheControl cacheControl,String urlName, Map<String, String> headerParams) throws IOException {
		long startTime = System.currentTimeMillis();
		RequestBody requestBody = FormBody.create(MediaType.parse("application/json; charset=utf-8"), jsonStr);
		Request.Builder rBuilder = new Request.Builder();

		if(headerParams != null) {
			for (String key : headerParams.keySet()) {
				rBuilder.addHeader(key,headerParams.get(key));
			}
		}

		Request requestPost = rBuilder.url(url).post(requestBody).cacheControl(cacheControl).build();
		Response res = null;
		try {
			res = ClientFactory.getInstance().getClient(urlName).getHttpClient().newCall(requestPost).execute();

			if (res.isSuccessful()) {
				return res.body().string();
			} else {
				String bodyString = res.body().string();
				log.error("doput for url: {} is failed, code:{}, body:{}", url, res.code(), bodyString);
				return bodyString;
			}
		} finally {
			if(res != null) {
				res.close();
			}
			log.info("调用上游接口:{},耗时:{}",url, (System.currentTimeMillis() - startTime));
		}
		// return null;
	}

	/**
	 * json put请求
	 * @date 2021/4/26
	 * @param url
	 * @param jsonStr
	 * @param cacheControl
	 * @param urlName
	 * @param headerParams
	 * @return java.lang.String
	 */
	protected String doJsonPut(String url, String jsonStr, CacheControl cacheControl, String urlName, Map<String, String> headerParams) throws IOException {
		long startTime = System.currentTimeMillis();
		RequestBody requestBody = FormBody.create(MediaType.parse("application/json; charset=utf-8"), jsonStr);
		Request.Builder rBuilder = new Request.Builder();

		if(headerParams != null) {
			for (String key : headerParams.keySet()) {
				rBuilder.addHeader(key,headerParams.get(key));
			}
		}

		Request requestPut = rBuilder.url(url).put(requestBody).cacheControl(cacheControl).build();
		Response res = null;
		try {
			res = ClientFactory.getInstance().getClient(urlName).getHttpClient().newCall(requestPut).execute();

			if (res.isSuccessful()) {
				return res.body().string();
			} else {
				String bodyString = res.body().string();
				log.error("doput for url: {} is failed, code:{}, body:{}", url, res.code(), bodyString);
				return bodyString;
			}
		} finally {
			if(res != null) {
				res.close();
			}
			log.info("调用上游接口:{},耗时:{}", url, (System.currentTimeMillis()-startTime));
		}
	}


	/**
	 *
	 * @param url
	 * @param jsonStr
	 * @param cacheControl
	 * @return
	 * @throws IOException
	 */
	protected String doJsonPost(String url, String jsonStr, CacheControl cacheControl,String urlName) throws IOException {
		long startTime = System.currentTimeMillis();
		RequestBody requestBody = FormBody.create(MediaType.parse("application/json; charset=utf-8"), jsonStr);
		Request.Builder rBuilder = new Request.Builder();
		Request requestPost = rBuilder.url(url).post(requestBody).cacheControl(cacheControl).build();
		Response res = null;
		try {
			res = ClientFactory.getInstance().getClient(urlName).getHttpClient().newCall(requestPost).execute();
			if (res.isSuccessful()) {
				return res.body().string();
			} else {
				String bodyString = res.body().string();
				log.error("doput for url: {} is failed, code:{}, body:{}", url, res.code(), bodyString);
				return bodyString;
			}
		} finally {
			if(res != null) {
				res.close();
			}
			log.info("调用上游接口:{},耗时:{}",url, (System.currentTimeMillis() - startTime));
		}
		// return null;
	}

	@Override
	public void  finalize()  {
		ClientFactory.getInstance().getClient("").getHttpClient().connectionPool().evictAll();
	}
}
