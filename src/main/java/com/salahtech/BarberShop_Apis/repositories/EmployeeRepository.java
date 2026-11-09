package com.salahtech.BarberShop_Apis.repositories;

import com.salahtech.BarberShop_Apis.Enums.EmployeeRole;
import com.salahtech.BarberShop_Apis.models.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByUser_Id(Long userId);

    Optional<Employee> findByBarber_Id(Long barberId);

    @Query("SELECT e FROM Employee e JOIN e.salons s WHERE s.id = :salonId")
    List<Employee> findAllBySalonId(@Param("salonId") Long salonId);

    @Query("SELECT e FROM Employee e JOIN e.salons s WHERE s.id = :salonId AND e.isActive = true")
    List<Employee> findActiveBySalonId(@Param("salonId") Long salonId);

    @Query("SELECT e FROM Employee e JOIN e.salons s WHERE s.id = :salonId AND e.role = :role AND e.isActive = true")
    List<Employee> findBySalonIdAndRole(@Param("salonId") Long salonId, @Param("role") EmployeeRole role);

    @Query("SELECT e FROM Employee e JOIN e.salons s WHERE s.id = :salonId AND e.isAvailable = true AND e.isActive = true")
    List<Employee> findAvailableBySalonId(@Param("salonId") Long salonId);

    boolean existsByUser_IdAndSalons_Id(Long userId, Long salonId);
}