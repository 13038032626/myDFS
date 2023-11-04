package com.example.mydfs_storage.copy;

import com.example.mydfs_storage.threadPool.fileCopyExecutor;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
public class autoCopy {

    @Value("son1.ip")
    static String son1IP;
    @Value("son1.port")
    static Integer son1Port;

    @Value("son2.ip")
    static String son2IP;
    @Value("son2.port")
    static Integer son2Port;

    @Autowired
    fileCopyExecutor fileCopyExecutor;

    public ConcurrentLinkedQueue<MultipartFile> queue = new ConcurrentLinkedQueue<>();  //queue保存二元信息，value是文件的数据类型MIME

    CloseableHttpClient httpClient = HttpClients.createDefault();

    @Async
    public synchronized boolean copy() throws IOException {
        //策略是每上传一个文件就进行一次copy，必然异步，并且copy过程怎么能完全脱离上传过程且保证多个线程的文件都会执行
        //光异步不行吗？还是都加到一个队列中，再进行多线程复制？
        //担心异步并发会和上传并发有冲突，某个线程的异步复制可能会影响某个线程的上传，所以决定上传完毕后统一将文件复制

        //那么是发一个大的http还是发一些小的http？小的http配合线程池可能会挺快吧
        HttpPost post1 = new HttpPost("http:\\/\\/"+son1IP+":"+son1IP+"/copy");
        HttpPost post2 = new HttpPost("http:\\/\\/"+son2IP+":"+son2IP+"/copy");
        for (MultipartFile file: queue) {
            fileCopyExecutor.execute(()->{
                try {
                    post1.setHeader("Content-Type",file.getContentType());
                    post2.setHeader("Content-Type",file.getContentType());
                    //TODO 发起 HTTP 复制文件有点问题
                    post1.setEntity((HttpEntity) file);
                    post2.setEntity((HttpEntity) file);
                    CloseableHttpResponse response1 = httpClient.execute(post1);
                    CloseableHttpResponse response2 = httpClient.execute(post2);
                    System.out.println(response1);
                    System.out.println(response2);
                    response1.close();
                    response2.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            });
        }
        queue.clear();
        httpClient.close();
        return true;
    }
}
