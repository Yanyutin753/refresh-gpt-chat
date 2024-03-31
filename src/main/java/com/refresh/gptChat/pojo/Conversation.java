package com.refresh.gptChat.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author YANGYANG
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Conversation {

    private String model;

    private Object messages;

    private Boolean stream;

}
