package com.salahtech.BarberShop_Apis.repositories;

import com.salahtech.BarberShop_Apis.models.EmployeeTimeOff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EmployeeTimeOffRepository extends JpaRepository<EmployeeTimeOff, Long> {
    List<EmployeeTimeOff> findByEmployee_IdOrderByStartDateAsc(Long employeeId);

    /** Vrai si l'employé a un congé couvrant la date donnée (start <= date <= end). */
    boolean existsByEmployee_IdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long employeeId, LocalDate dateForStart, LocalDate dateForEnd);
}
