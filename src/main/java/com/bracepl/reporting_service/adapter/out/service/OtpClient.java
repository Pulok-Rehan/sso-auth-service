package com.bracepl.reporting_service.adapter.out.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class OtpClient {
    @Value("${client.otp.baseUrl}")
    private String baseUrl;
    @Value("${client.otp.sendOtp}")
    private String sendOtp;
    @Value("${client.otp.validateOtp}")
    private String validateOtp;
    @Value("${client.otp.sendTemporaryPassword}")
    private String sendTemporaryPassword;

    private final RestTemplate restTemplate;

    public OtpClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String sendOtp(String senderNumber, String senderEmail) {

        String url = baseUrl;

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("senderNumber", senderNumber);
        params.add("senderEmail", senderEmail);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(params, headers);

        return restTemplate.postForObject(url+sendOtp, request, String.class);
    }

    public boolean validateOtp(String mobileNumber, String email, String otp) {

        String url = baseUrl;

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("mobileNumber", mobileNumber);
        params.add("email", email);
        params.add("otp", otp);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(params, headers);

        Boolean response = restTemplate.postForObject(
                url+validateOtp,
                request,
                Boolean.class
        );

        return Boolean.TRUE.equals(response);
    }

    public boolean sendTemporaryPassword(String mobileNumber, String temporaryPassword, String email){
        String url = baseUrl;

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("mobileNumber", mobileNumber);
        params.add("email", email);
        params.add("temporaryPassword", temporaryPassword);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(params, headers);

        Boolean response = restTemplate.postForObject(
                url+sendTemporaryPassword,
                request,
                Boolean.class
        );

        return Boolean.TRUE.equals(response);
    }



}
