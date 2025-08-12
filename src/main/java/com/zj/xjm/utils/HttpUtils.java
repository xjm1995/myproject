package com.zj.xjm.utils;


import com.kykj.hk.base.common.net.CustomWebClientBuilder;
import com.kykj.hk.ex.BaseException;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author ChenHao
 * @date 2024/10/17 16:19 周四
 */
@Slf4j
@UtilityClass
public class HttpUtils {
    private static final Object LOCK = new Object();
    private static volatile WebClient webClient;
    // 10MB
    private static final int BUFFER_SIZE = 10 * 1024 * 1024;
    private static void init(){
        if (webClient == null){
            synchronized (LOCK){
                if (webClient == null){
                    webClient = CustomWebClientBuilder.create()
                            .setExchangeStrategies(ExchangeStrategies.builder()
                                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(BUFFER_SIZE))
                                    .build()
                            ).build();
                }
            }
        }
    }
    public static WebClient getWebClient(){
        init();
        return webClient;
    }
    @SneakyThrows
    public static File downloadFile(String url) {
        init();
        Mono<byte[]> dataBufferMono = webClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .bodyToMono(byte[].class);
        String fileName = url.substring(url.lastIndexOf("/") + 1);
        Path path = Files.createTempFile("download", fileName);
        File file = path.toFile();
        byte[] data = dataBufferMono.block();
        if (data != null) {
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(data);
            } catch (IOException e) {
                log.error("文件写入到临时文件失败,下载链接:{}", url, e);
                throw new BaseException(e);
            }
        } else {
            log.error("文件下载失败:{}", url);
            throw new BaseException("文件下载失败");
        }
        System.out.println(file.getName());
        return file;
    }


}
