
# Jenkins Job Executor
This is generalized project to schedule the jobs in the Jenkins. This project can be used to schedule and trigger any type of jobs in any Jenkins.

### How it works ?

*	The project starts the execution of job builds specified in the **'Job_Build_Details.xlsx'**.
*	Create a Job URI based on the **Jenkins URL**, **Job Name** and **build Type **specified in the **Job Details** sheet.
* 	If the build type is **build**, Then the job will be build without parameters and it will be built number times specified in the 'Number of builds'
*	If the build type is **buildWithParameters**, Then the job will be build with parameters and it will be built number of times based on parameters row specified in **Build Parameters** sheet.
*	The job was built using the API end point **(https://{JOBURI}/{BuildType} + Query Parameters)**, and verify the build was created.
* 	Once the build is started, wait for 30 seconds and check for the built is completed. The build status is captured by another API end point **(https://{JOBURI}/lastBuild/api/json)**.
* 	If the build is not completed, Check the status of build in every 10 seconds interval until the build is completed.
*	Once the build is completed, the **Build Number** and **Status** will be updated in the **Result** sheet.
* 	The next build will be triggered, once the current build is completed.


### How to use ?
Project can be used from local machine or from the Jenkins.

##### From Local:

*   Clone or download the project **(https://stash.cengage.com/projects/CTAF/repos/jenkins-job-executor/browse)** to the local machine.
*   Edit the required information in the **'Job_Build_Details.xlsx'** sheet under project directory.

**Running the tests from Eclipse**

1. Go to Run configurations in eclipse
2. Right click on maven build => New configuration
3. Provide a 'Name' (eg: Jenkins Job Runner)
4. Click on 'Main' tab.
5. Provide the 'Base directory'. Usually this will be workspace. If so, click on Workspace button and select the workspace under which the project is available.
6. Add the maven goals in 'goals ' as **clean test** 
8. click on Run.

**Running the tests from command line**

1. Launch command prompt or terminal.
2. Navigate to the base project directory.
3. To run the test, specify the maven goals and maven parameters in command line as **mvn clean test**

##### From Jenkins:

*   Create a New item (Free style project) in the Jenkins and configure the Source Code Management to locate this project.
*	Check the **This project is parameterized** option and add the File parameter with File location as 'Job_Build_Details.xlsx' to upload the Jenkins job details to be run 
*   Add **Invoke top-level Maven targets** in Build Step and add a goal as **mvn test**. Finally apply the changes and save.
*	Click **Build With Parameters** Option available under the item and upload a file with Jenkins job details to be executed.


### How to specify the Job Details ?

*   Sample template file can be found in the jenkins-job-executor project folder. FileName : **'Job_Build_Details.xlsx'**.
*   The Workbook contains the 3 sheets namely **'Job Details', 'Build Parameters' and 'Result'**.

**Job Details sheet**

*	Do not change the column headers in this sheet. Only specify the Jenkins job details.
*   In **'Jenkins_URL'** column , specify the Jenkins Base URL *(e.g https://jenkins-qa-automation.cengage.info/job/CTAF/)*.
*	In **'Job_Name'** column, specify the Job to be executed *(e.g CTAF - CAS Test Framework)*.
* 	In **'Build_Type'** column, select the build type from drop down. i.e, Build with parameters or Build (WithoutParameters).
* 	In **Number of builds'**, specify the number of times the job to be built(executed). If this field is not specified then default one build will be executed.
*	In **token** column, specify the Jenkins Authentication token of the Jenkins job which is to be located.

**Note 1:** This **Number of builds** option is applicable only for the job without parameters as the job with parameters is executed based on the parameters rows specified.

**Note 2:** How to get Authentication token from Jenkins ?<br/>
	In Jenkins, Go to the project Item => Configure => Build Triggers => Select 'Trigger builds remotely (e.g., from scripts)'=> Add name to the token if not available, use the same name in the Job Details sheet.

**Note 3:** The specified job should contain at least one build (running or completed) prior to executing this script.

Screenshot of Jenkins Authentication token:

![Jenkins Authentication token](screenshots/JenkinsAuthenticationToken.png "Jenkins Authentication token") 

**Build Parameters**

*	This sheet will be used if the 'Build Type' is specified as **buildWithParameters**.
*	Add/Change/Delete the parameter header in this sheet as required for the job.(Do not delete the token column)
* 	The the job will be built number of times based on parameters rows specified in this sheet.
*	The Top Row is the header for the parameters it should have each **'Parameter Name**.
*	The next rows will have the parameter value for each parameter name .

**What if parameter type is List of values?**

*	Prefix **'Multiple : '** to the parameter name (header) whose parameter value is a list of values.
*	Specify the list of values by **comma(',')** separated. *i.e., (Value1,Value2,Value3)*

**What if parameter type is File?**

*	Prefix **'File : '** to the parameter name (header) whose parameter value is a File.
*	If the project execution is from local, Then specify the completed path of the file as parameter value.
*	If the project execution is from **Jenkins** follow the below steps:<br/>
		> Configure the Jenkins job for the file upload. i.e In configure under build is **This project is parameterized**, add **'File Parameter'** under **'Add Parameter'** as many as needed<br/>
		> Specify the FileName which is to be uploaded as File location and specify the same file name as parameter value in the sheet<br/>**(Note: Mention only the FileName, not the file path and File location in jenkins & File parameter value should match)**<br/>
		> Upload a files to the File parameters while building the the job. see Screenshots<br/>

Screenshot Of File Parameter in Jenkins:

![File Parameter in Jenkins](screenshots/FileParameterInJenkins.png "File Parameter In Jenkins")

Screenshot Of File Parameter in Excel:

![File Parameter in Excel](screenshots/FileParameterInExcel.png "File Parameter In Excel")<br/>

Screenshot Of File Parameter While Building Job:

![File Parameter in Jenkins Build](screenshots/BuildWithFileParameter.png "File Parameter In Jenkins Build")<br/>

		
**Result**

This sheet will be updated with the status of job builds. No changes required in the sheet.
*	Column 1 : **Build Number**	- Build number of last completed build.
*	Column 2 : **Status** 	-	Status of the last completed build (SUCCESS, FAILURE, ABORTED, RUNNING).

**Demo Video**
[Jenkins Job Executor Demo Video](https://jira.cengage.com/plugins/servlet/jeditor_file_provider?imgId=ckupload202012183563654556295608629&fileName=Jenkins+Job+Executor+Demo.mp4 "Jenkins Job Executor Demo Video") 
