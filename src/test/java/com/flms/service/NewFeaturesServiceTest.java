package com.flms.service;

import com.flms.model.LeaveRequest;
import com.flms.model.LeaveStatus;
import com.flms.model.LeaveType;
import com.flms.repository.LeaveRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import static com.flms.service.LeaveServiceTest.FACULTY1;
import static com.flms.service.LeaveServiceTest.FACULTY2;
import static com.flms.service.LeaveServiceTest.HOD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Substitute arrangement, supporting documents and the leave calendar. */
@DataJpaTest(properties = "app.seed-data=false")
@Import({LeaveService.class, LeaveReportService.class, AttachmentService.class, NewFeaturesServiceTest.FixedClock.class})
class NewFeaturesServiceTest {

    static final LocalDate TODAY = LocalDate.of(2026, 11, 4);
    static final Actor FACULTY3 = new Actor("faculty3", false);
    static final byte[] PDF = "%PDF-1.4 test".getBytes();

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        }
    }

    @Autowired LeaveService leaveService;
    @Autowired LeaveReportService reports;
    @Autowired AttachmentService attachments;
    @Autowired LeaveRequestRepository repository;

    private LeaveRequest apply(Actor who, LocalDate start, LocalDate end, String substitute) {
        return leaveService.apply(who, LeaveType.CASUAL, start, end, "Personal work", substitute);
    }

    // ---- substitute ----

    @Test
    void substituteIsStoredWithDisplayName() {
        LeaveRequest l = apply(FACULTY1, TODAY.plusDays(2), TODAY.plusDays(3), "faculty3");
        assertThat(l.getSubstituteUsername()).isEqualTo("faculty3");
        assertThat(l.getSubstituteName()).isEqualTo("Prof. Neha Joshi");
    }

    @Test
    void cannotNameYourselfOrTheHodAsSubstitute() {
        assertThatThrownBy(() -> apply(FACULTY1, TODAY, TODAY, "faculty1"))
            .isInstanceOf(WorkflowException.class).hasMessageContaining("own substitute");
        assertThatThrownBy(() -> apply(FACULTY1, TODAY, TODAY, "hod"))
            .isInstanceOf(WorkflowException.class).hasMessageContaining("faculty member");
    }

    @Test
    void substituteCanViewButOtherFacultyCannot() {
        Long id = apply(FACULTY1, TODAY.plusDays(1), TODAY.plusDays(1), "faculty3").getId();
        assertThat(leaveService.get(FACULTY3, id).getFacultyUsername()).isEqualTo("faculty1");
        assertThatThrownBy(() -> leaveService.get(FACULTY2, id)).isInstanceOf(LeaveNotFoundException.class);
    }

    @Test
    void coveringListShowsUpcomingLeaveWhereIAmTheSubstitute() {
        apply(FACULTY1, TODAY.plusDays(5), TODAY.plusDays(6), "faculty3");
        apply(FACULTY2, TODAY.minusDays(9), TODAY.minusDays(8), "faculty3"); // already over
        apply(FACULTY2, TODAY.plusDays(1), TODAY.plusDays(1), null);         // no substitute

        assertThat(reports.coveringFor(FACULTY3)).extracting(LeaveRequest::getFacultyUsername).containsExactly("faculty1");
        assertThat(reports.summary(FACULTY3).covering()).hasSize(1);
        assertThat(reports.coveringFor(HOD)).isEmpty();
    }

    // ---- documents ----

    @Test
    void onlyPdfJpgPngUpToFiveMegabytesAreAccepted() {
        AttachmentService.validate("certificate.pdf", 1000);
        AttachmentService.validate("scan.JPG", 1000);
        assertThatThrownBy(() -> AttachmentService.validate("virus.exe", 10)).isInstanceOf(WorkflowException.class);
        assertThatThrownBy(() -> AttachmentService.validate("big.pdf", AttachmentService.MAX_BYTES + 1))
            .isInstanceOf(WorkflowException.class).hasMessageContaining("5 MB");
    }

    @Test
    void ownerAndHodCanOpenTheDocumentButTheSubstituteCannot() {
        Long id = apply(FACULTY1, TODAY, TODAY, "faculty3").getId();
        attachments.store(FACULTY1, id, "C:\\fakepath\\medical certificate.pdf", PDF);

        assertThat(repository.findById(id).orElseThrow().getAttachmentName()).isEqualTo("medical certificate.pdf");
        assertThat(attachments.load(FACULTY1, id)).get().extracting(a -> a.getContentType()).isEqualTo("application/pdf");
        assertThat(attachments.load(HOD, id)).isPresent();
        assertThatThrownBy(() -> attachments.load(FACULTY3, id)).isInstanceOf(LeaveNotFoundException.class);
    }

    @Test
    void onlyTheOwnerCanAttach() {
        Long id = apply(FACULTY1, TODAY, TODAY, "faculty3").getId();
        assertThatThrownBy(() -> attachments.store(FACULTY3, id, "x.pdf", PDF)).isInstanceOf(WorkflowException.class);
        assertThatThrownBy(() -> attachments.store(FACULTY2, id, "x.pdf", PDF)).isInstanceOf(LeaveNotFoundException.class);
    }

    // ---- calendar ----

    @Test
    void calendarPlacesLeaveOnEveryDayItCovers() {
        LeaveRequest l = apply(FACULTY1, LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 11), null);
        l.setStatus(LeaveStatus.APPROVED);
        repository.save(l);

        CalendarMonth cal = reports.calendar(HOD, YearMonth.of(2026, 11));
        List<CalendarMonth.Day> days = cal.weeks().stream().flatMap(List::stream).toList();

        assertThat(days.get(0).date().getDayOfWeek()).hasToString("MONDAY");
        assertThat(days).filteredOn(d -> !d.leaves().isEmpty()).extracting(CalendarMonth.Day::date)
            .containsExactly(LocalDate.of(2026, 11, 9), LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 11));
        assertThat(days).filteredOn(CalendarMonth.Day::today).extracting(CalendarMonth.Day::date).containsExactly(TODAY);
        assertThat(cal.previous()).isEqualTo(YearMonth.of(2026, 10));
    }

    @Test
    void facultySeeOwnPendingAndOthersApprovedOnly() {
        apply(FACULTY1, TODAY.plusDays(1), TODAY.plusDays(1), null);                  // faculty1 pending
        LeaveRequest approved = apply(FACULTY2, TODAY.plusDays(2), TODAY.plusDays(2), null);
        approved.setStatus(LeaveStatus.APPROVED);
        repository.save(approved);
        apply(FACULTY2, TODAY.plusDays(3), TODAY.plusDays(3), null);                  // faculty2 pending

        List<String> seenByFaculty1 = names(reports.calendar(FACULTY1, YearMonth.from(TODAY)));
        assertThat(seenByFaculty1).containsExactlyInAnyOrder("faculty1", "faculty2");
        assertThat(names(reports.calendar(HOD, YearMonth.from(TODAY)))).hasSize(3);
    }

    private static List<String> names(CalendarMonth cal) {
        return cal.weeks().stream().flatMap(List::stream).flatMap(d -> d.leaves().stream())
            .map(LeaveRequest::getFacultyUsername).toList();
    }
}
