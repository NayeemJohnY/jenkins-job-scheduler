package scripts;

import org.testng.annotations.Test;

import base.JenkinsBase;

public class JenkinsJobRunner {

	/**
	 * Main method to start the script to execute the Jenkins job specified in the sheet.
	 *
	 * @throws InterruptedException
	 */
	@Test
	public void jenkinsJobBuilder() throws InterruptedException {
		JenkinsBase jenkinsBase = new JenkinsBase("Job_Build_Details.xlsx");
		jenkinsBase.buildJenkinsJob();
	}

}
