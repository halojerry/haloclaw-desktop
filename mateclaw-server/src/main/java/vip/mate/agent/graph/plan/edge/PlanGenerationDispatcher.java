package vip.mate.agent.graph.plan.edge;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.EdgeAction;
import vip.mate.agent.graph.plan.state.PlanStateKeys;

/**
 * 计划生成后的路由分发器
 * <p>
 * 根据 needs_planning 判断：
 * <ul>
 *   <li>false → 路由到 DIRECT_ANSWER_NODE（简单问答快速退出）</li>
 *   <li>true → 路由到 STEP_EXECUTION_NODE（开始步骤执行）</li>
 * </ul>
 *
 * @author MateClaw Team
 */
public class PlanGenerationDispatcher implements EdgeAction {

    @Override
    public String apply(OverAllState state) {
        boolean needsPlanning = state.value(PlanStateKeys.NEEDS_PLANNING, true);
        if (!needsPlanning) {
            return PlanStateKeys.DIRECT_ANSWER_NODE;
        }
        return PlanStateKeys.STEP_EXECUTION_NODE;
    }
}
