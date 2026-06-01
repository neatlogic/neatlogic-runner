package com.neatlogic.autoexecrunner.web;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/error")
public class CustomErrorController implements ErrorController {

    @RequestMapping("/")
    public String handleError(HttpServletRequest request) {
        // Logic to handle the error and return a customized error page or response
        return "error";
    }


}