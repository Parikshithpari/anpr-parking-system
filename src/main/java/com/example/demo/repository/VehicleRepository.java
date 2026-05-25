package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entity.User;
import com.example.demo.entity.VehicleLog;

@Repository
public interface VehicleRepository extends JpaRepository<VehicleLog, Long>
{
	List<VehicleLog> findByBranchId(Long branchId);

	Optional<VehicleLog> findByPlateNumberAndInsideTrue(String plateNumber);

	Optional<VehicleLog> findLatestByPlateNumberAndBranchId(String plateNumber, Long branchId);

	boolean existsByPlateNumberAndInsideTrue(String plate);
	
	List<VehicleLog> findByPlateNumberAndInsideFalse(String plateNumber);

	List<VehicleLog> findByPlateNumberAndInsideFalseOrderByExitTimeDesc(String plateNumber);
}
