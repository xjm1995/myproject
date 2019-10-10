package com.zj.xjm.controller;

import com.zj.xjm.dto.AccessTokenDTO;
import com.zj.xjm.dto.GithubUser;
import com.zj.xjm.mapper.UserMapper;
import com.zj.xjm.pojo.User;
import com.zj.xjm.provider.GithubProvider;
import org.apache.coyote.http11.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

@Controller
public class AuthorizeController {
    @Autowired
    private GithubProvider githubProvider;

    @Value("${github.Client.id}")
    private String Clientid;
    @Value("${github.Client.secret}")
    private String Clientsecret;
    @Value("${github.Redirect.uri}")
    private String Redirecturi;

    @Autowired
    private UserMapper userMapper;



    @GetMapping("/callback")
    public String callback(@RequestParam(name = "code") String code,
                           @RequestParam(name = "state") String state,
                           HttpServletRequest request){
        AccessTokenDTO accessTokenDTO = new AccessTokenDTO();
        accessTokenDTO.setClient_id(Clientid);
        accessTokenDTO.setClient_secret(Clientsecret);
        accessTokenDTO.setCode(code);
        accessTokenDTO.setRedirect_uri(Redirecturi);
        accessTokenDTO.setState(state);
        String accessToken = githubProvider.getAccessToken(accessTokenDTO);
        GithubUser GithubUser = githubProvider.getuser(accessToken);
System.out.println(GithubUser.getLogin());
        if(GithubUser!=null){
            //登入成功
            request.getSession().setAttribute("user",GithubUser);
            User user= new User();
            user.setToken(UUID.randomUUID().toString());
            user.setName(GithubUser.getLogin());
            user.setGmtcreate(System.currentTimeMillis());
            user.setAccountId(String.valueOf(GithubUser.getId()));
            user.setGmtmodified(user.getGmtcreate());
            userMapper.save(user);


            return "redirect:/";
        }else{
            //登入失败
            return "redirect:/";
        }
    }

}
