package com.nju.edu;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nju.edu.entities.KanikoDTO;
import com.nju.edu.vo.BuildResultVO;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;


import static com.nju.edu.SeecoderBuildException.BIZ_ERROR_CODE;

public class SeecoderKanikoBuildClient implements SeecoderKanikoBuildApi {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final OkHttpClient client;
    private String token;
    private final String host;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public SeecoderKanikoBuildClient(String host, String token) {
        this.token = token;
        this.host = host;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build();
    }

    @Override
    public void startBuild(String gitRepoUrl, String branchName, String imageName, String configMapContentName, String configMapContent, String dockerfileContent) {
        try {
            // 构建请求体
            KanikoDTO kanikoDTO = KanikoDTO.builder()
                    .gitRepoUrl(gitRepoUrl)
                    .branchName(branchName)
                    .imageName(imageName)
                    .configMapContentName(configMapContentName)
                    .configMapContent(configMapContent)
                    .dockerfileContent(dockerfileContent)
                    .build();
            String json = objectMapper.writeValueAsString(kanikoDTO);
            RequestBody requestBody = RequestBody.Companion.create(
                    json,
                    MediaType.parse("application/json; charset=utf-8")
            );

            // 发送请求
            String responseBody = sendRequest(host + "/api/builds", requestBody);
            System.out.println("Build started successfully: " + responseBody);
        } catch (JsonProcessingException e) {
            System.err.println("Failed to serialize GitDTO: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Error during build request: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public BuildResultVO queryBuild(String imageName) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String url = String.format("%s/api/builds/query?imageName=%s", host, URLEncoder.encode(imageName, StandardCharsets.UTF_8));

            Request request = new Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Authorization", token)
                    .build();
            final Call call = client.newCall(request);
            Response response = call.execute();
            return processResponse(objectMapper, response, BuildResultVO.class);
        } catch (IOException e) {
            logger.error("IOException", e);
            throw SeecoderBuildException.IO_ERROR;
        }
    }

    private String sendRequest(String url, RequestBody requestBody) throws Exception {
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Authorization", token)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            } else {
                throw new Exception("Request failed: HTTP " + response.code());
            }
        }
    }


    private <T> T processResponse(ObjectMapper objectMapper, Response response, Class<T> clazz) {
        try {
            validateResponseStatus(response);
            String body = response.body().string();
            JsonNode node = objectMapper.readTree(body);
            Integer code = Integer.parseInt(node.get("code").asText());
            String msg = node.get("msg").asText();
            if (code != 0) {
                throw new SeecoderBuildException(BIZ_ERROR_CODE, msg);
            }
            if (clazz == null) {
                return null;
            }
            return objectMapper.treeToValue(node.get("result"), clazz);
        } catch (IOException e) {
            logger.error("IOException", e);
            throw SeecoderBuildException.IO_ERROR;
        }
    }

    private void validateResponseStatus(Response response) {
        if (response.code() == 403) {
            throw SeecoderBuildException.UNAHTORIZATION_ERROR;
        } else if (response.code() == 500) {
            throw SeecoderBuildException.INTERNAL_SERVER_ERROR;
        } else if (response.code() != 200) {
            throw SeecoderBuildException.UNKNOWN_ERROR;
        }
    }
}
