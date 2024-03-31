package com.refresh.gptChat.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @author YANGYANG
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Conversation {

    private String model;

    private List<Map<String, String>> messages;

    private Boolean stream;

}
