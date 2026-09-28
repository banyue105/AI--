package com.ican.assistant.modules.practicereview;

import static com.ican.assistant.modules.practicereview.PracticeDtos.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.ican.assistant.core.ai.MockPracticeAiProvider;
import com.ican.assistant.core.ai.PracticeAiGateway;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:practiceconcurrency;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1")
class PracticeConcurrencyTest {
    @Autowired PracticeService service;
    @Autowired PracticeReviewRepository reviews;
    @MockitoBean PracticeAiGateway ai;
    private final MockPracticeAiProvider mock = new MockPracticeAiProvider();

    private Analysis analysis(Project project) { return new Analysis(mock.review(project), "mock", "测试检查器"); }
    private Project create() {
        var project = service.create(new CreateRequest("并发验收", "部署网络服务", PracticeTemplates.NETWORK));
        return service.saveEvidence(project.id(), null, new EvidenceRequest(project.inputRevision(), "访问日志", "log",
                "HTTP/1.1 200 OK\nrequest_id=original", List.of()));
    }
    private Project edit(Project project) {
        return service.saveEvidence(project.id(), project.evidence().getFirst().id(), new EvidenceRequest(project.inputRevision(),
                "新日志", "log", "HTTP/1.1 503 Service Unavailable", List.of()));
    }
    @BeforeEach void provider() { doAnswer(call -> analysis(call.getArgument(0))).when(ai).review(any()); }

    @Test void modelWaitDoesNotLockEditsAndLateResultCannotBeConfirmedAsCurrent() throws Exception {
        Project project = create(); var request = new ReviewRequest(project.inputRevision(), UUID.randomUUID().toString());
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        doAnswer(call -> {
            entered.countDown();
            if (!release.await(8, TimeUnit.SECONDS)) throw new IllegalStateException("模型测试未释放");
            return analysis(call.getArgument(0));
        }).when(ai).review(any());
        try (var pool = Executors.newFixedThreadPool(2)) {
            var checking = pool.submit(() -> service.review(project.id(), request));
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            try {
                assertThatThrownBy(() -> service.review(project.id(), request)).isInstanceOfSatisfying(PracticeException.class,
                        error -> assertThat(error.code()).isEqualTo("REQUEST_IN_PROGRESS"));
                Project edited = pool.submit(() -> edit(project)).get(3, TimeUnit.SECONDS);
                release.countDown();
                Review old = checking.get(5, TimeUnit.SECONDS);
                assertThat(old.isStale()).isTrue();
                assertThat(old.input().evidence().getFirst().content()).contains("request_id=original");
                assertThatThrownBy(() -> service.confirm(project.id(), old.id(), new ConfirmRequest(edited.inputRevision(), List.of())))
                        .isInstanceOfSatisfying(PracticeException.class, error -> assertThat(error.code()).isEqualTo("REVIEW_STALE"));
                assertThat(service.review(project.id(), request).id()).isEqualTo(old.id());
                assertThat(service.history(project.id())).hasSize(1);
                verify(ai, times(1)).review(any());
            } finally { release.countDown(); }
        }
    }

    @Test void failedAndAbandonedRequestsRetryTheirOriginalSnapshotWithoutNewVersions() {
        Project project = create(); var request = new ReviewRequest(project.inputRevision(), UUID.randomUUID().toString());
        doThrow(new IllegalStateException("测试中断")).when(ai).review(any());
        assertThatThrownBy(() -> service.review(project.id(), request)).isInstanceOfSatisfying(PracticeException.class,
                error -> assertThat(error.code()).isEqualTo("REVIEW_FAILED"));
        assertThat(service.history(project.id())).isEmpty();
        var reservation = reviews.findByRequestId(request.requestId()).orElseThrow();
        String reservedId = reservation.id;
        // Simulate a process that stopped after reserving work, before storing a response.
        reservation.executionStatus = "running"; reservation.startedAt = Instant.now().minusSeconds(60);
        reviews.saveAndFlush(reservation);
        Project edited = edit(project);
        doAnswer(call -> analysis(call.getArgument(0))).when(ai).review(any());
        Review retried = service.review(project.id(), request);
        assertThat(retried.id()).isEqualTo(reservedId); assertThat(retried.number()).isEqualTo(1);
        assertThat(retried.inputRevision()).isEqualTo(project.inputRevision()); assertThat(retried.isStale()).isTrue();
        assertThat(retried.input().evidence().getFirst().content()).contains("request_id=original");
        var current = service.review(project.id(), new ReviewRequest(edited.inputRevision(), UUID.randomUUID().toString()));
        assertThat(current.number()).isEqualTo(2); assertThat(current.isStale()).isFalse();
        assertThat(service.history(project.id())).hasSize(2);
        assertThatThrownBy(() -> service.review(project.id(), new ReviewRequest(edited.inputRevision(), request.requestId())))
                .isInstanceOfSatisfying(PracticeException.class, error -> assertThat(error.code()).isEqualTo("IDEMPOTENCY_CONFLICT"));
    }

    @Test void concurrentFeedbackRetriesReturnOnePersistedBatch() throws Exception {
        Project project = create(); Review reviewed = service.review(project.id(), new ReviewRequest(project.inputRevision(), UUID.randomUUID().toString()));
        service.confirm(project.id(), reviewed.id(), new ConfirmRequest(project.inputRevision(), List.of()));
        var request = new FeedbackRequest(reviewed.id(), UUID.randomUUID().toString(), List.of(
                new FeedbackItem("ability", null, "网络服务部署", "能力证据", "保存参考", List.of(), List.of()),
                new FeedbackItem("decision", null, null, "投入参考", "保存参考", List.of(), List.of())));
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> service.saveFeedback(project.id(), request));
            var second = pool.submit(() -> service.saveFeedback(project.id(), request));
            assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo(second.get(5, TimeUnit.SECONDS));
        }
        assertThat(service.feedback(project.id())).hasSize(2);
    }
}
