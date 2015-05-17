package es.upm.dit.gocd.brooklyn;

import java.io.IOException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.plugin.api.task.JobConsoleLogger;

public class CheckDeploymentTask {

	private class TimeoutTask extends TimerTask{

		public void run() {
			throwTimeout();
		}

	}

	private boolean taskComplete = false; // Indicate if the task is done

	private DeploymentState currentState;

	private TimeoutTask timeout = null;

	private static Timer timerInstance = null; 
	
	private String urlBrooklyn;
	
	private String idApplication;
	
	private JobConsoleLogger console;

	
	public CheckDeploymentTask(String urlBrooklyn, String idApplication, JobConsoleLogger console){
		
		this.urlBrooklyn = urlBrooklyn;
		this.idApplication = idApplication;
		this.console = console;
		setCurrentState(DeploymentState.DEPLOYMENT_STOPPED);
	}
	
	//Singleton timer
	private final synchronized static Timer getTimer(){

		if (timerInstance == null){
			timerInstance = new Timer(true);
		}
		return timerInstance;
	
	}


	private void throwTimeout(){

		consolePrintLine("Timeout. The task is taking too long to finish.");

	}


	private void cancelTimeout(){
		if (timeout != null) {
			timeout.cancel();
		}
	}
	
	

	public final DeploymentState execute(long timeLimit){
		Thread deployTaskThread = new Thread() {
			public void run() {
				HttpClient client = HttpClientBuilder.create().build();
				HttpGet get = new HttpGet(urlBrooklyn+ "/v1/applications/"+idApplication);

				String state = "STARTED";
				while(!state.equals("RUNNING")){
					HttpResponse response;
					try {
						response = client.execute(get);
						String result = EntityUtils.toString(response.getEntity());
						HashMap<String,Object> map = new Gson().fromJson(result, new TypeToken<HashMap<String, Object>>(){}.getType());
						state = map.get("status").toString();
				
					} catch (IOException e) {
						e.printStackTrace();
					}

					if(state.equals("UNKNOWN") || state.equals("STOPPED") || state.equals("STOPPING") || state.equals("ON_FIRE")){
						setCurrentState(DeploymentState.DEPLOYMENT_FAILED);
						consolePrintLine(currentState.getMessage());
						getAndSetTaskComplete(true);
						cancelTimeout();
						return;
					
					}

				}
				setCurrentState(DeploymentState.DEPLOYMENT_SUCCESS);
				consolePrintLine(currentState.getMessage());
				getAndSetTaskComplete(true);
				cancelTimeout();
				return;
			}	
		};
		
		setCurrentState(DeploymentState.DEPLOYMENT_RUNNING);
		timeout = new TimeoutTask();
		getTimer().schedule(timeout, timeLimit);
		deployTaskThread.start();
		//Blocking main thread; Waiting for a result.
		blockExecutor();

		
		return currentState;
		
	}
	
	
	
	private void blockExecutor(){	
			while (!isTaskComplete()){
				
			}
	}
	
	public final synchronized boolean isTaskComplete() {
		return taskComplete;
	}

	private synchronized boolean getAndSetTaskComplete(boolean taskComplete) {
		boolean priorState = this.taskComplete;
		this.taskComplete = taskComplete;
		return priorState;
	}
	
	private synchronized void consolePrintLine(String line){
		console.printLine(line);
	}
	
	private synchronized void setCurrentState(DeploymentState deploymentState){
		currentState = deploymentState;
	}
	
	public synchronized DeploymentState getCurrentState(){
		return currentState;
	}
}
