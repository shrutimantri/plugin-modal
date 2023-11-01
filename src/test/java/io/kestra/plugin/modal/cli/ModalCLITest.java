package io.kestra.plugin.modal.cli;

import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.modal.cli.ModalCLI;
import io.kestra.plugin.scripts.exec.scripts.models.DockerOptions;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;

import jakarta.inject.Inject;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@MicronautTest
class ModalCLITest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    @SuppressWarnings("unchecked")
    void run() throws Exception {
        String environmentKey = "MY_KEY";
        String environmentValue = "MY_VALUE";

        ModalCLI.ModalCLIBuilder<?, ?> terraformBuilder = ModalCLI.builder()
            .id(IdUtils.create())
            .type(ModalCLI.class.getName())
            .docker(DockerOptions.builder().image("ghcr.io/kestra-io/modal").entryPoint(Collections.emptyList()).build())
            .commands(List.of("modal --version"));

        ModalCLI runner = terraformBuilder.build();

        RunContext runContext = TestsUtils.mockRunContext(runContextFactory, runner, Map.of("environmentKey", environmentKey, "environmentValue", environmentValue));

        ScriptOutput scriptOutput = runner.run(runContext);
        assertThat(scriptOutput.getExitCode(), is(0));

        runner = terraformBuilder
            .env(Map.of("{{ inputs.environmentKey }}", "{{ inputs.environmentValue }}"))
            .commands(List.of(
                "echo \"::{\\\"outputs\\\":{" +
                    "\\\"customEnv\\\":\\\"$" + environmentKey + "\\\"" +
                    "}}::\"",
                "modal --version | tr -d ' \n' | xargs -0 -I {} echo '::{\"outputs\":{}}::'"
                             ))
            .build();

        scriptOutput = runner.run(runContext);
        assertThat(scriptOutput.getExitCode(), is(0));
        assertThat(scriptOutput.getVars().get("customEnv"), is(environmentValue));
    }
}
