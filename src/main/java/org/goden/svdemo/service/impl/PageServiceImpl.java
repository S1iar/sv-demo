package org.goden.svdemo.service.impl;

import org.goden.svdemo.service.PageService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class PageServiceImpl implements PageService {
    @Override
    public List<String> list() {
        return new ArrayList<>();
    }
}
