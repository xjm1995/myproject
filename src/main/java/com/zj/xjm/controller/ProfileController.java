package com.zj.xjm.controller;

import com.zj.xjm.dto.QuestionDTO;
import com.zj.xjm.pojo.User;
import com.zj.xjm.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
public class MyQuestionsController {

    @Autowired
    private QuestionService questionService;

    @GetMapping("/profile/{action}")
    String getMyquestion(@PathVariable(name="action") String action,
                         Model model,
                         HttpServletRequest request){
        User user = (User) request.getSession().getAttribute("user");
        if (user == null) {
            return "redirect:/";
        }

        if("questions".equals(action)){
            model.addAttribute("section","questions");
            model.addAttribute("sectionName","我的提问");
        }else{
            model.addAttribute("section","replies");
            model.addAttribute("sectionName","我的回复");
        }


        List<QuestionDTO> questionDTOList = questionService.questionList(user.getId());
        model.addAttribute("questions",questionDTOList);


        return "myQuestions";
    }
    //第二次提交

    //第三次提交
}
