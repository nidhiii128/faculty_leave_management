package com.flms.repository;

import com.flms.model.LeaveRequest;
import com.flms.model.LeaveStatus;
import com.flms.model.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByFacultyUsernameOrderByStartDateDesc(String facultyUsername);

    List<LeaveRequest> findAllByOrderByStartDateDesc();

    /**
     * Combined search. Any null argument is ignored, so one query serves both
     * the faculty view (owner set) and the HOD view (owner null).
     */
    @Query("""
        select l from LeaveRequest l
        where (:owner is null or l.facultyUsername = :owner)
          and (:status is null or l.status = :status)
          and (:type is null or l.leaveType = :type)
          and (:keyword is null
               or lower(l.facultyName) like lower(concat('%', :keyword, '%'))
               or lower(l.reason) like lower(concat('%', :keyword, '%'))
               or lower(l.department) like lower(concat('%', :keyword, '%')))
        order by l.startDate desc
        """)
    List<LeaveRequest> search(@Param("owner") String owner,
                              @Param("keyword") String keyword,
                              @Param("status") LeaveStatus status,
                              @Param("type") LeaveType type);

    /** Requests that overlap [from, to] in one of the given statuses (calendar view). */
    @Query("""
        select l from LeaveRequest l
        where l.startDate <= :to and l.endDate >= :from and l.status in :statuses
        order by l.startDate, l.facultyName
        """)
    List<LeaveRequest> findOverlapping(@Param("from") LocalDate from,
                                       @Param("to") LocalDate to,
                                       @Param("statuses") Collection<LeaveStatus> statuses);

    /** Requests where this user is the arranged substitute and the leave has not ended yet. */
    List<LeaveRequest> findBySubstituteUsernameAndStatusInAndEndDateGreaterThanEqualOrderByStartDate(
        String substituteUsername, Collection<LeaveStatus> statuses, LocalDate endDate);
}
