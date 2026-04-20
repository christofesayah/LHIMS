package com.lhims.api.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping(value = { "/", "/{path:[^\\.]*}", "/{path1}/{path2:[^\\.]*}", "/{path1}/{path2}/{path3:[^\\.]*}" })
    public String forward() {
        return "forward:/dist/index.html";
    }
}
