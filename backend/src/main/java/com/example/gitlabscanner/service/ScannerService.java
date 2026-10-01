package com.example.gitlabscanner.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import com.example.gitlabscanner.dto.GitLabProjectResponse;
import com.example.gitlabscanner.dto.ScanHistoryItem;
import com.example.gitlabscanner.dto.ScanIssueResponse;
import com.example.gitlabscanner.dto.ScanRequest;
import com.example.gitlabscanner.dto.ScanResponse;
import com.example.gitlabscanner.dto.ScanType;
import com.example.gitlabscanner.dto.TreeItem;
import com.example.gitlabscanner.entity.Scan;
import com.example.gitlabscanner.entity.ScanResult;
import com.example.gitlabscanner.repository.ScanRepository;
import com.example.gitlabscanner.repository.ScanResultRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ScannerService {

	private static final Pattern SECRET_PATTERN = Pattern.compile(
			"(?i)(api[_-]?key|password|passwd|token|access[_-]?token)"
					+ "\\s*[:=]\\s*['\"][^'\"]+['\"]");

	private static final Set<String> SKIPPED_EXTENSIONS = Set.of(
			"png", "jpg", "jpeg", "gif", "webp", "ico", "pdf", "zip", "jar", "war",
			"class", "exe", "dll", "so", "woff", "woff2", "ttf", "mp4", "mp3");

	private static final int MAX_SECRET_FILES = 40;

	private static final int MAX_FILE_CHARS = 100_000;

	private final GitLabService gitLabService;
	private final ScanRepository scanRepository;
	private final ScanResultRepository scanResultRepository;

	public ScannerService(
			GitLabService gitLabService,
			ScanRepository scanRepository,
			ScanResultRepository scanResultRepository) {
		this.gitLabService = gitLabService;
		this.scanRepository = scanRepository;
		this.scanResultRepository = scanResultRepository;
	}

	@Transactional
	public ScanResponse scan(ScanRequest request) {
		if (request == null || request.type() == null || request.name() == null || request.name().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type and name are required");
		}

		String target = request.name().trim();
		List<GitLabProjectResponse> projects = request.type() == ScanType.USER
				? gitLabService.getUserProjects(target)
				: gitLabService.getGroupProjects(target);

		LocalDateTime scannedAt = LocalDateTime.now();
		Scan scan = new Scan();
		scan.setType(request.type().name());
		scan.setTargetName(target);
		scan.setTotalRepositories(projects.size());
		scan.setTotalIssues(0);
		scan.setScannedAt(scannedAt);
		scan = scanRepository.save(scan);

		List<ScanResult> stored = new ArrayList<>();
		for (GitLabProjectResponse project : projects) {
			for (ScanIssueResponse issue : scanProject(project)) {
				stored.add(toEntity(scan, project, issue, scannedAt));
			}
		}
		scanResultRepository.saveAll(stored);
		scan.setTotalIssues(stored.size());
		return new ScanResponse(scan.getId(), projects.size(), stored.size());
	}

	public List<ScanIssueResponse> scanProject(GitLabProjectResponse project) {
		List<ScanIssueResponse> results = new ArrayList<>();
		String projectName = project.pathWithNamespace() == null ? String.valueOf(project.id()) : project.pathWithNamespace();
		List<TreeItem> files = gitLabService.getRepositoryTree(project.id());
		List<TreeItem> blobs = files.stream()
				.filter(file -> file.name() != null && "blob".equals(file.type()))
				.toList();

		checkSensitiveFiles(projectName, project.webUrl(), blobs, results);
		checkMetadata(projectName, project.webUrl(), blobs, results);
		checkSecrets(project, blobs, results);
		return results;
	}

	public List<ScanHistoryItem> list() {
		return scanRepository.findAllByOrderByScannedAtDesc().stream()
				.map(ScanHistoryItem::from)
				.toList();
	}

	public List<ScanIssueResponse> results(Long scanId) {
		if (!scanRepository.existsById(scanId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Scan not found");
		}
		return scanResultRepository.findByScanIdOrderByIdAsc(scanId).stream()
				.map(ScanIssueResponse::from)
				.toList();
	}

	private void checkSensitiveFiles(
			String projectName,
			String projectUrl,
			List<TreeItem> files,
			List<ScanIssueResponse> results) {
		for (TreeItem file : files) {
			if (isSensitiveFile(file.name())) {
				results.add(issue(
						projectName,
						projectUrl,
						"Sensitive File",
						file.name() + " file found",
						"HIGH",
						file.path()));
			}
		}
	}

	private boolean isSensitiveFile(String filename) {
		String lower = filename.toLowerCase(Locale.ROOT);
		return lower.equals(".env")
				|| lower.equals("id_rsa")
				|| lower.equals("config.json")
				|| lower.equals("secrets.yml")
				|| lower.endsWith(".pem");
	}

	private void checkMetadata(
			String projectName,
			String projectUrl,
			List<TreeItem> files,
			List<ScanIssueResponse> results) {
		boolean hasReadme = files.stream()
				.anyMatch(file -> "README.md".equalsIgnoreCase(file.name()));
		boolean hasLicense = files.stream()
				.anyMatch(file -> "LICENSE".equalsIgnoreCase(file.name()));

		if (!hasReadme) {
			results.add(issue(projectName, projectUrl, "Missing Metadata", "README.md not found", "LOW", null));
		}
		if (!hasLicense) {
			results.add(issue(projectName, projectUrl, "Missing Metadata", "LICENSE not found", "LOW", null));
		}
	}

	private void checkSecrets(
			GitLabProjectResponse project,
			List<TreeItem> files,
			List<ScanIssueResponse> results) {
		String projectName = project.pathWithNamespace() == null ? String.valueOf(project.id()) : project.pathWithNamespace();
		int inspected = 0;
		for (TreeItem file : files) {
			if (inspected >= MAX_SECRET_FILES || !shouldScanContent(file)) {
				continue;
			}
			inspected++;
			String content = gitLabService.getFileContent(project.id(), file.path(), project.defaultBranch());
			if (content == null || content.isBlank()) {
				continue;
			}
			if (content.length() > MAX_FILE_CHARS) {
				content = content.substring(0, MAX_FILE_CHARS);
			}
			if (SECRET_PATTERN.matcher(content).find()) {
				results.add(issue(
						projectName,
						project.webUrl(),
						"Exposed Secret",
						"Possible secret matched in file",
						"Medium",
						file.path()));
			}
		}
	}

	private boolean shouldScanContent(TreeItem file) {
		if (file.path() == null) {
			return false;
		}
		int dot = file.name().lastIndexOf('.');
		if (dot > 0 && dot < file.name().length() - 1) {
			String extension = file.name().substring(dot + 1).toLowerCase(Locale.ROOT);
			return !SKIPPED_EXTENSIONS.contains(extension);
		}
		return true;
	}

	private ScanIssueResponse issue(
			String projectName,
			String projectUrl,
			String issueType,
			String description,
			String severity,
			String filePath) {
		return new ScanIssueResponse(projectName, projectUrl, issueType, description, severity, filePath);
	}

	private ScanResult toEntity(Scan scan, GitLabProjectResponse project, ScanIssueResponse issue, LocalDateTime scannedAt) {
		ScanResult result = new ScanResult();
		result.setScan(scan);
		result.setProjectName(issue.projectName());
		result.setProjectUrl(issue.projectUrl() != null ? issue.projectUrl() : project.webUrl());
		result.setIssueType(issue.issueType());
		result.setIssueDescription(issue.issueDescription());
		result.setSeverity(issue.severity());
		result.setFilePath(issue.filePath());
		result.setScannedAt(scannedAt);
		return result;
	}

}
