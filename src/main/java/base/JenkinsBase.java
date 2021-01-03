package base;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import utils.ExcelUtils;

public class JenkinsBase {

	private static Logger log = LogManager.getLogger();
	private String buildType;
	private String jobURI;
	private ExcelUtils excelUtils;
	private final String STATUS = "Status";
	private final String BUILD_NUMBER = "Build Number";
	private final String RUNNING = "RUNNING";
	private String jobDetailsSheet = "Job Details";
	private String buildParametersSheet = "Build Parameters";
	private String resultSheet = "Result";
	private int lastBuildNumber;

	public JenkinsBase(String fileName) {
		excelUtils = new ExcelUtils(fileName);
		setJenkinsJobURI();
		setResultSheet();
		lastBuildNumber = getLastBuildNumber();
	}

	/**
	 * Method to set the URI for the Jenkins job
	 * The Jenkins URL, Job Name and Build type retrieved from the sheet to form the URI
	 */
	private void setJenkinsJobURI() {

		String jenkinsURL = excelUtils.getCellValue(jobDetailsSheet, "Jenkins_URL", 1);
		String jobName = excelUtils.getCellValue(jobDetailsSheet, "Job_Name", 1);
		buildType = excelUtils.getCellValue(jobDetailsSheet, "Build_Type", 1);

		String[] jenkinsJobDetails = { jenkinsURL, jobName, buildType };
		Validate.isTrue(Arrays.stream(jenkinsJobDetails).allMatch(value -> !value.isEmpty()),
				"The Job details : Jenkins_URL, Job_Name and Build_Type should not be empty");

		if (!jenkinsURL.endsWith("/")) {
			jobURI = jenkinsURL + "/";
		} else {
			jobURI = jenkinsURL;
		}

		jobURI += "job/" + jobName;

		log.info("Jenkins Job URI is : {}", jobURI);

	}

	/**
	 * Method to retrieve the build parameters from the 'Build Parameters' sheet
	 * 
	 * @return listOfMapOfBuildParameters - List<Map<String, String>>
	 */
	private List<Map<String, String>> getBuildParametersForJenkinsJob() {

		// Check at least one row of build parameters are specified
		int numberOfBuildParamters = excelUtils.getRow(buildParametersSheet, 0).getLastCellNum();
		Validate.isTrue(numberOfBuildParamters != -1, "No build parameters are defined");

		// Retrieve the parameters name
		List<String> listOfBuildParameters = new ArrayList<>();
		for (int i = 0; i < numberOfBuildParamters; i++) {
			listOfBuildParameters.add(excelUtils.getCellValue(buildParametersSheet, i, 0));
		}

		int numberOfRows = excelUtils.getRowCount(buildParametersSheet);
		log.info("Jenkins Job number of Build Parameters Rows: {}", numberOfRows -1);

		List<Map<String, String>> listOfMapOfBuildParameters = new ArrayList<>();

		// Retrieve the parameters values for the parameters name as a Map and add to the list
		for (int i = 1; i < numberOfRows; i++) {
			Map<String, String> mapOfBuildParamters = new HashMap<>();
			for (int j = 0; j < numberOfBuildParamters; j++) {
				mapOfBuildParamters.put(listOfBuildParameters.get(j),
						excelUtils.getCellValue(buildParametersSheet, j, i));
			}
			listOfMapOfBuildParameters.add(mapOfBuildParamters);
		}

		return listOfMapOfBuildParameters;
	}

	/**
	 * Method to build a Jenkins job with the parameters.
	 * The parameters are added as query parameters based on parameter type File, List and String.
	 * The job executed using the RestAssured and status code verified for 201 - Created
	 * 
	 * @param parameters - Map<String, String>
	 * @throws InterruptedException
	 */
	private void buildJenkinsJobWithParameters(Map<String, String> parameters) throws InterruptedException {
		RequestSpecBuilder builder = new RequestSpecBuilder();

		for (Entry<String, String> parameterEntry : parameters.entrySet()) {
			String parameterKey = parameterEntry.getKey();
			String parameterValue = parameterEntry.getValue();

			if (!parameterValue.isEmpty()) {
				if (parameterKey.contains("File : ")) { // File type parameter
					builder.addMultiPart(parameterKey.split("File : ")[1], new File(parameterValue));
				} else if (parameterKey.contains("Multiple : ")) { // List type parameter
					builder.addQueryParam(parameterKey.split("Multiple : ")[1], Arrays.asList(parameterValue.split(",")));
				} else {
					builder.addQueryParam(parameterKey, parameterValue);
				}
			}
		}

		// Check if already any build is running.
		// If build is running wait for the build to complete and trigger run
		log.info("Checking previous build status before triggering new build");
		if (!getMapOfBuildResult().get(STATUS).equals(RUNNING)) {
			RestAssured.given(builder.build()).get(jobURI + "/" + buildType).then().assertThat().statusCode(201);
			log.info("Jenkins build was triggered successfully");
			lastBuildNumber++;
			log.info("Waiting for Jenkins job build to be triggered");
			Thread.sleep(30000);
		}

	}

	/**
	 * Method to get the number of times the jobs to be build.
	 * (Applicable only for build without parameters)
	 * If the 'Number of builds' is not specified, then execute the build only one time
	 * 
	 * @return numberofBuilds - Integer
	 */
	private int getBuildCountForJob() {
		String numberOfBuilds = excelUtils.getCellValue(jobDetailsSheet, "Number of builds", 1);
		if (numberOfBuilds.isEmpty()) {
			log.info(
					"Number of Build was not specified for the job without parameters. Executing 1 build for the job.");
			return 1;
		}

		return Integer.parseInt(numberOfBuilds);
	}

