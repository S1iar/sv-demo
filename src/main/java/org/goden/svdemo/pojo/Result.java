package org.goden.svdemo.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Result<T> implements Serializable {
    private Integer code;
    private String message;
    private T data;

    public static <E> Result<E> success(E data){
        return new Result<>(200, "操作成功", data);
    }

    public static Result success(){
        return new Result(200, "操作成功", null);
    }

    public static Result error(String message){
        return new Result(1, message, null);
    }
}
