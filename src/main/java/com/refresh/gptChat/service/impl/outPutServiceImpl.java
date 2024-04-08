package com.refresh.gptChat.service.impl;

import com.refresh.gptChat.pojo.Conversation;
import com.refresh.gptChat.pojo.Image;
import com.refresh.gptChat.pojo.Result;
import com.refresh.gptChat.pojo.Speech;
import com.refresh.gptChat.service.outPutService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Yangyang
 * @create 2024-04-03 17:08
 */
@Slf4j
@Service
public class outPutServiceImpl implements outPutService {
    /**
     * chat接口的输出
     *
     * @param response
     * @param resp
     * @param conversation
     */
    public void outPutChat(HttpServletResponse response, Response resp, Conversation conversation) {
        try {
            String model = (conversation.getModel() != null) ? conversation.getModel() : "gpt-3.5-turbo";
            boolean isStream = conversation.getStream() != null ? conversation.getStream() : false;
            int one_messageByte;
            int sleep_time;
            if (isStream) {
                if (!model.contains("gpt-4")) {
                    one_messageByte = 2048;
                    sleep_time = 0;
                } else {
                    one_messageByte = 128;
                    sleep_time = 25;
                }
                response.setContentType("text/event-stream; charset=UTF-8");
            } else {
                one_messageByte = 8192;
                sleep_time = 0;
                response.setContentType("application/json; charset=utf-8");
            }
            OutputStream out = new BufferedOutputStream(response.getOutputStream());
            InputStream in = new BufferedInputStream(resp.body().byteStream());
            // 一次拿多少数据 迭代循环
            byte[] buffer = new byte[one_messageByte];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                out.flush();
                try {
                    if (sleep_time > 0) {
                        Thread.sleep(sleep_time);
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            log.info("使用模型：" + model + "，" + resp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * image接口的输出
     *
     * @param response
     * @param resp
     */
    public void outPutImage(HttpServletResponse response, Response resp, Image conversation) {
        try {
            response.setContentType("application/json; charset=utf-8");
            String model = (conversation.getModel() != null) ? conversation.getModel() : "dell-e-3";
            OutputStream out = new BufferedOutputStream(response.getOutputStream());
            InputStream in = new BufferedInputStream(resp.body().byteStream());
            // 一次拿多少数据 迭代循环
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                out.flush();
            }
            log.info("使用模型：" + model + "，" + resp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * speech接口的输出
     *
     * @param response
     * @param resp
     */
    @Override
    public void outPutSpeech(HttpServletResponse response, Response resp, Speech conversation) {
        try {
            response.setContentType("audio/mpeg");
            String model = (conversation.getModel() != null) ? conversation.getModel() : "tts-1";
            OutputStream out = new BufferedOutputStream(response.getOutputStream());
            InputStream in = new BufferedInputStream(resp.body().byteStream());
            // 一次拿多少数据 迭代循环
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                out.flush();
            }
            log.info("使用模型：" + model + "，" + resp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void outPutAudio(HttpServletResponse response, Response resp, String temModel) {
        try {
            response.setContentType("application/json; charset=utf-8");
            String model = temModel != null ? temModel : "whisper-1";
            OutputStream out = new BufferedOutputStream(response.getOutputStream());
            InputStream in = new BufferedInputStream(resp.body().byteStream());
            // 一次拿多少数据 迭代循环
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                out.flush();
            }
            log.info("使用模型：" + model + "，" + resp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * edit接口的输出
     * @param response
     * @param resp
     */
    @Override
    public void outPutEdit(HttpServletResponse response, Response resp) {
        try {
            response.setContentType("application/json; charset=utf-8");
            OutputStream out = new BufferedOutputStream(response.getOutputStream());
            InputStream in = new BufferedInputStream(resp.body().byteStream());
            // 一次拿多少数据 迭代循环
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                out.flush();
            }
            log.info("使用edits接口编辑图片, " + resp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 返回异步responseEntity
     *
     * @param response future
     */
    public ResponseEntity<Object> getObjectResponseEntity(HttpServletResponse response, CompletableFuture<ResponseEntity<Object>> future) {
        ResponseEntity<Object> responseEntity;

        try {
            responseEntity = future.get(6, TimeUnit.MINUTES);
        } catch (TimeoutException ex) {
            response.setContentType("application/json; charset=utf-8");
            future.cancel(true);
            responseEntity = new ResponseEntity<>(Result.error("The Chat timed out"), HttpStatus.REQUEST_TIMEOUT);
        } catch (Exception ex) {
            response.setContentType("application/json; charset=utf-8");
            log.error(ex.getMessage());
            responseEntity = new ResponseEntity<>(Result.error("An error occurred"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return responseEntity;
    }
}
