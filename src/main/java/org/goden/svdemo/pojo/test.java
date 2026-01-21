package org.goden.svdemo.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "yml-test")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class test {
    public String number;

    public String name;

    public List<String> hobbies;

    public List<Map<String,Object>> peoples;
}
