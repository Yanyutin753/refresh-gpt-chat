package com.refresh.gptChat.pojo;

/**
 * @author Yangyang
 * @create 2024-01-04 19:44
 */

public class modelsUsage {
    private String Model;
    private Integer usage;

    public modelsUsage() {
    }

    public modelsUsage(String model, Integer usage) {
        Model = model;
        this.usage = usage;
    }

    public String getModel() {
        return Model;
    }

    public void setModel(String model) {
        Model = model;
    }

    public Integer getUsage() {
        return usage;
    }

    public void setUsage(Integer usage) {
        this.usage = usage;
    }
}
