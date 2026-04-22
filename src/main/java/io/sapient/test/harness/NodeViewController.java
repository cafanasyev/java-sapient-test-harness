package io.sapient.test.harness;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
class NodeViewController {

    private final EdgeNodeRegistry registry;

    @GetMapping("/")
    String page(Model model) {
        model.addAttribute("rows", rows());
        model.addAttribute("autoReloadOnManualSend", registry.isAutoReloadOnManualSend());
        return "nodes";
    }

    @GetMapping("/nodes/rows")
    String rows(Model model) {
        model.addAttribute("rows", rows());
        return "nodes :: rows";
    }

    private List<NodeOrdering.Row> rows() {
        return NodeOrdering.reorder(registry.getNodes().stream().map(n -> (EdgeNode) n).toList());
    }
}
