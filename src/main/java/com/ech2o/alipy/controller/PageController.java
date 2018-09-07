package com.ech2o.alipy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * Created by KAI on 2018/9/6.
 * ectest@foxmail.com
 */
@Controller
public class PageController  {

    @RequestMapping("/console")
    public ModelAndView main()  {
        return new ModelAndView("console");
    }


}
