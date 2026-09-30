package com.example.gitlabscanner.dto;

import com.example.gitlabscanner.entity.ScanResult;

public record ScanIssueResponse(
		String projectName,
		String projectUrl,
		String issueType,
		String issueDescription,
		String severity,
		String filePath) {

	public static ScanIssueResponse from(ScanResult result) {
		return new ScanIssueResponse(
				result.getProjectName(),
				result.getProjectUrl(),
				result.getIssueType(),
				result.getIssueDescription(),
				result.getSeverity(),
				result.getFilePath());
	}

}
