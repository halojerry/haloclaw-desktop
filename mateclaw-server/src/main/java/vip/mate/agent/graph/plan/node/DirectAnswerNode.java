package vip.mate.agent.graph.plan.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import vip.mate.agent.graph.plan.state.PlanStateKeys;

import java.util.Map;

/**
 * 直接回答节点
 * <p>
 * 当 PlanGenerationNode 判定用户消息是简单问答时，
 * 将 direct_answer 透传为 final_summary，直接结束图执行。
 *
 * @author MateClaw Team
 */
public class DirectAnswerNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) {
        String directAnswer = state.value(PlanStateKeys.DIRECT_ANSWER, "");
        return Map.of(PlanStateKeys.FINAL_SUMMARY, directAnswer);
    }
}
