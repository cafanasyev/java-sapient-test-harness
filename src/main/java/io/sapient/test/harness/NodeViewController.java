package io.sapient.test.harness;

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
        model.addAttribute("nodes", registry.getNodes().stream().map(n -> (EdgeNode) n).toList());
        model.addAttribute("autoReloadOnManualSend", registry.isAutoReloadOnManualSend());
        return "nodes";
    }

    @GetMapping("/nodes/rows")
    String rows(Model model) {
        model.addAttribute("nodes", registry.getNodes().stream().map(n -> (EdgeNode) n).toList());
        return "nodes :: rows";
    }
}
