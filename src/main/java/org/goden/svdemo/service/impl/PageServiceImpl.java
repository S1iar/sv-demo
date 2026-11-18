package org.goden.svdemo.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

    @Override
    public List<String> pageList(Integer pageNum, Integer pageSize) {
        Page<String> page = Page.of(pageNum, pageSize);
        // 总数
        long total = page.getTotal();
        // 每页数量
        long size = page.getSize();
        // 当前页数量
        long current = page.getCurrent();
        // 总页数
        long pages = page.getPages();
        // 是否有下一页
        boolean hasNext = page.hasNext();
        // 是否有上一页
        boolean hasPrev = page.hasPrevious();
        // 当前页数据
        List<String> records = page.getRecords();
        return records;
    }
}
