package es.upm.dit.gocd.brooklyn;

import java.util.Map;

public class Config {

	private final String urlServer;
	private final String urlBlueprint;
	private final int timeout;
	private final String appName;
	private final String action;


	public Config(Map config) {

		urlServer = getValue(config, ApacheBrooklynTask.BROOKLYN_URL_PROPERTY);    	
		action = getValue(config, ApacheBrooklynTask.BROOKLYN_ACTION_PROPERTY);
		if(action.equals("deploy")){

			urlBlueprint = getValue(config, ApacheBrooklynTask.BLUEPRINT_URL_PROPERTY);
			timeout = Integer.parseInt(getValue(config, ApacheBrooklynTask.TIMEOUT_PROPERTY));
			appName = null;

		}else{

			// Action => Undeploy
			urlBlueprint = null;
			timeout = -1;
			appName = getValue(config, ApacheBrooklynTask.APPLICATION_NAME_PROPERTY);

		}         
	}

	private String getValue(Map config, String property) {
		return (String) ((Map) config.get(property)).get("value");
	}

	public String getUrlServer() {
		String urlServerFormatted = urlServer;
		if(urlServer.charAt(urlServer.length()-1) == '/'){
			urlServerFormatted = urlServer.substring(0,urlServer.length()-1);

		}
		return urlServerFormatted;
	}

	public String getBlueprintUrl(){
		return urlBlueprint;
	}

	public int getTimeout(){
		return timeout;
	}

	public String getAction(){
		return action;
	}

	public String getAppName() {
		return appName;
	}

}