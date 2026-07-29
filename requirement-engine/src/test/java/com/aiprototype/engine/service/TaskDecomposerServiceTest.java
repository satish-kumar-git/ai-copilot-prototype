package com.aiprototype.engine.service;

import com.aiprototype.core.domain.EngineeringTask;
import com.aiprototype.core.domain.Priority;
import com.aiprototype.core.domain.ScenarioType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class TaskDecomposerServiceTest {

    private final TaskDecomposerService decomposer = new TaskDecomposerService();

    private static final String SAMPLE = "Build a user authentication service";

    @Test
    void decompose_greenfield_returnsT1PlusTenTasks() {
        List<EngineeringTask> tasks = decomposer.decompose(SAMPLE, ScenarioType.GREENFIELD);

        assertThat(tasks).hasSize(11);
        assertThat(tasks.get(0).getId()).isEqualTo("T1");
        assertThat(tasks).anyMatch(t -> t.getTitle().contains("Architecture"));
    }

    @Test
    void decompose_brownfield_returnsT1PlusSixTasks() {
        List<EngineeringTask> tasks = decomposer.decompose(SAMPLE, ScenarioType.BROWNFIELD);

        assertThat(tasks).hasSize(7);
        assertThat(tasks.get(0).getId()).isEqualTo("T1");
    }

    @Test
    void decompose_ambiguous_returnsT1PlusFiveTasks() {
        List<EngineeringTask> tasks = decomposer.decompose(SAMPLE, ScenarioType.AMBIGUOUS);

        assertThat(tasks).hasSize(6);
        assertThat(tasks.get(0).getId()).isEqualTo("T1");
    }

    @Test
    void decompose_greenfield_allHighPriorityTasksFirst() {
        List<EngineeringTask> tasks = decomposer.decompose(SAMPLE, ScenarioType.GREENFIELD);

        // The first four tasks (T1–T4) form the foundation and are all HIGH priority
        assertThat(tasks.subList(0, 4))
                .extracting(EngineeringTask::getPriority)
                .containsOnly(Priority.HIGH);

        // Overall distribution: 8 HIGH and 3 MEDIUM across 11 tasks
        assertThat(tasks).filteredOn(t -> t.getPriority() == Priority.HIGH).hasSize(8);
        assertThat(tasks).filteredOn(t -> t.getPriority() == Priority.MEDIUM).hasSize(3);
    }

    @Test
    void decompose_greenfield_dependenciesAreValid() {
        List<EngineeringTask> tasks = decomposer.decompose(SAMPLE, ScenarioType.GREENFIELD);

        for (int i = 0; i < tasks.size(); i++) {
            EngineeringTask task = tasks.get(i);
            Set<String> priorIds = tasks.subList(0, i).stream()
                    .map(EngineeringTask::getId)
                    .collect(Collectors.toSet());

            for (String dep : task.getDependencies()) {
                assertThat(priorIds)
                        .as("Task %s depends on %s which does not appear earlier in the list",
                                task.getId(), dep)
                        .contains(dep);
            }
        }
    }

    @ParameterizedTest
    @EnumSource(ScenarioType.class)
    void decompose_allTasks_haveNonNullAiPromptHint(ScenarioType type) {
        List<EngineeringTask> tasks = decomposer.decompose(SAMPLE, type);

        assertThat(tasks).allSatisfy(task ->
                assertThat(task.getAiPromptHint())
                        .as("Task %s must have a non-blank aiPromptHint", task.getId())
                        .isNotNull()
                        .isNotBlank()
        );
    }
}
