package org.goden.svdemo.service;

import java.util.List;

public interface PageService {
    List<String> list();

    List<String> pageList(Integer pageNum, Integer pageSize);
}
