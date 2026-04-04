package vip.mate.agent.graph.edge;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import lombok.extern.slf4j.Slf4j;
import vip.mate.agent.graph.state.MateClawStateAccessor;

import static vip.mate.agent.graph.state.MateClawStateKeys.*;

/**
 * 推理路由（4 路分支）
 * <p>
 * 根据 ReasoningNode 产出的状态决定下一步去向：
 * <ol>
 *   <li>迭代超限 → limitExceededNode（最高优先级）</li>
 *   <li>需要工具调用 → actionNode</li>
 *   <li>需要总结压缩 → summarizingNode</li>
 *   <li>可直接回答 → finalAnswerNode</li>
 * </ol>
 *
 * @author MateClaw Team
 */
@Slf4j
public class ReasoningDispatcher implements EdgeAction {

    @Override
    public String apply(OverAllState state) throws Exception {
        MateClawStateAccessor accessor = new MateClawStateAccessor(state);

        // 1. 超限检查优先
        if (accessor.isLimitReached()) {
            log.warn("[ReasoningDispatcher] Iteration limit reached ({}/{}), routing to limitExceededNode",
                    accessor.iterationCount(), accessor.maxIterations());
            return LIMIT_EXCEEDED_NODE;
        }

        // 2. 工具调用
        if (accessor.needsToolCall()) {
            log.debug("[ReasoningDispatcher] Routing to actionNode (tool call needed)");
            return ACTION_NODE;
        }

        // 3. 需要总结（上下文过长，最终回答前先压缩）
        if (accessor.shouldSummarize()) {
            log.info("[ReasoningDispatcher] Routing to summarizingNode (observation context too large)");
            return SUMMARIZING_NODE;
        }

        // 4. 直接回答
        log.debug("[ReasoningDispatcher] Routing to finalAnswerNode (direct answer)");
        return FINAL_ANSWER_NODE;
    }
}
