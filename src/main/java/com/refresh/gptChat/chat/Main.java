package com.refresh.gptChat.chat;

import okhttp3.*;

import java.io.IOException;

    public class Main {
        public static void main(String[] args) {
            OkHttpClient client = new OkHttpClient();

            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, "{\n" +
                    "    \"stream\":true,\n" +
                    "    \"model\":\"gpt-3.5\",\n" +
                    "    \"messages\": [{\"role\": \"user\", \"content\": \"你好\"}]\n" +
                    "}");
            Request request = new Request.Builder()
                    .url("http://20.39.191.116:8888/666/v1/chat/completions")
                    .post(body)
                    .addHeader("Authorization", "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6Ik1UaEVOVUpHTkVNMVFURTRNMEZCTWpkQ05UZzVNRFUxUlRVd1FVSkRNRU13UmtGRVFrRXpSZyJ9.eyJodHRwczovL2FwaS5vcGVuYWkuY29tL3Byb2ZpbGUiOnsiZW1haWwiOiJoeXMyc2hAMzc2NjYuc2JzIiwiZW1haWxfdmVyaWZpZWQiOnRydWV9LCJodHRwczovL2FwaS5vcGVuYWkuY29tL2F1dGgiOnsicG9pZCI6Im9yZy1YcndTMndySmNRVzhkaUQ0bExlVHFFcVMiLCJ1c2VyX2lkIjoidXNlci12cm95ZXBBRVlzcHBuVGxtWmNydmlBWlEifSwiaXNzIjoiaHR0cHM6Ly9hdXRoMC5vcGVuYWkuY29tLyIsInN1YiI6ImF1dGgwfDY1NDBkMTllYTRiYmY5YWQwZGJhYWYxMyIsImF1ZCI6WyJodHRwczovL2FwaS5vcGVuYWkuY29tL3YxIiwiaHR0cHM6Ly9vcGVuYWkub3BlbmFpLmF1dGgwYXBwLmNvbS91c2VyaW5mbyJdLCJpYXQiOjE3MDY3MTkwMjYsImV4cCI6MTcwNzU4MzAyNiwiYXpwIjoicGRsTElYMlk3Mk1JbDJyaExoVEU5VlY5Yk45MDVrQmgiLCJzY29wZSI6Im9wZW5pZCBwcm9maWxlIGVtYWlsIG1vZGVsLnJlYWQgbW9kZWwucmVxdWVzdCBvcmdhbml6YXRpb24ucmVhZCBvZmZsaW5lX2FjY2VzcyJ9.gNTEryN6V2HJ-IU7yUi4mgKd3VS3v7odGH0sZ_v2KUU9zoyyJERxhNywW5O3TJtpwGZbBYDdMTgXnQ1vUSAHFsuRSI5o7NGBnu5i0OeQ4OqrENWkERRl1WADuD9Y9yje61PB3K3WSrWgZlrHJQdKzyb3Pe2-46Nj0VnBN68XferrfYHRXJT4bolA1XnxVRYfUmSp8_2M82edN2hGnRrwI3MOkkJX3OHP8hT1B78reyyQLERGEnqqZRIkr7Cxj0X926olVFrpqLJ5hOd90OuE1Mvo_Pv1rI9CGrfhlVK9xrAs49gilVbVz2sPC1wycYl9uUUwXVWq5pghXuy8CTa9CQ")
                    .addHeader("Content-Type", "application/json")
                    .build();

            try {
                Response response = client.newCall(request).execute();
                System.out.println(response.body().string());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

