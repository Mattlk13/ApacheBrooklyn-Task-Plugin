package es.upm.dit.gocd.brooklyn;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.api.task.JobConsoleLogger;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.IOUtils;

@Extension
public class ApacheBrooklynTask implements GoPlugin{

	public static final String BROOKLYN_URL_PROPERTY = "Url";
	public static final String BLUEPRINT_URL_PROPERTY = "UrlBlueprint";
	public static final String TIMEOUT_PROPERTY = "Timeout";
	public static final String APPLICATION_NAME_PROPERTY = "AppName";
	public static final String BROOKLYN_ACTION_PROPERTY = "Action";


	@Override
	public GoPluginApiResponse handle(GoPluginApiRequest request) throws UnhandledRequestTypeException {
		// TODO Auto-generated method stub
		if ("configuration".equals(request.requestName())) {
			return handleGetConfigRequest();
		} else if ("validate".equals(request.requestName())) {
			return handleValidation(request);
		} else if ("execute".equals(request.requestName())) {
			return handleTaskExecution(request);
		} else if ("view".equals(request.requestName())) {
			return handleTaskView();
		}
		throw new UnhandledRequestTypeException(request.requestName());
	}

	@Override
	public void initializeGoApplicationAccessor(GoApplicationAccessor arg0) {
		// TODO Auto-generated method stub

	}

	private GoPluginApiResponse handleTaskView() {
		int responseCode = DefaultGoApiResponse.SUCCESS_RESPONSE_CODE;
		Map view = new HashMap();
		view.put("displayValue", "Apache Brooklyn Task");
		try {
			view.put("template", IOUtils.toString(getClass().getResourceAsStream("/views/task.template.html"), "UTF-8"));
		} catch (Exception e) {
			responseCode = DefaultGoApiResponse.INTERNAL_ERROR;
			String errorMessage = "Failed to find template: " + e.getMessage();
			view.put("exception", errorMessage);
			//logger.error(errorMessage, e);
		}
		return createResponse(responseCode, view);
	}



