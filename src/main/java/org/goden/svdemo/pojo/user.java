package org.goden.svdemo.pojo;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class user {
    private Integer id;
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String userPic;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
