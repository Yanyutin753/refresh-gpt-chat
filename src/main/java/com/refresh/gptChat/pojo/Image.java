package com.refresh.gptChat.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Yangyang
 * @create 2024-04-07 21:11
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Image {

    private String model;

    private String prompt;
}
