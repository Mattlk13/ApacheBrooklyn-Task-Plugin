package es.upm.dit.gocd.brooklyn;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.plugin.api.task.JobConsoleLogger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.http.NameValuePair;


public class ApacheBrooklynTaskExecutor {

	private String urlBrooklyn;
	private String urlBlueprint;
	private String idApplication;
	private String appName;
	private String action;
	private int timeout;

	public Result execute(Config config, Context context, JobConsoleLogger console) {
		action = config.getAction();
		console.printLine("Starting Brooklyn app "+action+".");

		urlBrooklyn = config.getUrlServer();
		console.printLine("Getting url server -> "+urlBrooklyn);

		if(action.equals("deploy")){
		
			urlBlueprint = config.getBlueprintUrl();
			console.printLine("Getting url blueprint -> "+urlBlueprint);

			timeout = config.getTimeout();
			console.printLine("Getting timeout property. ");
		
		}else{
			
			//Undeploy action
			appName = config.getAppName();
			
			console.printLine("Getting app name -> "+appName);
		}



		try {
			return runCommand(context, config, console);
		} catch (Exception e) {
			return new Result(false, "Failed to deploy app", e);
		}
	}

	private int deployApp(String url, Context taskContext,  JobConsoleLogger console) throws Exception {


		HttpClient client = HttpClientBuilder.create().build();
		HttpPost post = new HttpPost(url + "/v1/applications");

		post.setHeader("Content-Type", "application/yaml");
		String filePath = taskContext.getWorkingDir() + "/prueba.yaml";
		File archivo = new File (filePath);
		FileReader fr = new FileReader (archivo);
		BufferedReader br = new BufferedReader(fr);
		String bom = "";
		String linea;
		while((linea = br.readLine()) != null){
			bom+=linea+"\n";
		}
		br.close();
		HttpEntity entity = new ByteArrayEntity(bom.getBytes("UTF-8"));
		post.setEntity(entity);
		HttpResponse response = client.execute(post);
		String result = EntityUtils.toString(response.getEntity());
		console.printLine(result);
		HashMap<String,Object> map = new Gson().fromJson(result, new TypeToken<HashMap<String, Object>>(){}.getType());
		idApplication = map.get("entityId").toString();
		return response.getStatusLine().getStatusCode();
	}

	private int downloadBlueprint(String url, Context taskContext,JobConsoleLogger console) throws Exception {

		HttpClient client = HttpClientBuilder.create().build();
		HttpGet get = new HttpGet(url);
		HttpResponse response = null;
		try{
			response = client.execute(get);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			console.printLine(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			console.printLine(e.getMessage());
		}

		HttpEntity entity = response.getEntity();

		if (entity != null) {

			InputStream inputStream = entity.getContent();

			BufferedInputStream bis = new BufferedInputStream(inputStream);
			String filePath = taskContext.getWorkingDir() + "/prueba.yaml";
			BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(filePath)));
			int inByte;
			while((inByte = bis.read()) != -1) bos.write(inByte);
			bis.close();
			bos.close();
		}

		return response.getStatusLine().getStatusCode();


	}

	private int stopApp(String url, String appName,Context taskContext,  JobConsoleLogger console) throws Exception {

		HttpClient client = HttpClientBuilder.create().build();
		HttpPost post = null;
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		pairs.add(new BasicNameValuePair("timeout", "0"));
		try{
			post = new HttpPost(url + "/v1/applications/"+ URLEncoder.encode(appName, "UTF-8").replace("+","%20") +"/entities/"+ URLEncoder.encode(appName, "UTF-8").replace("+","%20")+ "/effectors/stop");
			post.setHeader("Content-Type", "application/json");
			post.setEntity(new UrlEncodedFormEntity(pairs));
			
			//Is necessary to send a empty json. If we don't do it, we will get error
			String json = "{}";
			HttpEntity entity = new ByteArrayEntity(json.getBytes("UTF-8"));
			post.setEntity(entity);

		}catch (IllegalArgumentException e){
		
			console.printLine(e.toString());
		
		}
		
		console.printLine("Launching post request to stop effector of: " + appName);
		HttpResponse response = client.execute(post);
		String result = EntityUtils.toString(response.getEntity());
		console.printLine(result);
		return response.getStatusLine().getStatusCode();

	}

	private Result runCommand(Context taskContext, Config taskConfig, JobConsoleLogger console) throws Exception {

		if(action.equals("deploy")){
			console.printLine("Downloading blueprint");

			if(downloadBlueprint(urlBlueprint, taskContext,console) != 200){

				return new Result(false, "Failed deploying, the blueprint cannot be downloaded correctly. Please check the output");

			}

			console.printLine("Deploying app.");

			int statusCode = deployApp(urlBrooklyn, taskContext, console);

			System.out.println("Status code is: "+statusCode);
			if(statusCode != 201){

				return new Result(false, "Failed deploying. Please check the output.");

			}else{
				
				console.printLine("Deploying application with id: "+idApplication);

				DeploymentState deploymentState= new CheckDeploymentTask(urlBrooklyn, idApplication, console).execute(timeout*60*1000);

				if(deploymentState.equals(DeploymentState.DEPLOYMENT_SUCCESS)){
					return new Result(true, deploymentState.getMessage());
				}else{
					return new Result(false, deploymentState.getMessage());
				}

			}
		}else{
			int statusCode = 500;

			//Undeploy action
			try{
				statusCode = stopApp(urlBrooklyn, appName, taskContext, console);
			} catch (ClientProtocolException e) {
				return new Result(false, "Failed deploying. Please check the output: "+e.toString());
			} catch (IOException e) {
				return new Result(false, "Failed deploying. Please check the output: "+e.toString());
			}

			if(statusCode == 202){

				return new Result(true, "The app has been undeployed");

			}else{

				return new Result(false, "Failed stopping the application "+appName+". Please check Brooklyn server.");

			}
		}
	}

}
