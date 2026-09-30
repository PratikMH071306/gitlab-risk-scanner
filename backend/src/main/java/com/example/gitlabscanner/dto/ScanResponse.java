package com.example.gitlabscanner.dto;

public record ScanResponse(Long scanId, int totalRepositories, int totalIssues) {
}
