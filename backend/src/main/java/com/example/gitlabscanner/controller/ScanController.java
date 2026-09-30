package com.example.gitlabscanner.controller;

import java.util.List;

import com.example.gitlabscanner.dto.ScanHistoryItem;
import com.example.gitlabscanner.dto.ScanIssueResponse;
import com.example.gitlabscanner.dto.ScanRequest;
import com.example.gitlabscanner.dto.ScanResponse;
import com.example.gitlabscanner.service.ScannerService;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/scans")
public class ScanController {

	private final ScannerService scannerService;

	public ScanController(ScannerService scannerService) {
		this.scannerService = scannerService;
	}

	@PostMapping
	public ScanResponse create(@RequestBody ScanRequest request) {
		return scannerService.scan(request);
	}

	@GetMapping
	public List<ScanHistoryItem> list() {
		return scannerService.list();
	}

	@GetMapping("/{id}/results")
	public List<ScanIssueResponse> results(@PathVariable Long id) {
		return scannerService.results(id);
	}

}
