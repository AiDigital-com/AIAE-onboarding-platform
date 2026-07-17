package com.aidigital.aionboarding.controllers;

import com.aidigital.aionboarding.api.v1.YoutubeApi;
import com.aidigital.aionboarding.api.v1.model.YoutubeOembedResponseV1;
import com.aidigital.aionboarding.external.youtube.YoutubeClient;
import com.aidigital.aionboarding.mappers.youtube.YoutubeApiMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class YoutubeController implements YoutubeApi {

    private final CurrentUserSupport currentUser;
    private final YoutubeClient youtubeClient;
    private final YoutubeApiMapper youtubeApiMapper;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<YoutubeOembedResponseV1> getYoutubeOembed(String url) {
        currentUser.requireUser();
        return ResponseEntity.ok(youtubeApiMapper.toYoutubeOembedResponseV1(youtubeClient.fetchOembed(url)));
    }
}