	private GoPluginApiResponse handleGetConfigRequest() {
		HashMap config = new HashMap();
		HashMap brooklynUrl = new HashMap();
		brooklynUrl.put("required", false);
		config.put(BROOKLYN_URL_PROPERTY, brooklynUrl);
		HashMap blueprintUrl = new HashMap();
		blueprintUrl.put("required", false);
		config.put(BLUEPRINT_URL_PROPERTY, blueprintUrl);
		HashMap timeout = new HashMap();
		timeout.put("required", false);
		config.put(TIMEOUT_PROPERTY,timeout);
		HashMap appName = new HashMap();
		appName.put("required", false);
		config.put(APPLICATION_NAME_PROPERTY, appName);
		HashMap brooklynAction = new HashMap();
		brooklynAction.put("required", false);
		config.put(BROOKLYN_ACTION_PROPERTY, brooklynAction);
		return createResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, config);
	}

	private GoPluginApiResponse handleValidation(GoPluginApiRequest request) {
		HashMap validationResult = new HashMap();
		int responseCode = DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE;
		Map configMap = (Map) new GsonBuilder().create().fromJson(request.requestBody(), Object.class);

		if(!configMap.containsKey(BROOKLYN_ACTION_PROPERTY) || ((Map) configMap.get(BROOKLYN_ACTION_PROPERTY)).get("value") == null || ((String) ((Map) configMap.get(BROOKLYN_ACTION_PROPERTY)).get("value")).trim().isEmpty()){
			responseCode = DefaultGoPluginApiResponse.VALIDATION_FAILED;
			HashMap errorMap = new HashMap();
			errorMap.put(BROOKLYN_ACTION_PROPERTY, "Action property cannot be empty.");
			validationResult.put("errors", errorMap);
		}else{
			String action = (String) ((Map) configMap.get(BROOKLYN_ACTION_PROPERTY)).get("value");
			if(action.equals("deploy")){
				if (!configMap.containsKey(BROOKLYN_URL_PROPERTY) || ((Map) configMap.get(BROOKLYN_URL_PROPERTY)).get("value") == null || ((String) ((Map) configMap.get(BROOKLYN_URL_PROPERTY)).get("value")).trim().isEmpty()) {
					responseCode = DefaultGoPluginApiResponse.VALIDATION_FAILED;
					HashMap errorMap = new HashMap();
					errorMap.put(BROOKLYN_URL_PROPERTY, "URL Brooklyn server cannot be empty");
					validationResult.put("errors", errorMap);
				}	
				if (!configMap.containsKey(BLUEPRINT_URL_PROPERTY) || ((Map) configMap.get(BLUEPRINT_URL_PROPERTY)).get("value") == null || ((String) ((Map) configMap.get(BLUEPRINT_URL_PROPERTY)).get("value")).trim().isEmpty()) {
					responseCode = DefaultGoPluginApiResponse.VALIDATION_FAILED;
					HashMap errorMap = new HashMap();
					errorMap.put(BROOKLYN_URL_PROPERTY, "URL Blueprint cannot be empty");
					validationResult.put("errors", errorMap);
				}

				if(!configMap.containsKey(TIMEOUT_PROPERTY) || ((Map) configMap.get(TIMEOUT_PROPERTY)).get("value") == null || ((String) ((Map) configMap.get(TIMEOUT_PROPERTY)).get("value")).trim().isEmpty()){
					responseCode = DefaultGoPluginApiResponse.VALIDATION_FAILED;
					HashMap errorMap = new HashMap();
					errorMap.put(TIMEOUT_PROPERTY, "Tmeout property cannot be empty");
					validationResult.put("errors", errorMap);
				}else{
					if(!isNumeric(((String) ((Map) configMap.get(TIMEOUT_PROPERTY)).get("value")).trim())){
						responseCode = DefaultGoPluginApiResponse.VALIDATION_FAILED;
						HashMap errorMap = new HashMap();
						errorMap.put(TIMEOUT_PROPERTY, "Timeout property must be a number");
						validationResult.put("errors", errorMap);
					}else{
						if(Integer.parseInt((((String) ((Map) configMap.get(TIMEOUT_PROPERTY)).get("value")).trim()))<=0){
							responseCode = DefaultGoPluginApiResponse.VALIDATION_FAILED;
							HashMap errorMap = new HashMap();
							errorMap.put(TIMEOUT_PROPERTY, "Timeout property cannot be less or equal to 0");
							validationResult.put("errors", errorMap);

						}
					}
				}
			}else{
				if(action.equals("undeploy")){

					if (!configMap.containsKey(APPLICATION_NAME_PROPERTY) || ((Map) configMap.get(APPLICATION_NAME_PROPERTY)).get("value") == null || ((String) ((Map) configMap.get(APPLICATION_NAME_PROPERTY)).get("value")).trim().isEmpty()) {
						responseCode = DefaultGoPluginApiResponse.VALIDATION_FAILED;
						HashMap errorMap = new HashMap();
						errorMap.put(APPLICATION_NAME_PROPERTY, "Application name to stopping cannot be empty");
						validationResult.put("errors", errorMap);
					}
				}else{
					responseCode = DefaultGoPluginApiResponse.VALIDATION_FAILED;
					HashMap errorMap = new HashMap();
					errorMap.put(BROOKLYN_ACTION_PROPERTY, "Action property must be deploy or undeploy. The value "+action+" is undefined");
					validationResult.put("errors", errorMap);
				}
			}
				

			
		}

		return createResponse(responseCode, validationResult);
	}

	private GoPluginApiResponse handleTaskExecution(GoPluginApiRequest request) {
		ApacheBrooklynTaskExecutor executor = new ApacheBrooklynTaskExecutor();
		Map executionRequest = (Map) new GsonBuilder().create().fromJson(request.requestBody(), Object.class);
		Map config = (Map) executionRequest.get("config");
		Map context = (Map) executionRequest.get("context");

		Result result = executor.execute(new Config(config), new Context(context), JobConsoleLogger.getConsoleLogger());
		return createResponse(result.responseCode(), result.toMap());
	}

	private GoPluginApiResponse createResponse(int responseCode, Map body) {
		final DefaultGoPluginApiResponse response = new DefaultGoPluginApiResponse(responseCode);
		response.setResponseBody(new com.google.gson.GsonBuilder().serializeNulls().create().toJson(body));
		return response;
	}


	@Override
	public GoPluginIdentifier pluginIdentifier() {
		// TODO Auto-generated method stub
		return new GoPluginIdentifier("task", Arrays.asList("1.0"));
	}

	private boolean isNumeric(String numString){
		try{
			Integer.parseInt(numString);
			return true;
		}catch(NumberFormatException e){
			return false;
		}
	}

}
