package com.ouroboros.data.orchestration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vavr.control.Try;
import com.ouroboros.data.orchestration.step.QueryStep;
import com.ouroboros.data.record.RecordList;
/**
 * 顺序执行计划
 * <p>
 * 按顺序执行 QueryStep 列表，任何一步失败则整体失败（快速失败）。
 * </p>
 */
public record SequentialPlan(List<QueryStep> steps, String finalStepName) implements QueryPlan {
    private static final Logger logger = LoggerFactory.getLogger(SequentialPlan.class);
    public SequentialPlan(List<QueryStep> steps, String finalStepName) {
        this.steps = new ArrayList<>(steps);
        this.finalStepName = finalStepName;
    }
    @Override
    public Try<RecordList> execute(OrchestrationContext context) {
        return Try.of(() -> {
            logger.debug("Starting sequential plan execution with {} steps", steps.size());
            for (QueryStep step : steps) {
                logger.debug("Executing step: {}", step.getName());
                try {
                    step.execute(context);
                } catch (Exception e) {
                    logger.error("Step execution failed: {}", step.getName(), e);
                    throw new OrchestrationException(
                        "Step execution failed: " + step.getName(), e);
                }
            }
            if (!context.hasResult(finalStepName)) {
                throw new OrchestrationException(
                    "Final step result not found: " + finalStepName);
            }
            RecordList result = context.getResult(finalStepName);
            logger.debug("Sequential plan execution completed successfully");
            return result;
        });
    }
    @Override
    public List<QueryStep> steps() {
        return Collections.unmodifiableList(steps);
    }
}