	/**
	 * Method to build the Jenkins job without parameters
	 * @throws InterruptedException
	 * 
	 */
	private void buildJenkinsJobWithoutParameters() throws InterruptedException {
		String token = excelUtils.getCellValue(jobDetailsSheet, "token", 1);

		// Check if already any build is running.
		// If build is running wait for the build to complete and trigger run
		log.info("Checking previous build status before triggering new build");
		if (!getMapOfBuildResult().get(STATUS).equals(RUNNING)) {
			RestAssured.given().queryParam("token", token).get(jobURI + "/" + buildType).then().assertThat()
					.statusCode(201);
			log.info("Jenkins build was triggered successfully");
			lastBuildNumber++;
			log.info("Waiting for Jenkins job build to be triggered");
			Thread.sleep(30000);
		}

	}

	/**
	 * Method to retrieve the last build number
	 * The build result is captured using the API call 'JobURI + /lastBuild/api/json'
	 * The lastBuildNumber is retrieved from the value of key JSON 'number'
	 *
	 * @return lastBuildNumber - int
	 */
	private int getLastBuildNumber() {
		Response response = RestAssured.get(jobURI + "/lastBuild/api/json");

		Validate.isTrue(response.getStatusCode() == 200,
				"Expected response code is 200. But Actual response code is " + response.getStatusCode());

		JsonPath jsonPath = response.jsonPath();

		return Integer.parseInt(jsonPath.getString("number"));
	}

	/**
	 * Method to retrieve the status of build by build number
	 * The build result is captured using the API call 'JobURI + /buildNumber/api/json'
	 * If the result object in the API response is null, means that the job is still running
	 * The status is marked as RUNNING , if the result object in the API response is null
	 *
	 * @return STATUS - String
	 */
	private String getStatusOfBuild(int buildNumber) {
		Response response = RestAssured.get(jobURI + "/" + buildNumber + "/api/json");

		Validate.isTrue(response.getStatusCode() == 200,
				"Expected response code is 200. But Actual response code is " + response.getStatusCode());

		JsonPath jsonPath = response.jsonPath();

		return jsonPath.getString("result") == null ? RUNNING : jsonPath.getString("result");
	}

	/**
	 * Method to get the final build results of the job build.
	 * Waits for the job build to be completed
	 * The Jenkins build result polled for every 30 seconds
	 *
	 * @return mapOfBuildResult - Map<String, String>
	 * @throws InterruptedException
	 */
	private Map<String, String> getMapOfBuildResult() throws InterruptedException {

		Map<String, String> mapOfBuildResult = new HashMap<>();
		String status = "";

		// If the build was not triggered or status is not updated in 30 seconds
		// Then, Wait for another 30 seconds and check again
		try {
			status = getStatusOfBuild(lastBuildNumber);
		} catch (Exception e) {
			log.info("Retrying to get the status of last build {}", lastBuildNumber);
			Thread.sleep(30000);
			status = getStatusOfBuild(lastBuildNumber);
		}

		int i = 1;
		while (status.equals(RUNNING)) {
			Thread.sleep(30000);
			log.info("Waited {} seconds for the build to be completed", (30 * i++));
			status = getStatusOfBuild(lastBuildNumber);
		}

		mapOfBuildResult.put(STATUS, status);
		mapOfBuildResult.put(BUILD_NUMBER, String.valueOf(lastBuildNumber));
		log.info("The current build number - {} and status - {}", lastBuildNumber, status);

		return mapOfBuildResult;
	}

	/**
	 * Method to write a map of build result to the excel sheet.
	 *
	 * @param rownum - int
	 * @throws InterruptedException
	 */
	public void writeBuildResult(int rownum) throws InterruptedException {
		Map<String, String> mapOfBuildResult = getMapOfBuildResult();
		for (Entry<String, String> resultEntry : mapOfBuildResult.entrySet()) {
			excelUtils.setCellValue(resultSheet, resultEntry.getKey(), rownum, resultEntry.getValue());
		}
	}

	/**
	 * Method to initialize the 'Result' sheet with headers
	 */
	private void setResultSheet() {
		excelUtils.createSheet(resultSheet);
		excelUtils.setCellValue(resultSheet, 0, 0, BUILD_NUMBER);
		excelUtils.setCellValue(resultSheet, 1, 0, STATUS);
	}

	/**
	 * Method to update the workbook with the results
	 */
	private void updateWorkbookWithResult() {
		excelUtils.writeToWorkbook();
		log.info("Updated the Workbook with the Build Results. Please refer the 'Result' sheet in the workbook.");
	}

	/**
	 * Method to build the Jenkins job with or without parameters
	 * if build type is 'buildWithParameters', then read the parameters sheet and build with parameters
	 * else build without parameters
	 * After completing each build the result will be update to the excel sheet.
	 *
	 * @throws InterruptedException
	 */
	public void buildJenkinsJob() throws InterruptedException {
		if (buildType.equalsIgnoreCase("buildWithParameters")) {
			List<Map<String, String>> listOfMapOfParamerters = getBuildParametersForJenkinsJob();
			// Execute the job for number of times based on the number of parameters rows specified
			for (int i = 0; i < listOfMapOfParamerters.size(); i++) {
				Map<String, String> mapOfParameters = listOfMapOfParamerters.get(i);
				buildJenkinsJobWithParameters(mapOfParameters);
				writeBuildResult(i + 1);
			}
		} else { // For build type is 'build', then execute the job without parameters
			int numberOfBuilds = getBuildCountForJob();
			// Execute the job for number of times based on the number of builds specified
			for (int i = 0; i < numberOfBuilds; i++) {
				buildJenkinsJobWithoutParameters();
				writeBuildResult(i + 1);
			}
		}

		updateWorkbookWithResult();
	}
}
