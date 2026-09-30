package com.example.gitlabscanner.dto;

import java.time.LocalDateTime;

import com.example.gitlabscanner.entity.Scan;

public record ScanHistoryItem(
		Long scanId,
		String type,
		String name,
		int totalRepositories,
		int totalIssues,
		LocalDateTime scannedAt) {

	public static ScanHistoryItem from(Scan scan) {
		return new ScanHistoryItem(
				scan.getId(),
				scan.getType(),
				scan.getTargetName(),
				scan.getTotalRepositories(),
				scan.getTotalIssues(),
				scan.getScannedAt());
	}

}
