package es.upm.dit.gocd.brooklyn;

public class DeploymentState {

	private int idState = -1;
	private String message = null;

	public static final DeploymentState DEPLOYMENT_STOPPED = new DeploymentState(0, "The deployment task has not started yet."); 
	public static final DeploymentState DEPLOYMENT_RUNNING = new DeploymentState(1, "The deployment task is still running.");
	public static final DeploymentState DEPLOYMENT_SUCCESS = new DeploymentState(2, "The app has been deployed successfully."); 
	public static final DeploymentState DEPLOYMENT_FAILED = new DeploymentState(3, "Failed deploying. Please check the output."); 
	

	private DeploymentState() {
	}

	private DeploymentState(int idState, String message) {
		this.idState = idState;
		this.message = message;
	}

	public final int getIdState() {
		return idState;
	}

	public final String getMessage() {
		return message;
	}
}
