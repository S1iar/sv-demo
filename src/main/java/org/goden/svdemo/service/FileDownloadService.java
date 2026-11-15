package org.goden.svdemo.service;

import java.io.IOException;
import java.io.OutputStream;

public interface FileDownloadService {

    /**
     * 导出 Quality 数据为 Excel，并按 urlPath 抓取原始 PNG 图片，无损打包为 zip 写入输出流
     */
    void exportQualityZip(OutputStream outputStream) throws IOException;
}