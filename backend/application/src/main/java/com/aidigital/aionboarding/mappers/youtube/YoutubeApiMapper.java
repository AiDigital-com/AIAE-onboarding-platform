package com.aidigital.aionboarding.mappers.youtube;

import com.aidigital.aionboarding.api.v1.model.YoutubeOembedResponseV1;
import com.aidigital.aionboarding.config.ApplicationMapperConfig;
import com.aidigital.aionboarding.external.youtube.model.YoutubeOEmbedMetadata;
import org.mapstruct.Mapper;

@Mapper(config = ApplicationMapperConfig.class)
public interface YoutubeApiMapper {

	YoutubeOembedResponseV1 toYoutubeOembedResponseV1(YoutubeOEmbedMetadata metadata);
}
