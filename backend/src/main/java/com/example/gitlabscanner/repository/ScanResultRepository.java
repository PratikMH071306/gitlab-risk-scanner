package com.example.gitlabscanner.repository;

import java.util.List;

import com.example.gitlabscanner.entity.ScanResult;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ScanResultRepository extends JpaRepository<ScanResult, Long> {

	List<ScanResult> findByScanIdOrderByIdAsc(Long scanId);

}
